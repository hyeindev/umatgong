package com.umatgong.domain.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.umatgong.domain.auth.dto.LoginResponse;
import com.umatgong.domain.auth.dto.TokenResponse;
import com.umatgong.domain.auth.entity.RefreshToken;
import com.umatgong.domain.auth.repository.RefreshTokenRepository;
import com.umatgong.domain.user.entity.User;
import com.umatgong.domain.user.repository.UserRepository;
import com.umatgong.global.error.BusinessException;
import com.umatgong.global.error.ErrorCode;
import com.umatgong.global.kakao.KakaoAuthClient;
import com.umatgong.global.kakao.KakaoUserInfo;
import com.umatgong.global.security.JwtProperties;
import com.umatgong.global.security.JwtProvider;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AuthService {

	private static final int REFRESH_TOKEN_BYTES = 32;
	private static final int MAX_NAME_LENGTH = 50;
	// 닉네임 동의를 하지 않은 사용자. users.name은 NOT NULL이다.
	private static final String DEFAULT_NAME = "이름 없음";
	private static final SecureRandom RANDOM = new SecureRandom();
	private static final String KAKAO_ID_UNIQUE_CONSTRAINT = "uq_users_kakao_id";

	private final KakaoAuthClient kakaoAuthClient;
	private final UserRepository userRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final JwtProvider jwtProvider;
	private final JwtProperties jwtProperties;
	private final Clock clock;
	private final TransactionTemplate transactionTemplate;

	public AuthService(KakaoAuthClient kakaoAuthClient, UserRepository userRepository,
		RefreshTokenRepository refreshTokenRepository, JwtProvider jwtProvider, JwtProperties jwtProperties,
		Clock clock, PlatformTransactionManager transactionManager) {
		this.kakaoAuthClient = kakaoAuthClient;
		this.userRepository = userRepository;
		this.refreshTokenRepository = refreshTokenRepository;
		this.jwtProvider = jwtProvider;
		this.jwtProperties = jwtProperties;
		this.clock = clock;
		this.transactionTemplate = new TransactionTemplate(transactionManager);
	}

	/** 네이티브 로그인. 카카오 네이티브 SDK가 준 액세스 토큰으로 로그인한다. */
	public LoginResponse loginWithKakao(String kakaoAccessToken) {
		return signInWithKakaoToken(kakaoAccessToken);
	}

	/**
	 * 웹 로그인. 카카오 JS SDK authorize가 준 인가 코드를 카카오 액세스 토큰으로 바꾼 뒤
	 * 네이티브 로그인과 같은 경로로 로그인한다. 같은 카카오 회원은 어느 경로로 와도 같은 사용자다.
	 */
	public LoginResponse loginWithKakaoCode(String code, String redirectUri) {
		return signInWithKakaoToken(kakaoAuthClient.exchangeCode(code, redirectUri));
	}

	/**
	 * 두 로그인 경로가 공유하는 사용자 처리. 카카오 토큰 검증 → 조회·가입 → 우리 토큰 발급.
	 *
	 * <p>카카오 호출은 트랜잭션 밖에서 한다. 카카오가 느릴 때 DB 커넥션을 붙잡고 기다리지 않기 위해서다.
	 */
	private LoginResponse signInWithKakaoToken(String kakaoAccessToken) {
		KakaoUserInfo kakaoUser = kakaoAuthClient.verifyAndFetchUser(kakaoAccessToken);
		try {
			return transactionTemplate.execute(status -> signInOrSignUp(kakaoUser));
		} catch (DataIntegrityViolationException e) {
			// 같은 신규 사용자의 로그인이 동시에 오면(버튼 연타, 재시도) 둘 다 "없음"을 보고 가입을 시도하고
			// 늦은 쪽은 회원번호 중복으로 실패한다. 먼저 가입한 쪽은 이미 커밋됐으므로 새 트랜잭션에서
			// 한 번 더 하면 기존 사용자로 로그인된다. 다른 무결성 위반은 그대로 올린다.
			if (!isDuplicateKakaoId(e)) {
				throw e;
			}
			log.info("Concurrent sign-up for the same Kakao user; retrying as sign-in: kakaoId={}",
				kakaoUser.kakaoId());
			return transactionTemplate.execute(status -> signInOrSignUp(kakaoUser));
		}
	}

	private static boolean isDuplicateKakaoId(DataIntegrityViolationException e) {
		for (Throwable t = e; t != null; t = t.getCause()) {
			if (t instanceof ConstraintViolationException cve
				&& KAKAO_ID_UNIQUE_CONSTRAINT.equalsIgnoreCase(cve.getConstraintName())) {
				return true;
			}
		}
		return false;
	}

	private LoginResponse signInOrSignUp(KakaoUserInfo kakaoUser) {
		Optional<User> existing = userRepository.findByKakaoId(kakaoUser.kakaoId());
		User user;
		if (existing.isPresent()) {
			user = existing.get();
			// 동의를 철회해 닉네임이 안 오면 기존 이름을 지우지 않는다.
			String name = kakaoUser.nickname() == null ? user.getName() : normalizeName(kakaoUser.nickname());
			user.updateProfile(name, kakaoUser.profileImageUrl());
		} else {
			user = userRepository.save(User.signUpWithKakao(
				kakaoUser.kakaoId(), normalizeName(kakaoUser.nickname()), kakaoUser.profileImageUrl()));
		}

		Instant now = clock.instant();
		String rawRefreshToken = newRawRefreshToken();
		refreshTokenRepository.save(RefreshToken.issueOnLogin(
			user, hash(rawRefreshToken), now.plus(jwtProperties.refreshTokenValidity())));

		return new LoginResponse(tokens(user.getId(), rawRefreshToken), LoginResponse.UserSummary.from(user),
			existing.isEmpty());
	}

	/**
	 * 리프레시 토큰을 회전한다. 이미 폐기된 토큰이 들어오면 탈취로 보고 그 로그인 세션(family) 전체를 폐기한다.
	 *
	 * <p>폐기 처리는 예외를 던져도 커밋돼야 하므로 BusinessException에 롤백하지 않는다.
	 */
	@Transactional(noRollbackFor = BusinessException.class)
	public TokenResponse refresh(String rawRefreshToken) {
		Instant now = clock.instant();
		RefreshToken current = refreshTokenRepository.findByTokenHashForUpdate(hash(rawRefreshToken))
			.orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));

		if (current.isRevoked()) {
			int revoked = refreshTokenRepository.revokeFamily(current.getFamilyId(), now);
			log.warn("Refresh token reuse detected: userId={}, familyId={}, revoked={}",
				current.getUser().getId(), current.getFamilyId(), revoked);
			throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
		}
		if (!current.isActive(now)) {
			throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
		}

		String nextRawToken = newRawRefreshToken();
		refreshTokenRepository.save(
			current.rotate(hash(nextRawToken), now.plus(jwtProperties.refreshTokenValidity()), now));
		return tokens(current.getUser().getId(), nextRawToken);
	}

	/**
	 * 이 기기의 로그인 세션만 끝낸다. 다른 기기의 로그인은 유지된다.
	 * 이미 로그아웃됐거나 모르는 토큰이어도 성공으로 본다 (클라이언트는 어차피 토큰을 버린다).
	 */
	@Transactional
	public void logout(String rawRefreshToken) {
		refreshTokenRepository.findByTokenHash(hash(rawRefreshToken))
			.ifPresent(token -> refreshTokenRepository.revokeFamily(token.getFamilyId(), clock.instant()));
	}

	private TokenResponse tokens(Long userId, String rawRefreshToken) {
		return new TokenResponse(
			jwtProvider.createAccessToken(userId),
			jwtProperties.accessTokenValidity().toSeconds(),
			rawRefreshToken,
			jwtProperties.refreshTokenValidity().toSeconds());
	}

	private static String normalizeName(String nickname) {
		if (nickname == null || nickname.isBlank()) {
			return DEFAULT_NAME;
		}
		String trimmed = nickname.strip();
		return trimmed.length() > MAX_NAME_LENGTH ? trimmed.substring(0, MAX_NAME_LENGTH) : trimmed;
	}

	// 리프레시 토큰은 JWT가 아니라 무작위 문자열이다. 서버가 DB로 상태를 들고 있으므로 서명이 필요 없다.
	private static String newRawRefreshToken() {
		byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
		RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	static String hash(String rawToken) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 is required by every JVM", e);
		}
	}
}

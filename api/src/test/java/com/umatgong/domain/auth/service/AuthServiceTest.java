package com.umatgong.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;

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

class AuthServiceTest {

	private static final Instant NOW = Instant.parse("2026-09-30T00:00:00Z");
	private static final Duration REFRESH_VALIDITY = Duration.ofDays(14);

	private final KakaoAuthClient kakaoAuthClient = mock(KakaoAuthClient.class);
	private final UserRepository userRepository = mock(UserRepository.class);
	private final RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
	private final JwtProperties jwtProperties =
		new JwtProperties("test-secret-test-secret-test-secret-0123", Duration.ofMinutes(30), REFRESH_VALIDITY);
	private final JwtProvider jwtProvider = new JwtProvider(jwtProperties);

	private AuthService authService;

	@BeforeEach
	void setUp() {
		authService = new AuthService(kakaoAuthClient, userRepository, refreshTokenRepository, jwtProvider,
			jwtProperties, Clock.fixed(NOW, ZoneOffset.UTC), mock(PlatformTransactionManager.class));
		given(userRepository.save(any(User.class))).willAnswer(inv -> withId(inv.getArgument(0), 7L));
	}

	@Test
	void 처음_로그인하면_카카오_회원번호로_가입하고_newUser가_true다() {
		given(kakaoAuthClient.verifyAndFetchUser("kakao-token"))
			.willReturn(new KakaoUserInfo(1234L, "지현", "https://img/1.jpg"));
		given(userRepository.findByKakaoId(1234L)).willReturn(Optional.empty());

		LoginResponse response = authService.loginWithKakao("kakao-token");

		assertThat(response.newUser()).isTrue();
		assertThat(response.user().id()).isEqualTo(7L);
		assertThat(response.user().name()).isEqualTo("지현");
		assertThat(jwtProvider.parseAccessToken(response.tokens().accessToken())).isEqualTo(7L);
		assertThat(response.tokens().accessTokenExpiresIn()).isEqualTo(1800);
		assertThat(response.tokens().refreshTokenExpiresIn()).isEqualTo(1_209_600);
	}

	@Test
	void 리프레시_토큰은_원문이_아니라_해시로_저장된다() {
		given(kakaoAuthClient.verifyAndFetchUser(anyString())).willReturn(new KakaoUserInfo(1L, "지현", null));
		given(userRepository.findByKakaoId(1L)).willReturn(Optional.empty());

		LoginResponse response = authService.loginWithKakao("kakao-token");

		ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
		verify(refreshTokenRepository).save(saved.capture());
		String raw = response.tokens().refreshToken();
		assertThat(saved.getValue().getTokenHash()).isNotEqualTo(raw).isEqualTo(AuthService.hash(raw)).hasSize(64);
		assertThat(saved.getValue().getExpiresAt()).isEqualTo(NOW.plus(REFRESH_VALIDITY));
	}

	@Test
	void 기존_사용자는_프로필을_갱신하되_닉네임이_안_오면_이름을_지우지_않는다() {
		User existing = withId(User.signUpWithKakao(1L, "지현", "https://img/old.jpg"), 3L);
		given(kakaoAuthClient.verifyAndFetchUser(anyString())).willReturn(new KakaoUserInfo(1L, null, null));
		given(userRepository.findByKakaoId(1L)).willReturn(Optional.of(existing));

		LoginResponse response = authService.loginWithKakao("kakao-token");

		assertThat(response.newUser()).isFalse();
		assertThat(existing.getName()).isEqualTo("지현");
		assertThat(existing.getAvatarUrl()).isNull();
		verify(userRepository, never()).save(any());
	}

	@Test
	void 닉네임_동의가_없는_신규_사용자는_기본_이름으로_가입한다() {
		given(kakaoAuthClient.verifyAndFetchUser(anyString())).willReturn(new KakaoUserInfo(1L, null, null));
		given(userRepository.findByKakaoId(1L)).willReturn(Optional.empty());

		assertThat(authService.loginWithKakao("kakao-token").user().name()).isEqualTo("이름 없음");
	}

	@Test
	void 카카오_검증에_실패하면_사용자를_만들지_않는다() {
		given(kakaoAuthClient.verifyAndFetchUser(anyString()))
			.willThrow(new BusinessException(ErrorCode.KAKAO_INVALID_TOKEN));

		assertThatThrownBy(() -> authService.loginWithKakao("bad"))
			.isInstanceOf(BusinessException.class);
		verify(userRepository, never()).save(any());
		verify(refreshTokenRepository, never()).save(any());
	}

	@Test
	void 갱신하면_토큰이_회전되고_같은_family로_새_토큰이_저장된다() {
		User user = withId(User.signUpWithKakao(1L, "지현", null), 3L);
		RefreshToken current = RefreshToken.issueOnLogin(user, AuthService.hash("old"), NOW.plus(REFRESH_VALIDITY));
		given(refreshTokenRepository.findByTokenHashForUpdate(AuthService.hash("old"))).willReturn(Optional.of(current));

		TokenResponse tokens = authService.refresh("old");

		assertThat(current.isRevoked()).isTrue();
		assertThat(tokens.refreshToken()).isNotEqualTo("old");
		assertThat(jwtProvider.parseAccessToken(tokens.accessToken())).isEqualTo(3L);
		ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
		verify(refreshTokenRepository).save(saved.capture());
		assertThat(saved.getValue().getFamilyId()).isEqualTo(current.getFamilyId());
		assertThat(saved.getValue().getTokenHash()).isEqualTo(AuthService.hash(tokens.refreshToken()));
	}

	@Test
	void 폐기된_토큰이_다시_오면_family_전체를_폐기하고_거부한다() {
		User user = withId(User.signUpWithKakao(1L, "지현", null), 3L);
		RefreshToken reused = RefreshToken.issueOnLogin(user, AuthService.hash("old"), NOW.plus(REFRESH_VALIDITY));
		reused.revoke(NOW.minusSeconds(60));
		given(refreshTokenRepository.findByTokenHashForUpdate(AuthService.hash("old"))).willReturn(Optional.of(reused));

		assertThatThrownBy(() -> authService.refresh("old"))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
		verify(refreshTokenRepository).revokeFamily(reused.getFamilyId(), NOW);
		verify(refreshTokenRepository, never()).save(any());
	}

	@Test
	void 만료되거나_모르는_토큰은_거부한다() {
		User user = withId(User.signUpWithKakao(1L, "지현", null), 3L);
		RefreshToken expired = RefreshToken.issueOnLogin(user, AuthService.hash("expired"), NOW);
		given(refreshTokenRepository.findByTokenHashForUpdate(AuthService.hash("expired")))
			.willReturn(Optional.of(expired));
		given(refreshTokenRepository.findByTokenHashForUpdate(AuthService.hash("unknown"))).willReturn(Optional.empty());

		assertThatThrownBy(() -> authService.refresh("expired")).isInstanceOf(BusinessException.class);
		assertThatThrownBy(() -> authService.refresh("unknown")).isInstanceOf(BusinessException.class);
		verify(refreshTokenRepository, never()).revokeFamily(any(), any());
	}

	@Test
	void 로그아웃은_그_토큰의_family만_폐기하고_모르는_토큰이어도_성공한다() {
		User user = withId(User.signUpWithKakao(1L, "지현", null), 3L);
		RefreshToken token = RefreshToken.issueOnLogin(user, AuthService.hash("mine"), NOW.plus(REFRESH_VALIDITY));
		given(refreshTokenRepository.findByTokenHash(AuthService.hash("mine"))).willReturn(Optional.of(token));
		given(refreshTokenRepository.findByTokenHash(AuthService.hash("unknown"))).willReturn(Optional.empty());

		authService.logout("mine");
		authService.logout("unknown");

		verify(refreshTokenRepository).revokeFamily(eq(token.getFamilyId()), eq(NOW));
	}

	@Test
	void 동시_가입으로_회원번호가_중복되면_기존_사용자로_다시_로그인한다() {
		User winner = withId(User.signUpWithKakao(1L, "지현", null), 9L);
		given(kakaoAuthClient.verifyAndFetchUser(anyString())).willReturn(new KakaoUserInfo(1L, "지현", null));
		// 첫 시도: 아직 없다고 보고 가입 → 다른 요청이 먼저 가입해 중복 / 재시도: 먼저 가입한 사용자가 보인다
		given(userRepository.findByKakaoId(1L)).willReturn(Optional.empty(), Optional.of(winner));
		given(userRepository.save(any(User.class))).willThrow(duplicate("uq_users_kakao_id"));

		LoginResponse response = authService.loginWithKakao("kakao-token");

		assertThat(response.user().id()).isEqualTo(9L);
		assertThat(response.newUser()).isFalse();
		verify(userRepository, times(2)).findByKakaoId(1L);
		// 실패한 첫 시도에서는 리프레시 토큰을 저장하지 않았고, 재시도에서 한 번만 저장한다.
		verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
	}

	@Test
	void 회원번호가_아닌_다른_무결성_위반은_재시도하지_않고_그대로_올린다() {
		given(kakaoAuthClient.verifyAndFetchUser(anyString())).willReturn(new KakaoUserInfo(1L, "지현", null));
		given(userRepository.findByKakaoId(1L)).willReturn(Optional.empty());
		given(userRepository.save(any(User.class))).willThrow(duplicate("some_other_constraint"));

		assertThatThrownBy(() -> authService.loginWithKakao("kakao-token"))
			.isInstanceOf(DataIntegrityViolationException.class);
		verify(userRepository, times(1)).findByKakaoId(1L);
	}

	@Test
	void 재시도는_한_번만_한다() {
		given(kakaoAuthClient.verifyAndFetchUser(anyString())).willReturn(new KakaoUserInfo(1L, "지현", null));
		given(userRepository.findByKakaoId(1L)).willReturn(Optional.empty());
		given(userRepository.save(any(User.class))).willThrow(duplicate("uq_users_kakao_id"));

		assertThatThrownBy(() -> authService.loginWithKakao("kakao-token"))
			.isInstanceOf(DataIntegrityViolationException.class);
		verify(userRepository, times(2)).findByKakaoId(1L);
	}

	private static DataIntegrityViolationException duplicate(String constraint) {
		return new DataIntegrityViolationException("duplicate key",
			new ConstraintViolationException("duplicate key", new SQLException("duplicate key"), constraint));
	}

	private static User withId(User user, Long id) {
		ReflectionTestUtils.setField(user, "id", id);
		return user;
	}
}

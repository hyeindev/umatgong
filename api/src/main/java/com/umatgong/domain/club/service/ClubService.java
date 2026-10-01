package com.umatgong.domain.club.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umatgong.domain.club.dto.ClubMemberResponse;
import com.umatgong.domain.club.dto.ClubResponse;
import com.umatgong.domain.club.dto.InviteResponse;
import com.umatgong.domain.club.entity.Club;
import com.umatgong.domain.club.entity.ClubColor;
import com.umatgong.domain.club.entity.ClubMember;
import com.umatgong.domain.club.entity.ClubMemberId;
import com.umatgong.domain.club.entity.ClubPlan;
import com.umatgong.domain.club.repository.ClubMemberRepository;
import com.umatgong.domain.club.repository.ClubMemberRepository.MemberCount;
import com.umatgong.domain.club.repository.ClubRepository;
import com.umatgong.domain.user.entity.User;
import com.umatgong.domain.user.repository.UserRepository;
import com.umatgong.global.error.BusinessException;
import com.umatgong.global.error.ErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * 클럽 생성·초대·합류·탈퇴.
 *
 * <p><b>클럽 경계:</b> 클럽 정보는 그 클럽 멤버에게만 보인다. 멤버가 아니면 클럽이 있는지도 알려주지 않고
 * “없는 클럽”과 똑같이 CLUB_NOT_FOUND로 답한다. 이 검사는 컨트롤러가 아니라 여기서 한다.
 */
@Service
@RequiredArgsConstructor
public class ClubService {

	private final ClubRepository clubRepository;
	private final ClubMemberRepository clubMemberRepository;
	private final UserRepository userRepository;
	private final InviteCodeGenerator inviteCodeGenerator;
	private final PlanProperties planProperties;

	@Transactional
	public ClubResponse create(Long userId, String name) {
		User user = lockUser(userId);
		ensureCanJoinAnotherClub(userId);

		ClubColor color = ClubColorPicker.pick(clubMemberRepository.findClubColorsByUserId(userId));
		Club club = clubRepository.save(Club.create(name.strip(), color, user, inviteCodeGenerator.next()));
		clubMemberRepository.save(ClubMember.join(club, user));
		return ClubResponse.of(club, 1, maxMembers(club), userId);
	}

	@Transactional(readOnly = true)
	public List<ClubResponse> myClubs(Long userId) {
		List<Club> clubs = clubMemberRepository.findWithClubByUserId(userId).stream()
			.map(ClubMember::getClub)
			.toList();
		if (clubs.isEmpty()) {
			return List.of();
		}
		Map<Long, Long> counts = clubMemberRepository.countByClubIds(clubs.stream().map(Club::getId).toList())
			.stream()
			.collect(Collectors.toMap(MemberCount::getClubId, MemberCount::getMemberCount));
		return clubs.stream()
			.map(club -> ClubResponse.of(club, counts.getOrDefault(club.getId(), 0L), maxMembers(club), userId))
			.toList();
	}

	/**
	 * 초대 코드를 돌려준다. 정원이 다 찼으면 초대를 막는다 (기존 멤버 접근은 막지 않는다).
	 *
	 * @param reissue true면 새 코드를 만들어 이전 링크를 무효로 한다. 클럽장만 할 수 있다
	 */
	@Transactional
	public InviteResponse invite(Long userId, Long clubId, boolean reissue) {
		Club club = requireMembership(clubId, userId, true);
		long memberCount = clubMemberRepository.countByClubId(clubId);
		int maxMembers = maxMembers(club);
		if (memberCount >= maxMembers) {
			throw new BusinessException(ErrorCode.CLUB_FULL);
		}
		if (reissue) {
			if (!club.isOwnedBy(userId)) {
				throw new BusinessException(ErrorCode.FORBIDDEN, "초대 링크는 클럽장만 새로 만들 수 있습니다.");
			}
			club.reissueInviteCode(inviteCodeGenerator.next());
		}
		return new InviteResponse(club.getInviteCode(), memberCount, maxMembers);
	}

	@Transactional
	public ClubResponse join(Long userId, String inviteCode) {
		// 클럽 → 사용자 순서로 잠근다. 클럽 생성은 사용자만 잠그므로 잠금 순서가 엇갈려 교착되지 않는다.
		Club club = clubRepository.findByInviteCodeForUpdate(inviteCode.strip())
			.orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INVITE_CODE));
		User user = lockUser(userId);

		if (clubMemberRepository.existsById(new ClubMemberId(club.getId(), userId))) {
			throw new BusinessException(ErrorCode.ALREADY_CLUB_MEMBER);
		}
		long memberCount = clubMemberRepository.countByClubId(club.getId());
		int maxMembers = maxMembers(club);
		if (memberCount >= maxMembers) {
			throw new BusinessException(ErrorCode.CLUB_FULL);
		}
		ensureCanJoinAnotherClub(userId);

		clubMemberRepository.save(ClubMember.join(club, user));
		return ClubResponse.of(club, memberCount + 1, maxMembers, userId);
	}

	@Transactional(readOnly = true)
	public List<ClubMemberResponse> members(Long userId, Long clubId) {
		Club club = requireMembership(clubId, userId, false);
		return clubMemberRepository.findWithUserByClubId(clubId).stream()
			.map(member -> ClubMemberResponse.of(member, club))
			.toList();
	}

	/**
	 * 클럽을 나간다. 클럽장이 나가면 클럽장 자리만 비우고 클럽은 남긴다.
	 * 마지막 멤버가 나가도 클럽은 지우지 않는다 (그 클럽에 남긴 기록이 클럽을 참조한다).
	 */
	@Transactional
	public void leave(Long userId, Long clubId) {
		Club club = requireMembership(clubId, userId, true);
		clubMemberRepository.deleteById(new ClubMemberId(clubId, userId));
		if (club.isOwnedBy(userId)) {
			club.releaseOwner();
		}
	}

	private Club requireMembership(Long clubId, Long userId, boolean forUpdate) {
		Club club = (forUpdate ? clubRepository.findByIdForUpdate(clubId) : clubRepository.findById(clubId))
			.orElseThrow(() -> new BusinessException(ErrorCode.CLUB_NOT_FOUND));
		if (!clubMemberRepository.existsById(new ClubMemberId(clubId, userId))) {
			throw new BusinessException(ErrorCode.CLUB_NOT_FOUND);
		}
		return club;
	}

	private void ensureCanJoinAnotherClub(Long userId) {
		// 사용자에게는 요금제가 없으므로 무료 기준을 쓴다 (PlanProperties 참고)
		if (clubMemberRepository.countByUserId(userId) >= planProperties.of(ClubPlan.FREE).maxClubsPerUser()) {
			throw new BusinessException(ErrorCode.CLUB_LIMIT_REACHED);
		}
	}

	private User lockUser(Long userId) {
		// 토큰은 유효한데 사용자가 없으면 탈퇴 등으로 지워진 계정이다
		return userRepository.findByIdForUpdate(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
	}

	private int maxMembers(Club club) {
		return planProperties.of(club.getPlan()).maxClubMembers();
	}
}

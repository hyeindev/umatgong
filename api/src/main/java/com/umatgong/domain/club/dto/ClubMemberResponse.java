package com.umatgong.domain.club.dto;

import java.time.Instant;

import com.umatgong.domain.club.entity.Club;
import com.umatgong.domain.club.entity.ClubMember;
import com.umatgong.domain.user.entity.User;

/**
 * @param owner      클럽장인지
 * @param visitCount 이 멤버가 이 클럽에 남긴 기록 중 요청자에게 보이는 것의 수.
 *                   남의 비공개 기록은 세지 않고, 요청자 본인 것은 비공개도 센다
 */
public record ClubMemberResponse(Long userId, String name, String avatarUrl, boolean owner, long visitCount,
	Instant joinedAt) {

	public static ClubMemberResponse of(ClubMember member, Club club, long visitCount) {
		User user = member.getUser();
		return new ClubMemberResponse(user.getId(), user.getName(), user.getAvatarUrl(),
			club.isOwnedBy(user.getId()), visitCount, member.getJoinedAt());
	}
}

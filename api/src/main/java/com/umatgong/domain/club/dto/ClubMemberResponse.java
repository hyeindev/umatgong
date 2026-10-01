package com.umatgong.domain.club.dto;

import java.time.Instant;

import com.umatgong.domain.club.entity.Club;
import com.umatgong.domain.club.entity.ClubMember;
import com.umatgong.domain.user.entity.User;

/** @param owner 클럽장인지 */
public record ClubMemberResponse(Long userId, String name, String avatarUrl, boolean owner, Instant joinedAt) {

	public static ClubMemberResponse of(ClubMember member, Club club) {
		User user = member.getUser();
		return new ClubMemberResponse(user.getId(), user.getName(), user.getAvatarUrl(),
			club.isOwnedBy(user.getId()), member.getJoinedAt());
	}
}

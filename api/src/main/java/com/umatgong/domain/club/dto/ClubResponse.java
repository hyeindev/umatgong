package com.umatgong.domain.club.dto;

import java.time.Instant;

import com.umatgong.domain.club.entity.Club;
import com.umatgong.domain.club.entity.ClubColor;

/**
 * @param color      디자인 시스템 클럽 색 토큰 이름 (app/src/theme colors.club의 키)
 * @param full       정원이 다 찼는지. true면 새 초대·합류가 막힌다
 * @param owner      요청한 사용자가 클럽장인지
 */
public record ClubResponse(
	Long id,
	String name,
	ClubColor color,
	long memberCount,
	int maxMembers,
	boolean full,
	boolean owner,
	Instant createdAt
) {

	public static ClubResponse of(Club club, long memberCount, int maxMembers, Long viewerId) {
		return new ClubResponse(club.getId(), club.getName(), club.getColor(), memberCount, maxMembers,
			memberCount >= maxMembers, club.isOwnedBy(viewerId), club.getCreatedAt());
	}
}

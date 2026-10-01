package com.umatgong.domain.club.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import com.umatgong.domain.club.entity.ClubPlan;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 요금제별 제한값 (기획서 9장). 코드에 숫자를 두지 않고 설정에서 읽는다.
 *
 * <p>과금은 아직 켜지 않았다. 사용자에게는 요금제가 없으므로 “한 사람이 들어갈 수 있는 클럽 수”는
 * 무료 기준(free.maxClubsPerUser)을 쓴다.
 */
@Validated
@ConfigurationProperties(prefix = "umatgong.plan")
public record PlanProperties(@Valid @NotNull Limits free, @Valid @NotNull Limits paid) {

	/**
	 * @param maxClubMembers  클럽 정원. 다 차면 새 초대만 막는다. 기존 멤버는 그대로 쓴다
	 * @param maxClubsPerUser 한 사람이 동시에 속할 수 있는 클럽 수
	 */
	public record Limits(@Positive int maxClubMembers, @Positive int maxClubsPerUser) {
	}

	public Limits of(ClubPlan plan) {
		return plan == ClubPlan.PAID ? paid : free;
	}
}

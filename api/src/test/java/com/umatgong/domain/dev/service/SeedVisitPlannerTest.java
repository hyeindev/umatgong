package com.umatgong.domain.dev.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.umatgong.domain.dev.service.SeedVisitPlanner.PlannedVisit;
import com.umatgong.domain.visit.entity.Rating;

class SeedVisitPlannerTest {

	private static final Instant NOW = Instant.parse("2026-10-01T03:00:00Z");
	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	@Test
	void 스무_곳이면_또_갈래_12_괜찮아_6_한_번은_2() {
		assertThat(countByRating(SeedVisitPlanner.plan(20, NOW, 7L)))
			.containsEntry(Rating.AGAIN, 12L).containsEntry(Rating.OKAY, 6L).containsEntry(Rating.NOPE, 2L);
	}

	@ParameterizedTest
	@ValueSource(ints = {15, 16, 17, 18, 19, 20})
	void 평가는_60_30_10에_가깝다(int count) {
		Map<Rating, Long> counts = countByRating(SeedVisitPlanner.plan(count, NOW, 1L));

		assertThat(counts.values().stream().mapToLong(Long::longValue).sum()).isEqualTo(count);
		assertThat(counts.get(Rating.AGAIN)).isEqualTo(Math.round(count * 0.6));
		assertThat(Math.abs(counts.get(Rating.OKAY) - count * 0.3)).isLessThanOrEqualTo(0.5);
		assertThat(counts.get(Rating.NOPE)).isBetween(1L, 2L);
	}

	@ParameterizedTest
	@ValueSource(ints = {15, 20})
	void 방문_시각은_최근_6개월에_퍼져_있고_미래가_아니다(int count) {
		List<Instant> times = SeedVisitPlanner.plan(count, NOW, 3L).stream().map(PlannedVisit::visitedAt).toList();

		assertThat(times).allSatisfy(t -> {
			assertThat(t).isBefore(NOW);
			assertThat(t).isAfter(NOW.minus(Duration.ofDays(SeedVisitPlanner.SPAN_DAYS + 1)));
		});
		// 한쪽에 몰리지 않는다: 최근 한 달과 5~6개월 전 양쪽에 모두 있다
		assertThat(times).anyMatch(t -> t.isAfter(NOW.minus(Duration.ofDays(30))));
		assertThat(times).anyMatch(t -> t.isBefore(NOW.minus(Duration.ofDays(150))));
		// 같은 날 두 번 가지 않는다
		assertThat(times.stream().map(t -> t.atZone(KST).toLocalDate()).distinct()).hasSize(count);
	}

	@Test
	void 방문_시각은_한국_시간_점심이나_저녁이다() {
		assertThat(SeedVisitPlanner.plan(20, NOW, 5L)).allSatisfy(v -> {
			LocalTime time = v.visitedAt().atZone(KST).toLocalTime();
			boolean lunch = !time.isBefore(LocalTime.of(11, 30)) && time.isBefore(LocalTime.of(13, 30));
			boolean dinner = !time.isBefore(LocalTime.of(18, 0)) && time.isBefore(LocalTime.of(20, 30));
			assertThat(lunch || dinner).as("visit time %s", time).isTrue();
		});
	}

	@ParameterizedTest
	@ValueSource(ints = {15, 20})
	void 메모는_절반만_있다(int count) {
		List<PlannedVisit> plan = SeedVisitPlanner.plan(count, NOW, 9L);

		assertThat(plan.stream().filter(v -> v.memo() != null)).hasSize(count / 2);
		assertThat(plan).allSatisfy(v -> {
			if (v.memo() != null) {
				assertThat(v.memo()).isNotBlank().hasSizeLessThanOrEqualTo(200);
			}
		});
	}

	@Test
	void 같은_seed면_같은_결과다() {
		assertThat(SeedVisitPlanner.plan(18, NOW, 42L)).isEqualTo(SeedVisitPlanner.plan(18, NOW, 42L));
		assertThat(SeedVisitPlanner.plan(18, NOW, 42L)).isNotEqualTo(SeedVisitPlanner.plan(18, NOW, 43L));
	}

	private static Map<Rating, Long> countByRating(List<PlannedVisit> plan) {
		return plan.stream().collect(Collectors.groupingBy(PlannedVisit::rating, Collectors.counting()));
	}
}

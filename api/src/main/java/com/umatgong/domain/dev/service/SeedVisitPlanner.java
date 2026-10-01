package com.umatgong.domain.dev.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.umatgong.domain.visit.entity.Rating;

/**
 * 시드 기록의 평가·방문 시각·메모를 정한다. DB와 무관한 순수 계산이라 따로 테스트한다.
 *
 * <ul>
 *   <li>평가: 또 갈래 60% / 괜찮아 30% / 한 번은 10% (반올림, 나머지는 한 번은)</li>
 *   <li>방문 시각: 최근 {@value #SPAN_DAYS}일을 기록 수만큼 구간으로 나눠 구간마다 하루를 고른다.
 *       시각은 한국 시간 점심(11:30~13:30) 또는 저녁(18:00~20:30)</li>
 *   <li>메모: 절반만 (실제로 메모를 안 쓰는 경우가 많다)</li>
 * </ul>
 * 같은 seed면 같은 결과다. 사용자 ID를 seed로 쓰므로 다시 불러도 같은 데이터가 나온다.
 */
public final class SeedVisitPlanner {

	static final int SPAN_DAYS = 180;
	private static final ZoneId KST = ZoneId.of("Asia/Seoul");
	private static final LocalTime LUNCH_START = LocalTime.of(11, 30);
	private static final LocalTime DINNER_START = LocalTime.of(18, 0);
	private static final int LUNCH_WINDOW_MINUTES = 120;
	private static final int DINNER_WINDOW_MINUTES = 150;

	private static final Map<Rating, List<String>> MEMOS = new EnumMap<>(Map.of(
		Rating.AGAIN, List.of(
			"웨이팅 있었는데 기다릴 만했다",
			"다음엔 다 같이 오자",
			"양이 넉넉해서 좋았음",
			"점심 메뉴 가성비 최고",
			"사장님이 친절하심",
			"재방문 확정"),
		Rating.OKAY, List.of(
			"무난하게 괜찮았다",
			"가격 생각하면 나쁘지 않음",
			"근처 오면 또 갈 듯",
			"자리가 좀 좁았다"),
		Rating.NOPE, List.of(
			"기대보다는 평범했다",
			"한 번이면 충분할 듯",
			"대기가 너무 길었다")));

	private SeedVisitPlanner() {
	}

	public record PlannedVisit(Rating rating, Instant visitedAt, String memo) {
	}

	public static List<PlannedVisit> plan(int count, Instant now, long seed) {
		Random random = new Random(seed);
		List<Rating> ratings = ratings(count, random);
		Set<Integer> withMemo = pickHalf(count, random);
		LocalDate today = now.atZone(KST).toLocalDate();

		List<PlannedVisit> visits = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			Instant visitedAt = visitedAt(i, count, today, random);
			Rating rating = ratings.get(i);
			String memo = withMemo.contains(i) ? pick(MEMOS.get(rating), random) : null;
			visits.add(new PlannedVisit(rating, visitedAt, memo));
		}
		return visits;
	}

	private static List<Rating> ratings(int count, Random random) {
		int again = Math.round(count * 0.6f);
		int okay = Math.min(count - again, Math.round(count * 0.3f));
		List<Rating> ratings = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			ratings.add(i < again ? Rating.AGAIN : i < again + okay ? Rating.OKAY : Rating.NOPE);
		}
		Collections.shuffle(ratings, random);
		return ratings;
	}

	private static Set<Integer> pickHalf(int count, Random random) {
		List<Integer> indexes = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			indexes.add(i);
		}
		Collections.shuffle(indexes, random);
		return new HashSet<>(indexes.subList(0, count / 2));
	}

	// i번째 기록은 [i, i+1) 구간 안의 하루. 어제부터 거슬러 올라가므로 미래 시각이 나오지 않는다.
	private static Instant visitedAt(int index, int count, LocalDate today, Random random) {
		double slot = (double) SPAN_DAYS / count;
		int fromDay = (int) Math.floor(index * slot);
		int toDay = Math.max(fromDay + 1, (int) Math.floor((index + 1) * slot));
		int daysAgo = 1 + fromDay + random.nextInt(toDay - fromDay);
		boolean lunch = random.nextBoolean();
		LocalTime time = lunch
			? LUNCH_START.plusMinutes(random.nextInt(LUNCH_WINDOW_MINUTES))
			: DINNER_START.plusMinutes(random.nextInt(DINNER_WINDOW_MINUTES));
		return today.minusDays(daysAgo).atTime(time).atZone(KST).toInstant();
	}

	private static String pick(List<String> values, Random random) {
		return values.get(random.nextInt(values.size()));
	}
}

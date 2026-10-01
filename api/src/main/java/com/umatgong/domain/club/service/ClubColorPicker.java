package com.umatgong.domain.club.service;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Map;

import com.umatgong.domain.club.entity.ClubColor;

/**
 * 새 클럽의 색. 클럽은 한 지도 위에서 색으로 구분하므로(화면기획서 4.1), 만드는 사람이 이미 속한
 * 클럽들과 겹치지 않는 색을 고른다. 팔레트 순서(SAGE → SKY → SAND → LILAC)대로 비어 있는 첫 색,
 * 다 쓰였으면 가장 적게 쓰인 색이다. 팔레트에 빨강 계열은 없다.
 */
public final class ClubColorPicker {

	private ClubColorPicker() {
	}

	public static ClubColor pick(Collection<ClubColor> usedColors) {
		Map<ClubColor, Long> usage = new EnumMap<>(ClubColor.class);
		Arrays.stream(ClubColor.values()).forEach(color -> usage.put(color, 0L));
		usedColors.forEach(color -> usage.merge(color, 1L, Long::sum));
		// EnumMap은 선언 순서를 지키므로 같은 횟수면 팔레트 앞쪽 색이 이긴다.
		return usage.entrySet().stream()
			.min(Comparator.comparingLong(Map.Entry::getValue))
			.orElseThrow()
			.getKey();
	}
}

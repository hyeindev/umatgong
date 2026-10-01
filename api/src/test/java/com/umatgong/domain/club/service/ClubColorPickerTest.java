package com.umatgong.domain.club.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.umatgong.domain.club.entity.ClubColor;

class ClubColorPickerTest {

	@Test
	void 첫_클럽은_팔레트_첫_색이다() {
		assertThat(ClubColorPicker.pick(List.of())).isEqualTo(ClubColor.SAGE);
	}

	@Test
	void 이미_속한_클럽과_겹치지_않는_색을_팔레트_순서대로_고른다() {
		assertThat(ClubColorPicker.pick(List.of(ClubColor.SAGE))).isEqualTo(ClubColor.SKY);
		assertThat(ClubColorPicker.pick(List.of(ClubColor.SAGE, ClubColor.SAND))).isEqualTo(ClubColor.SKY);
	}

	@Test
	void 다_쓰였으면_가장_적게_쓰인_색을_고른다() {
		List<ClubColor> used = List.of(ClubColor.SAGE, ClubColor.SAGE, ClubColor.SKY, ClubColor.SAND, ClubColor.LILAC);

		assertThat(ClubColorPicker.pick(used)).isEqualTo(ClubColor.SKY);
	}

	@Test
	void 팔레트에_빨강_계열이_없다() {
		assertThat(ClubColor.values()).extracting(Enum::name)
			.noneMatch(name -> name.contains("RED") || name.contains("CORAL") || name.contains("PINK"));
	}
}

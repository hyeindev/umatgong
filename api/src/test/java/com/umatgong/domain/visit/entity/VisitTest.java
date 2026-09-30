package com.umatgong.domain.visit.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.umatgong.domain.club.entity.Club;
import com.umatgong.domain.club.entity.ClubColor;
import com.umatgong.domain.place.entity.Place;
import com.umatgong.domain.user.entity.User;
import com.umatgong.global.geo.Coordinate;

class VisitTest {

	private final User user = User.signUpWithKakao(1L, "지현", null);
	private final Club clubA = Club.create("동네친구들", ClubColor.SAGE, user, "invite-a");
	private final Club clubB = Club.create("회사", ClubColor.SKY, user, "invite-b");
	private final Coordinate here = Coordinate.of(37.556, 126.914);
	private final Place kakaoPlace = Place.fromKakao("123", "망원동 김반장", null, here, "곱창", List.of(), user);

	@Test
	void 클럽이_없으면_PRIVATE로만_기록할_수_있다() {
		Visit visit = Visit.record(user, kakaoPlace, null, Rating.AGAIN, null, Instant.now(), Visibility.PRIVATE);
		assertThat(visit.getVisibility()).isEqualTo(Visibility.PRIVATE);

		assertThatThrownBy(() ->
			Visit.record(user, kakaoPlace, null, Rating.AGAIN, null, Instant.now(), Visibility.CLUB))
			.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> visit.changeVisibility(Visibility.CLUB))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void 커스텀_장소는_만든_클럽의_기록에만_붙는다() {
		Place custom = Place.custom(clubA, "골목 포차", null, here, user);

		Visit ok = Visit.record(user, custom, clubA, Rating.OKAY, null, Instant.now(), Visibility.CLUB);
		assertThat(ok.getClub()).isSameAs(clubA);

		assertThatThrownBy(() ->
			Visit.record(user, custom, clubB, Rating.OKAY, null, Instant.now(), Visibility.CLUB))
			.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() ->
			Visit.record(user, custom, null, Rating.OKAY, null, Instant.now(), Visibility.PRIVATE))
			.isInstanceOf(IllegalArgumentException.class);
	}
}

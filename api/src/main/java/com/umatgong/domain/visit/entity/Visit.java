package com.umatgong.domain.visit.entity;

import java.time.Instant;
import java.util.Objects;

import org.hibernate.annotations.CreationTimestamp;

import com.umatgong.domain.club.entity.Club;
import com.umatgong.domain.place.entity.Place;
import com.umatgong.domain.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "visits")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Visit {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "place_id", nullable = false)
	private Place place;

	// 클럽 없이도 기록할 수 있다("혼자서도 완결"). 그 경우 PRIVATE만 가능하다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "club_id")
	private Club club;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 5)
	private Rating rating;

	@Column(length = 200)
	private String memo;

	@Column(nullable = false)
	private Instant visitedAt;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private Visibility visibility;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	private Visit(User user, Place place, Club club, Rating rating, String memo, Instant visitedAt,
		Visibility visibility) {
		this.user = Objects.requireNonNull(user, "user");
		this.place = Objects.requireNonNull(place, "place");
		this.club = club;
		this.rating = Objects.requireNonNull(rating, "rating");
		this.memo = memo;
		this.visitedAt = Objects.requireNonNull(visitedAt, "visitedAt");
		this.visibility = Objects.requireNonNull(visibility, "visibility");
		requireClubForClubVisibility(club, visibility);
		// 커스텀 장소는 만든 클럽 밖으로 새면 안 된다. 다른 클럽의 기록에 붙는 것도 막는다.
		if (place.isCustom() && !sameClub(place.getClub(), club)) {
			throw new IllegalArgumentException("Custom place belongs to another club");
		}
	}

	/** 클럽 멤버십 검증은 서비스가 먼저 끝낸 뒤 호출한다. */
	public static Visit record(User user, Place place, Club club, Rating rating, String memo, Instant visitedAt,
		Visibility visibility) {
		return new Visit(user, place, club, rating, memo, visitedAt, visibility);
	}

	public void changeRating(Rating rating) {
		this.rating = Objects.requireNonNull(rating, "rating");
	}

	public void editMemo(String memo) {
		this.memo = memo;
	}

	public void changeVisibility(Visibility visibility) {
		requireClubForClubVisibility(club, visibility);
		this.visibility = visibility;
	}

	public boolean isWrittenBy(Long userId) {
		return user.getId().equals(userId);
	}

	// 프록시와 실제 엔티티가 섞여 들어올 수 있어 인스턴스가 아니라 ID로 비교한다.
	// 저장 전(ID 없음)이면 같은 인스턴스일 때만 같은 클럽으로 본다.
	private static boolean sameClub(Club a, Club b) {
		if (a == null || b == null) {
			return false;
		}
		if (a == b) {
			return true;
		}
		return a.getId() != null && a.getId().equals(b.getId());
	}

	private static void requireClubForClubVisibility(Club club, Visibility visibility) {
		if (visibility == Visibility.CLUB && club == null) {
			throw new IllegalArgumentException("CLUB visibility requires a club");
		}
	}
}

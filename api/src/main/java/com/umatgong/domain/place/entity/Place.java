package com.umatgong.domain.place.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.umatgong.domain.club.entity.Club;
import com.umatgong.domain.user.entity.User;
import com.umatgong.global.geo.Coordinate;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
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
@Table(name = "places")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Place {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(length = 255)
	private String address;

	@Embedded
	@AttributeOverride(name = "point", column = @Column(name = "coordinate", nullable = false))
	private Coordinate coordinate;

	@Column(length = 100)
	private String category;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private PlaceSource source;

	@Column(length = 50)
	private String externalId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "created_by")
	private User createdBy;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "club_id")
	private Club club;

	@JdbcTypeCode(SqlTypes.ARRAY)
	@Column(nullable = false)
	private List<String> categoryTags = new ArrayList<>();

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	private Place(String name, String address, Coordinate coordinate, String category, PlaceSource source,
		String externalId, User createdBy, Club club, List<String> categoryTags) {
		this.name = name;
		this.address = address;
		this.coordinate = Objects.requireNonNull(coordinate, "coordinate");
		this.category = category;
		this.source = source;
		this.externalId = externalId;
		this.createdBy = createdBy;
		this.club = club;
		this.categoryTags = new ArrayList<>(categoryTags);
	}

	/**
	 * 카카오 장소 검색 결과를 캐싱한다. 같은 externalId가 이미 있으면 새로 만들지 말고 그 행을 쓴다.
	 *
	 * @param requestedBy 처음 이 장소를 불러온 사용자. 기록용이며 권한과는 무관하다.
	 */
	public static Place fromKakao(String externalId, String name, String address, Coordinate coordinate,
		String category, List<String> categoryTags, User requestedBy) {
		Objects.requireNonNull(externalId, "externalId");
		return new Place(name, address, coordinate, category, PlaceSource.API, externalId, requestedBy, null,
			categoryTags);
	}

	/** 사용자가 직접 입력한 장소. 만든 클럽 밖으로는 노출되지 않는다. */
	public static Place custom(Club club, String name, String address, Coordinate coordinate, User createdBy) {
		Objects.requireNonNull(club, "club");
		return new Place(name, address, coordinate, null, PlaceSource.USER, null, createdBy, club, List.of());
	}

	public boolean isCustom() {
		return source == PlaceSource.USER;
	}
}

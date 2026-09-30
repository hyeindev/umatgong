package com.umatgong.domain.visit.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
@Table(name = "visit_photos")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VisitPhoto {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "visit_id", nullable = false)
	private Visit visit;

	// 원본은 서버에 올리지 않는다. 썸네일 주소만.
	@Column(nullable = false, length = 500)
	private String thumbUrl;

	private Instant takenAt;

	@Embedded
	@AttributeOverride(name = "point", column = @Column(name = "exif_coordinate"))
	private Coordinate exifCoordinate;

	// 검색 인덱스일 뿐이다. 화면에 확정 정보처럼 내보내지 않는다.
	@JdbcTypeCode(SqlTypes.ARRAY)
	@Column(nullable = false)
	private List<String> tags = new ArrayList<>();

	@Enumerated(EnumType.STRING)
	@Column(length = 10)
	private TagSource tagSource;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	private VisitPhoto(Visit visit, String thumbUrl, Instant takenAt, Coordinate exifCoordinate) {
		this.visit = Objects.requireNonNull(visit, "visit");
		this.thumbUrl = Objects.requireNonNull(thumbUrl, "thumbUrl");
		this.takenAt = takenAt;
		this.exifCoordinate = exifCoordinate;
	}

	/** @param exifCoordinate 위치 정보가 없는 사진이면 null */
	public static VisitPhoto attach(Visit visit, String thumbUrl, Instant takenAt, Coordinate exifCoordinate) {
		return new VisitPhoto(visit, thumbUrl, takenAt, exifCoordinate);
	}

	/**
	 * 확신도 필터는 호출하는 쪽에서 이미 거친 태그만 넘긴다. 빈 목록이면 태그와 출처를 함께 비운다.
	 */
	public void applyTags(List<String> tags, TagSource source) {
		if (tags.isEmpty()) {
			clearTags();
			return;
		}
		this.tags = new ArrayList<>(tags);
		this.tagSource = Objects.requireNonNull(source, "source");
	}

	public void clearTags() {
		this.tags = new ArrayList<>();
		this.tagSource = null;
	}
}

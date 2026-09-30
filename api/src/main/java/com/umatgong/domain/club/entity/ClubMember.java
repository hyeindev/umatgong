package com.umatgong.domain.club.entity;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import com.umatgong.domain.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "club_members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClubMember {

	@EmbeddedId
	private ClubMemberId id;

	@MapsId("clubId")
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "club_id")
	private Club club;

	@MapsId("userId")
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private User user;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private Instant joinedAt;

	private ClubMember(Club club, User user) {
		this.id = new ClubMemberId(club.getId(), user.getId());
		this.club = club;
		this.user = user;
	}

	public static ClubMember join(Club club, User user) {
		return new ClubMember(club, user);
	}
}

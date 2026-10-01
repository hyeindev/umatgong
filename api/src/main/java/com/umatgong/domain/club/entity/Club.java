package com.umatgong.domain.club.entity;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

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
@Table(name = "clubs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Club {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 30)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private ClubColor color;

	// 클럽장. 탈퇴하면 NULL이 된다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "created_by")
	private User createdBy;

	@Column(nullable = false, unique = true, length = 32)
	private String inviteCode;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private ClubPlan plan;

	private Instant planExpiresAt;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	private Club(String name, ClubColor color, User createdBy, String inviteCode) {
		this.name = name;
		this.color = color;
		this.createdBy = createdBy;
		this.inviteCode = inviteCode;
		this.plan = ClubPlan.FREE;
	}

	public static Club create(String name, ClubColor color, User owner, String inviteCode) {
		return new Club(name, color, owner, inviteCode);
	}

	public void rename(String name) {
		this.name = name;
	}

	public void changeColor(ClubColor color) {
		this.color = color;
	}

	// 링크가 유출됐을 때 기존 초대 링크를 무효화하는 용도.
	public void reissueInviteCode(String inviteCode) {
		this.inviteCode = inviteCode;
	}

	// 클럽장이 탈퇴하면 클럽은 남기고 클럽장 자리만 비운다 (멤버와 기록은 그대로).
	public void releaseOwner() {
		this.createdBy = null;
	}

	public boolean isOwnedBy(Long userId) {
		return createdBy != null && createdBy.getId().equals(userId);
	}
}

package com.umatgong.domain.club.dto;

import com.umatgong.domain.club.entity.ClubColor;

import jakarta.validation.constraints.NotNull;

/** @param color 바꿀 클럽 색. 팔레트(SAGE·SKY·SAND·LILAC) 밖의 값은 400이다 */
public record UpdateClubRequest(@NotNull ClubColor color) {
}

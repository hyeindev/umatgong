package com.umatgong.domain.club.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** @param name 클럽 이름. 앞뒤 공백은 서버가 지운다 */
public record CreateClubRequest(@NotBlank @Size(max = 30) String name) {
}

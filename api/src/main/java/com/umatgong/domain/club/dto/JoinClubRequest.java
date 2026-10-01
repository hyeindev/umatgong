package com.umatgong.domain.club.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JoinClubRequest(@NotBlank @Size(max = 64) String inviteCode) {
}

package com.umatgong.domain.place.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 「여기 없어요」로 직접 입력하는 가게. 이 클럽 안에서만 보인다.
 *
 * @param clubId 내가 멤버인 클럽
 * @param name   가게 이름. 앞뒤 공백은 서버가 지운다
 */
public record CreateCustomPlaceRequest(
	@NotNull Long clubId,
	@NotBlank @Size(max = 100) String name,
	@Size(max = 255) String address,
	@NotNull @Valid LatLng coordinate
) {

	public record LatLng(
		@NotNull @DecimalMin("-90") @DecimalMax("90") Double lat,
		@NotNull @DecimalMin("-180") @DecimalMax("180") Double lng) {
	}
}

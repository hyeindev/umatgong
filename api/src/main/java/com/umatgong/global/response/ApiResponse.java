package com.umatgong.global.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.umatgong.global.error.ErrorCode;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, T data, ErrorBody error) {

	public static <T> ApiResponse<T> ok(T data) {
		return new ApiResponse<>(true, data, null);
	}

	public static ApiResponse<Void> ok() {
		return new ApiResponse<>(true, null, null);
	}

	public static ApiResponse<Void> fail(ErrorCode errorCode) {
		return fail(errorCode, errorCode.getMessage());
	}

	public static ApiResponse<Void> fail(ErrorCode errorCode, String message) {
		return new ApiResponse<>(false, null, new ErrorBody(errorCode.name(), message, null));
	}

	public static ApiResponse<Void> fail(ErrorCode errorCode, List<FieldError> fieldErrors) {
		return new ApiResponse<>(false, null,
			new ErrorBody(errorCode.name(), errorCode.getMessage(), fieldErrors));
	}

	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	public record ErrorBody(String code, String message, List<FieldError> fieldErrors) {
	}

	public record FieldError(String field, String reason) {
	}
}

package com.umatgong.global.error;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.umatgong.global.response.ApiResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException e) {
		ErrorCode code = e.getErrorCode();
		return ResponseEntity.status(code.getStatus()).body(ApiResponse.fail(code, e.getMessage()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Void>> handleInvalidBody(MethodArgumentNotValidException e) {
		List<ApiResponse.FieldError> fieldErrors = e.getBindingResult().getFieldErrors().stream()
			.map(fe -> new ApiResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
			.toList();
		return respond(ErrorCode.INVALID_INPUT, ApiResponse.fail(ErrorCode.INVALID_INPUT, fieldErrors));
	}

	@ExceptionHandler({
		HandlerMethodValidationException.class,
		MissingServletRequestParameterException.class,
		MethodArgumentTypeMismatchException.class,
		HttpMessageNotReadableException.class
	})
	public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception e) {
		return respond(ErrorCode.INVALID_INPUT);
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ApiResponse<Void>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException e) {
		return respond(ErrorCode.METHOD_NOT_ALLOWED);
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ApiResponse<Void>> handleNotFound(NoResourceFoundException e) {
		return respond(ErrorCode.NOT_FOUND);
	}

	// 메서드 보안(@PreAuthorize 등)에서 던진 거부. 필터 단계의 거부는 SecurityConfig가 처리한다.
	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException e) {
		return respond(ErrorCode.FORBIDDEN);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception e) {
		log.error("Unhandled exception", e);
		return respond(ErrorCode.INTERNAL_ERROR);
	}

	private ResponseEntity<ApiResponse<Void>> respond(ErrorCode code) {
		return respond(code, ApiResponse.fail(code));
	}

	private ResponseEntity<ApiResponse<Void>> respond(ErrorCode code, ApiResponse<Void> body) {
		return ResponseEntity.status(code.getStatus()).body(body);
	}
}

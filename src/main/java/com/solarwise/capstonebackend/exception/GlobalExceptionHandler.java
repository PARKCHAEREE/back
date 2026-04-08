package com.solarwise.capstonebackend.exception;

import com.solarwise.capstonebackend.dto.ApiErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.ArrayList;
import java.util.List;

/**
 * 전역 예외 처리 클래스
 * - @RestControllerAdvice를 사용한 일관된 에러 응답 처리
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 비즈니스 로직 예외 처리
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessException(BusinessException ex, WebRequest request) {
        log.warn("비즈니스 예외 발생: {}", ex.getMessage());
        return new ResponseEntity<>(
                ApiErrorResponse.error("BUSINESS_ERROR", ex.getMessage()),
                HttpStatus.BAD_REQUEST
        );
    }

    /**
     * 리소스 없음 예외 처리
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        log.warn("리소스 없음: {}", ex.getMessage());
        return new ResponseEntity<>(
                ApiErrorResponse.error("NOT_FOUND", ex.getMessage()),
                HttpStatus.NOT_FOUND
        );
    }

    /**
     * 필드 검증 예외 처리
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(MethodArgumentNotValidException ex, WebRequest request) {
        log.error("필드 검증 예외 발생");
        List<ApiErrorResponse.FieldError> details = new ArrayList<>();

        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String message = error.getDefaultMessage();
            details.add(ApiErrorResponse.FieldError.builder()
                    .field(fieldName)
                    .reason(message)
                    .build());
        });

        return new ResponseEntity<>(
                ApiErrorResponse.error("VALIDATION_ERROR", "요청 값이 올바르지 않습니다.", details),
                HttpStatus.BAD_REQUEST
        );
    }

    /**
     * 권한 없음 예외 처리
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        log.error("권한 없음 예외 발생: {}", ex.getMessage());
        return new ResponseEntity<>(
                ApiErrorResponse.error("FORBIDDEN", "접근 권한이 없습니다."),
                HttpStatus.FORBIDDEN
        );
    }

    /**
     * 404 예외 처리
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoHandlerFoundException(NoHandlerFoundException ex, WebRequest request) {
        log.error("404 에러: {}", ex.getRequestURL());
        return new ResponseEntity<>(
                ApiErrorResponse.error("NOT_FOUND", "요청한 리소스를 찾을 수 없습니다."),
                HttpStatus.NOT_FOUND
        );
    }

    /**
     * 일반 예외 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGlobalException(Exception ex, WebRequest request) {
        log.error("예상치 못한 예외 발생", ex);
        return new ResponseEntity<>(
                ApiErrorResponse.error("INTERNAL_SERVER_ERROR", "서버 오류가 발생했습니다."),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

}


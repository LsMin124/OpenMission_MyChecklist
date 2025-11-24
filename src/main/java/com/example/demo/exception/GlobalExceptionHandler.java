package com.example.demo.exception;

import com.example.demo.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // CustomException 처리 (ErrorCode 활용)
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Object>> handleCustomException(CustomException ex) {
        return ApiResponse.error(ex.getErrorCode());
    }

    // 정적 리소스 없음 (404) 처리
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNoResourceFoundException(NoResourceFoundException ex) {
        return ApiResponse.error(ErrorCode.RESOURCE_NOT_FOUND);
    }

    // 그 외 모든 예외 (500)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleException(Exception ex) {
        // 실제 운영에선 에러 로그를 남겨야 함 (log.error)
        return ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR, ex.getMessage());
    }
}
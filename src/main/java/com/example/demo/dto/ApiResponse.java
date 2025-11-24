package com.example.demo.dto;

import com.example.demo.exception.ErrorCode;
import com.example.demo.exception.SuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.ResponseEntity;

@Getter
@AllArgsConstructor
public class ApiResponse<T> {

    private final int status;
    private final String message;
    private final T data;

    // 성공 응답 생성 (데이터 있음)
    public static <T> ResponseEntity<ApiResponse<T>> success(SuccessCode successCode, T data) {
        return ResponseEntity
                .status(successCode.getHttpStatus())
                .body(new ApiResponse<>(successCode.getHttpStatus().value(), successCode.getMessage(), data));
    }

    // 성공 응답 생성 (데이터 없음 - 예: 삭제/수정)
    public static <T> ResponseEntity<ApiResponse<T>> success(SuccessCode successCode) {
        return ResponseEntity
                .status(successCode.getHttpStatus())
                .body(new ApiResponse<>(successCode.getHttpStatus().value(), successCode.getMessage(), null));
    }

    // 에러 응답 생성 (GlobalExceptionHandler에서 사용)
    public static ResponseEntity<ApiResponse<Object>> error(ErrorCode errorCode) {
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(new ApiResponse<>(errorCode.getHttpStatus().value(), errorCode.getMessage(), null));
    }

    // 에러 응답 생성 (메시지 직접 입력)
    public static ResponseEntity<ApiResponse<Object>> error(ErrorCode errorCode, String message) {
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(new ApiResponse<>(errorCode.getHttpStatus().value(), message, null));
    }
}
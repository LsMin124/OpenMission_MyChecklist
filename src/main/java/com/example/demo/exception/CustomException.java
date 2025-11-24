package com.example.demo.exception;

import lombok.Getter;

@Getter
public class CustomException extends RuntimeException {

    private final ErrorCode errorCode;

    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage()); // RuntimeException 메시지로도 저장
        this.errorCode = errorCode;
    }
}
package com.example.demo.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum SuccessCode {

    // 200 OK
    SELECT_SUCCESS(HttpStatus.OK, "조회 성공"),
    UPDATE_SUCCESS(HttpStatus.OK, "수정 성공"),
    DELETE_SUCCESS(HttpStatus.OK, "삭제 성공"),
    LOGIN_SUCCESS(HttpStatus.OK, "로그인 성공"),

    // 201 Created
    CREATE_SUCCESS(HttpStatus.CREATED, "생성 성공"),
    signup_SUCCESS(HttpStatus.CREATED, "회원가입 성공");

    private final HttpStatus httpStatus;
    private final String message;
}
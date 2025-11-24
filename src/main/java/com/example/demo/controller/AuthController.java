package com.example.demo.controller;

import com.example.demo.config.JwtTokenProvider;
import com.example.demo.domain.User;
import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.AuthDto;
import com.example.demo.exception.CustomException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.exception.SuccessCode;
import com.example.demo.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    // 회원가입
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<String>> signup(@RequestBody @Valid AuthDto.SignupRequest request) {

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .build();

        userRepository.save(user);
        return ApiResponse.success(SuccessCode.SIGNUP_SUCCESS);
    }

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthDto.TokenResponse>> login(@RequestBody @Valid AuthDto.LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 비밀번호 체크
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        // 토큰 발급
        String token = jwtTokenProvider.createToken(user.getId());
        return ApiResponse.success(SuccessCode.LOGIN_SUCCESS, new AuthDto.TokenResponse(token));
    }
}

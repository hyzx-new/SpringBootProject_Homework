package com.example.answer.controller;

import com.example.answer.common.ApiResponse;
import com.example.answer.dto.AuthUser;
import com.example.answer.dto.LoginRequest;
import com.example.answer.dto.LoginResponse;
import com.example.answer.dto.RegisterRequest;
import com.example.answer.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }
    /**
    * @ Register
     * 注册
    * */
    @PostMapping("/register")
    public ApiResponse<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(authService.register(request));
    }
    /**
     * @ Login
     * 登录
     * */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }
    /**
     * @ Logout
     * 退出登录
     * */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        authService.logout(authorization);
        return ApiResponse.ok();
    }

    @GetMapping("/me")
    public ApiResponse<AuthUser> me() {
        return ApiResponse.ok(authService.me());
    }
}

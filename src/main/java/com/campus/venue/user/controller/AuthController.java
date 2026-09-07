package com.campus.venue.user.controller;

import com.campus.venue.common.api.Result;
import com.campus.venue.security.LoginUser;
import com.campus.venue.security.SecurityUtils;
import com.campus.venue.user.dto.CurrentUserResponse;
import com.campus.venue.user.dto.LoginRequest;
import com.campus.venue.user.dto.LoginResponse;
import com.campus.venue.user.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "1. 登录")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @SecurityRequirements
    @Operation(summary = "登录，返回 token；把 token 填到右上角 Authorize")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
    }

    @GetMapping("/me")
    public Result<CurrentUserResponse> me() {
        LoginUser user = SecurityUtils.requireCurrentUser();
        return Result.ok(new CurrentUserResponse(user.getUserId(), user.getUsername(), user.getRole()));
    }
}

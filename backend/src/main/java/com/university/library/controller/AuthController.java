package com.university.library.controller;

import com.university.library.common.OperationResult;
import com.university.library.common.Result;
import com.university.library.common.security.UserPrincipal;
import com.university.library.common.util.WebUtils;
import com.university.library.dto.ChangePasswordRequest;
import com.university.library.dto.LoginRequest;
import com.university.library.service.AuthService;
import com.university.library.vo.LoginVO;
import com.university.library.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口：/api/auth/**。
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpServletRequest) {
        OperationResult<LoginVO> result = authService.login(request, WebUtils.getClientIp(httpServletRequest));
        return Result.success(result.data(), result.message());
    }

    @GetMapping("/me")
    public Result<UserVO> me(@AuthenticationPrincipal UserPrincipal principal) {
        return Result.success(authService.currentUser(principal));
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        OperationResult<Void> result = authService.logout();
        return Result.success(null, result.message());
    }

    @PutMapping("/password")
    public Result<Void> changePassword(@AuthenticationPrincipal UserPrincipal principal,
                                       @Valid @RequestBody ChangePasswordRequest request) {
        OperationResult<Void> result = authService.changePassword(principal, request);
        return Result.success(null, result.message());
    }
}

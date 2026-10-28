package com.university.library.controller;

import com.university.library.common.Result;
import com.university.library.common.security.UserPrincipal;
import com.university.library.service.DashboardService;
import com.university.library.vo.NotificationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 仪表盘与通知接口。
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard(@AuthenticationPrincipal UserPrincipal principal) {
        return Result.success(dashboardService.dashboard(principal));
    }

    @GetMapping("/notifications")
    public Result<NotificationVO> notifications(@AuthenticationPrincipal UserPrincipal principal) {
        return Result.success(dashboardService.notifications(principal));
    }
}

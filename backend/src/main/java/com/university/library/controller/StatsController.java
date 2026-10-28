package com.university.library.controller;

import com.university.library.common.Result;
import com.university.library.service.StatsService;
import com.university.library.vo.AdminOverviewVO;
import com.university.library.vo.OverdueStatsVO;
import com.university.library.vo.RankingVO;
import com.university.library.vo.TrendsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 统计接口：/api/stats/**，仅管理员可访问。
 */
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/overview")
    public Result<AdminOverviewVO> overview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.success(statsService.adminOverview(startDate, endDate));
    }

    @GetMapping("/trends")
    public Result<TrendsVO> trends() {
        return Result.success(statsService.trends());
    }

    @GetMapping("/ranking")
    public Result<RankingVO> ranking(
            @RequestParam(defaultValue = "8") Integer limit,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.success(statsService.ranking(limit, startDate, endDate));
    }

    @GetMapping("/overdue")
    public Result<OverdueStatsVO> overdue() {
        return Result.success(statsService.overdueStats(6));
    }
}

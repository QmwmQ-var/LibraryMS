package com.university.library.controller;

import com.university.library.common.OperationResult;
import com.university.library.common.PageResult;
import com.university.library.common.Result;
import com.university.library.common.security.UserPrincipal;
import com.university.library.service.PenaltyService;
import com.university.library.vo.PenaltySummaryVO;
import com.university.library.vo.PenaltyVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 罚款接口：/api/penalties。
 */
@RestController
@RequestMapping("/api/penalties")
@RequiredArgsConstructor
public class PenaltyController {

    private final PenaltyService penaltyService;

    @GetMapping("/summary")
    public Result<PenaltySummaryVO> summary(@AuthenticationPrincipal UserPrincipal principal) {
        return Result.success(penaltyService.summary(principal));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('READER')")
    public Result<PageResult<PenaltyVO>> my(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        return Result.success(penaltyService.list(principal, true, status, keyword, page, size));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PageResult<PenaltyVO>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        return Result.success(penaltyService.list(principal, false, status, keyword, page, size));
    }

    @PostMapping("/{id}/pay")
    public Result<PenaltyVO> pay(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        OperationResult<PenaltyVO> result = penaltyService.pay(principal, id);
        return Result.success(result.data(), result.message());
    }
}

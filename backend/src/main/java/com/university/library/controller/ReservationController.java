package com.university.library.controller;

import com.university.library.common.OperationResult;
import com.university.library.common.PageResult;
import com.university.library.common.Result;
import com.university.library.common.security.UserPrincipal;
import com.university.library.dto.ReservationRequest;
import com.university.library.service.ReservationService;
import com.university.library.vo.ReservationSummaryVO;
import com.university.library.vo.ReservationVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 预约接口：/api/reservations。
 */
@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    /**
     * 当前用户视角的预约汇总。
     */
    @GetMapping("/summary")
    public Result<ReservationSummaryVO> summary(@AuthenticationPrincipal UserPrincipal principal) {
        return Result.success(reservationService.summary(principal));
    }

    /**
     * 我的预约（读者）。
     */
    @GetMapping("/my")
    @PreAuthorize("hasRole('READER')")
    public Result<PageResult<ReservationVO>> my(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        return Result.success(reservationService.list(principal, true, status, keyword, page, size));
    }

    /**
     * 全部预约（管理员）。
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PageResult<ReservationVO>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        return Result.success(reservationService.list(principal, false, status, keyword, page, size));
    }

    /**
     * 读者对无可借副本的图书发起预约。
     */
    @PostMapping
    @PreAuthorize("hasRole('READER')")
    public Result<ReservationVO> create(@AuthenticationPrincipal UserPrincipal principal,
                                        @Valid @RequestBody ReservationRequest request) {
        OperationResult<ReservationVO> result = reservationService.create(principal, request);
        return Result.success(result.data(), result.message());
    }

    /**
     * 取消预约：读者本人或管理员。
     */
    @PostMapping("/{id}/cancel")
    public Result<ReservationVO> cancel(@AuthenticationPrincipal UserPrincipal principal,
                                        @PathVariable Long id) {
        OperationResult<ReservationVO> result = reservationService.cancel(principal, id);
        return Result.success(result.data(), result.message());
    }
}

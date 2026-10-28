package com.university.library.controller;

import com.university.library.common.OperationResult;
import com.university.library.common.PageResult;
import com.university.library.common.Result;
import com.university.library.common.security.UserPrincipal;
import com.university.library.dto.BorrowRequest;
import com.university.library.service.BorrowService;
import com.university.library.vo.BorrowRecordDetailVO;
import com.university.library.vo.BorrowRecordVO;
import com.university.library.vo.BorrowResultVO;
import com.university.library.vo.ReturnResultVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 借阅接口：/api/borrow-records。
 */
@RestController
@RequestMapping("/api/borrow-records")
@RequiredArgsConstructor
public class BorrowRecordController {

    private final BorrowService borrowService;

    /**
     * 我的借阅（读者）。
     */
    @GetMapping("/my")
    @PreAuthorize("hasRole('READER')")
    public Result<PageResult<BorrowRecordVO>> my(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.success(borrowService.list(principal, true, null, null, status, keyword,
                startDate, endDate, page, size));
    }

    /**
     * 借阅记录列表（管理员）。
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PageResult<BorrowRecordVO>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Long readerId,
            @RequestParam(required = false) Long bookId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.success(borrowService.list(principal, false, readerId, bookId, status, keyword,
                startDate, endDate, page, size));
    }

    @GetMapping("/{id}")
    public Result<BorrowRecordDetailVO> detail(@AuthenticationPrincipal UserPrincipal principal,
                                               @PathVariable Long id) {
        return Result.success(borrowService.detail(principal, id));
    }

    @PostMapping
    public Result<BorrowResultVO> borrow(@AuthenticationPrincipal UserPrincipal principal,
                                         @Valid @RequestBody BorrowRequest request) {
        OperationResult<BorrowResultVO> result = borrowService.borrow(principal, request);
        return Result.success(result.data(), result.message());
    }

    @PostMapping("/{id}/return")
    public Result<ReturnResultVO> returnBook(@AuthenticationPrincipal UserPrincipal principal,
                                             @PathVariable Long id) {
        OperationResult<ReturnResultVO> result = borrowService.returnBook(principal, id);
        return Result.success(result.data(), result.message());
    }

    @PostMapping("/{id}/renew")
    public Result<BorrowRecordVO> renew(@AuthenticationPrincipal UserPrincipal principal,
                                        @PathVariable Long id) {
        OperationResult<BorrowRecordVO> result = borrowService.renew(principal, id);
        return Result.success(result.data(), result.message());
    }
}

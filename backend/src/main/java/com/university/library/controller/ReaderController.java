package com.university.library.controller;

import com.university.library.common.OperationResult;
import com.university.library.common.PageResult;
import com.university.library.common.Result;
import com.university.library.common.security.UserPrincipal;
import com.university.library.dto.CardStatusRequest;
import com.university.library.dto.ReaderRequest;
import com.university.library.service.ReaderService;
import com.university.library.vo.ReaderDetailVO;
import com.university.library.vo.ReaderVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 读者接口：/api/readers。
 */
@RestController
@RequestMapping("/api/readers")
@RequiredArgsConstructor
public class ReaderController {

    private final ReaderService readerService;

    /**
     * 读者自助借阅证。
     */
    @GetMapping("/me")
    @PreAuthorize("hasRole('READER')")
    public Result<ReaderVO> me(@AuthenticationPrincipal UserPrincipal principal) {
        return Result.success(readerService.me(principal));
    }

    /**
     * 读者列表（管理员）。
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PageResult<ReaderVO>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String cardStatus,
            @RequestParam(required = false) Boolean hasOverdue) {
        return Result.success(readerService.list(keyword, type, cardStatus, hasOverdue, page, size));
    }

    /**
     * 读者详情：管理员可查看全部，读者仅能查看自己。
     */
    @GetMapping("/{id}")
    public Result<ReaderDetailVO> detail(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        return Result.success(readerService.detail(principal, id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<ReaderVO> create(@Valid @RequestBody ReaderRequest request) {
        OperationResult<ReaderVO> result = readerService.create(request);
        return Result.success(result.data(), result.message());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<ReaderVO> update(@PathVariable Long id, @Valid @RequestBody ReaderRequest request) {
        OperationResult<ReaderVO> result = readerService.update(id, request);
        return Result.success(result.data(), result.message());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        OperationResult<Void> result = readerService.delete(id);
        return Result.success(null, result.message());
    }

    @PutMapping("/{id}/card")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<ReaderVO> updateCard(@PathVariable Long id, @Valid @RequestBody CardStatusRequest request) {
        OperationResult<ReaderVO> result = readerService.updateCardStatus(id, request);
        return Result.success(result.data(), result.message());
    }
}

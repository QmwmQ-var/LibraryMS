package com.university.library.controller;

import com.university.library.common.OperationResult;
import com.university.library.common.PageResult;
import com.university.library.common.Result;
import com.university.library.dto.BookRequest;
import com.university.library.service.BookService;
import com.university.library.vo.BookVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
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
 * 图书接口：/api/books。
 */
@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    @GetMapping
    public Result<PageResult<BookVO>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String availability,
            @RequestParam(required = false) String sort) {
        return Result.success(bookService.list(keyword, categoryId, availability, sort, page, size));
    }

    @GetMapping("/{id}")
    public Result<BookVO> detail(@PathVariable Long id) {
        return Result.success(bookService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<BookVO> create(@Valid @RequestBody BookRequest request) {
        OperationResult<BookVO> result = bookService.create(request);
        return Result.success(result.data(), result.message());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<BookVO> update(@PathVariable Long id, @Valid @RequestBody BookRequest request) {
        OperationResult<BookVO> result = bookService.update(id, request);
        return Result.success(result.data(), result.message());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        OperationResult<Void> result = bookService.delete(id);
        return Result.success(null, result.message());
    }
}

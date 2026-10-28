package com.university.library.controller;

import com.university.library.common.OperationResult;
import com.university.library.common.Result;
import com.university.library.dto.CategoryRequest;
import com.university.library.service.CategoryService;
import com.university.library.vo.CategoryVO;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 图书分类接口：/api/categories。
 */
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public Result<List<CategoryVO>> list() {
        return Result.success(categoryService.list());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<CategoryVO> create(@Valid @RequestBody CategoryRequest request) {
        OperationResult<CategoryVO> result = categoryService.create(request);
        return Result.success(result.data(), result.message());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<CategoryVO> update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        OperationResult<CategoryVO> result = categoryService.update(id, request);
        return Result.success(result.data(), result.message());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        OperationResult<Void> result = categoryService.delete(id);
        return Result.success(null, result.message());
    }
}

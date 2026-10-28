package com.university.library.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.university.library.common.BusinessException;
import com.university.library.common.OperationResult;
import com.university.library.common.util.TextUtils;
import com.university.library.dto.CategoryRequest;
import com.university.library.entity.Book;
import com.university.library.entity.BookCategory;
import com.university.library.entity.BorrowRecord;
import com.university.library.mapper.BookCategoryMapper;
import com.university.library.mapper.BookMapper;
import com.university.library.mapper.BorrowRecordMapper;
import com.university.library.vo.CategoryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 图书分类服务：分类 CRUD 与分类下的图书/馆藏/借阅统计。
 */
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final BookCategoryMapper bookCategoryMapper;
    private final BookMapper bookMapper;
    private final BorrowRecordMapper borrowRecordMapper;

    /**
     * 分类列表：附带图书种数、馆藏册数、借阅次数。
     */
    public List<CategoryVO> list() {
        List<BookCategory> categories = bookCategoryMapper.selectList(
                Wrappers.<BookCategory>lambdaQuery().orderByAsc(BookCategory::getId));
        List<Book> books = bookMapper.selectList(null);
        List<BorrowRecord> records = borrowRecordMapper.selectList(null);

        Map<Long, List<Book>> booksByCategory = books.stream()
                .collect(Collectors.groupingBy(Book::getCategoryId));
        Set<Long> recordBookIds = records.stream().map(BorrowRecord::getBookId).collect(Collectors.toSet());

        return categories.stream().map(category -> {
            List<Book> categoryBooks = booksByCategory.getOrDefault(category.getId(), List.of());
            Set<Long> bookIds = categoryBooks.stream().map(Book::getId).collect(Collectors.toSet());
            CategoryVO vo = new CategoryVO();
            BeanUtils.copyProperties(category, vo);
            vo.setColor(TextUtils.defaultIfBlank(category.getColor(), "#2e7364"));
            vo.setBookCount((long) categoryBooks.size());
            vo.setCopyCount(categoryBooks.stream()
                    .map(Book::getTotalCopies)
                    .filter(java.util.Objects::nonNull)
                    .mapToLong(Integer::longValue)
                    .sum());
            vo.setBorrowCount(records.stream().filter(record -> bookIds.contains(record.getBookId())).count());
            return vo;
        }).toList();
    }

    @Transactional
    public OperationResult<CategoryVO> create(CategoryRequest request) {
        String name = TextUtils.trimToEmpty(request.getName());
        String code = normalizeCode(request.getCode());
        validate(name, code);
        if (existsByName(name, null)) {
            throw BusinessException.conflict("该分类名称已存在");
        }
        if (existsByCode(code, null)) {
            throw BusinessException.conflict("该分类代码已存在");
        }

        BookCategory category = new BookCategory();
        category.setName(name);
        category.setCode(code);
        category.setColor(TextUtils.defaultIfBlank(request.getColor(), "#2e7364"));
        category.setDescription(TextUtils.trimToEmpty(request.getDescription()));
        category.setDeleted(0);
        bookCategoryMapper.insert(category);
        return OperationResult.of(toVO(category), "分类创建成功");
    }

    @Transactional
    public OperationResult<CategoryVO> update(Long id, CategoryRequest request) {
        BookCategory category = requireCategory(id);
        String name = TextUtils.trimToEmpty(request.getName());
        String code = normalizeCode(request.getCode());
        validate(name, code);
        if (existsByName(name, id)) {
            throw BusinessException.conflict("该分类名称已存在");
        }
        if (existsByCode(code, id)) {
            throw BusinessException.conflict("该分类代码已存在");
        }

        category.setName(name);
        category.setCode(code);
        category.setColor(TextUtils.defaultIfBlank(request.getColor(), category.getColor()));
        category.setDescription(TextUtils.trimToEmpty(request.getDescription()));
        bookCategoryMapper.updateById(category);
        return OperationResult.of(toVO(category), "分类信息已更新");
    }

    /**
     * 删除分类：分类下仍有图书时禁止删除。
     */
    @Transactional
    public OperationResult<Void> delete(Long id) {
        requireCategory(id);
        Long bookCount = bookMapper.selectCount(
                Wrappers.<Book>lambdaQuery().eq(Book::getCategoryId, id));
        if (bookCount != null && bookCount > 0) {
            throw BusinessException.badRequest("该分类下仍有图书，无法删除");
        }
        bookCategoryMapper.deleteById(id);
        return OperationResult.ok("分类已删除");
    }

    public BookCategory requireCategory(Long id) {
        BookCategory category = bookCategoryMapper.selectById(id);
        if (category == null) {
            throw BusinessException.notFound("分类不存在");
        }
        return category;
    }

    private void validate(String name, String code) {
        if (name.isEmpty() || code.isEmpty()) {
            throw BusinessException.badRequest("分类名称与代码不能为空");
        }
    }

    private String normalizeCode(String code) {
        return TextUtils.trimToEmpty(code).toUpperCase(Locale.ROOT);
    }

    private boolean existsByName(String name, Long excludeId) {
        Long count = bookCategoryMapper.selectCount(Wrappers.<BookCategory>lambdaQuery()
                .eq(BookCategory::getName, name)
                .ne(excludeId != null, BookCategory::getId, excludeId));
        return count != null && count > 0;
    }

    private boolean existsByCode(String code, Long excludeId) {
        Long count = bookCategoryMapper.selectCount(Wrappers.<BookCategory>lambdaQuery()
                .eq(BookCategory::getCode, code)
                .ne(excludeId != null, BookCategory::getId, excludeId));
        return count != null && count > 0;
    }

    private CategoryVO toVO(BookCategory category) {
        CategoryVO vo = new CategoryVO();
        BeanUtils.copyProperties(category, vo);
        vo.setColor(TextUtils.defaultIfBlank(category.getColor(), "#2e7364"));
        vo.setBookCount(0L);
        vo.setCopyCount(0L);
        vo.setBorrowCount(0L);
        return vo;
    }
}

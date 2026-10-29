package com.university.library.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.university.library.common.BusinessException;
import com.university.library.common.OperationResult;
import com.university.library.common.PageResult;
import com.university.library.common.constant.BorrowStatus;
import com.university.library.common.util.PageUtils;
import com.university.library.common.util.TextUtils;
import com.university.library.dto.BookRequest;
import com.university.library.entity.Book;
import com.university.library.entity.BookCategory;
import com.university.library.entity.BorrowRecord;
import com.university.library.mapper.BookCategoryMapper;
import com.university.library.mapper.BookMapper;
import com.university.library.mapper.BorrowRecordMapper;
import com.university.library.mapper.model.BookBorrowCount;
import com.university.library.service.support.ViewService;
import com.university.library.vo.BookVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Collator;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 图书服务：多条件查询、排序、CRUD 与库存一致性校验。
 */
@Service
@RequiredArgsConstructor
public class BookService {

    private static final String SORT_NEWEST = "newest";
    private static final String SORT_RATING = "rating";
    private static final String SORT_POPULAR = "popular";
    private static final String SORT_TITLE = "title";

    private final BookMapper bookMapper;
    private final BookCategoryMapper bookCategoryMapper;
    private final BorrowRecordMapper borrowRecordMapper;
    private final ViewService viewService;
    private final OverdueService overdueService;
    private final ReservationService reservationService;

    /**
     * 图书列表：书名/作者/ISBN/出版社/分类名关键字 + 分类 + 可借状态筛选，支持多种排序。
     */
    public PageResult<BookVO> list(String keyword, Long categoryId, String availability, String sort,
                                   long page, long size) {
        overdueService.refresh();

        List<BookCategory> categories = bookCategoryMapper.selectList(null);
        List<Long> categoryIdsByKeyword = TextUtils.isBlank(keyword) ? List.of()
                : categories.stream()
                .filter(category -> TextUtils.containsIgnoreCase(category.getName(), keyword))
                .map(BookCategory::getId)
                .toList();

        LambdaQueryWrapper<Book> wrapper = Wrappers.lambdaQuery();
        if (categoryId != null) {
            wrapper.eq(Book::getCategoryId, categoryId);
        }
        if ("AVAILABLE".equals(availability)) {
            wrapper.gt(Book::getAvailableCopies, 0);
        } else if ("UNAVAILABLE".equals(availability)) {
            wrapper.le(Book::getAvailableCopies, 0);
        }
        if (!TextUtils.isBlank(keyword)) {
            String term = keyword.trim();
            wrapper.and(w -> {
                w.like(Book::getTitle, term)
                        .or().like(Book::getAuthor, term)
                        .or().like(Book::getIsbn, term)
                        .or().like(Book::getPublisher, term);
                if (!categoryIdsByKeyword.isEmpty()) {
                    w.or().in(Book::getCategoryId, categoryIdsByKeyword);
                }
            });
        }

        List<Book> books = new ArrayList<>(bookMapper.selectList(wrapper));
        sortBooks(books, sort);

        long currentPage = PageUtils.normalizePage(page);
        long pageSize = PageUtils.normalizeSize(size);
        List<Book> sliced = PageUtils.slice(books, currentPage, pageSize);
        List<BookVO> records = viewService.toBookVOs(sliced);
        return PageResult.of(records, books.size(), currentPage, pageSize);
    }

    public BookVO get(Long id) {
        Book book = bookMapper.selectById(id);
        if (book == null) {
            throw BusinessException.notFound("图书不存在或已下架");
        }
        return viewService.toBookVO(book);
    }

    /**
     * 新增图书：可借数量默认等于馆藏总量。
     */
    @Transactional
    public OperationResult<BookVO> create(BookRequest request) {
        Book book = new Book();
        applyRequest(book, request, null);
        book.setAvailableCopies(book.getTotalCopies());
        book.setRating(new BigDecimal("4.5"));
        book.setReservationCount(0);
        book.setDeleted(0);
        bookMapper.insert(book);
        return OperationResult.of(viewService.toBookVO(book), "图书新增成功");
    }

    /**
     * 修改图书：馆藏总量不得低于当前在借数量，并同步修正可借数量。
     */
    @Transactional
    public OperationResult<BookVO> update(Long id, BookRequest request) {
        Book book = bookMapper.selectById(id);
        if (book == null) {
            throw BusinessException.notFound("图书不存在");
        }
        applyRequest(book, request, id);

        long activeCount = countActiveLoans(id);
        long readyReservations = reservationService.countReadyForBook(id);
        if (book.getTotalCopies() < activeCount + readyReservations) {
            throw BusinessException.badRequest(
                    "当前有 " + activeCount + " 本在借、" + readyReservations
                            + " 本已到书留书，馆藏总量不能低于两者之和");
        }
        book.setAvailableCopies((int) (book.getTotalCopies() - activeCount - readyReservations));
        bookMapper.updateById(book);
        return OperationResult.of(viewService.toBookVO(book), "图书信息已更新");
    }

    /**
     * 删除图书：仍有未归还副本时禁止删除；逻辑删除保留历史借阅。
     */
    @Transactional
    public OperationResult<Void> delete(Long id) {
        Book book = bookMapper.selectById(id);
        if (book == null) {
            throw BusinessException.notFound("图书不存在");
        }
        long activeCount = countActiveLoans(id);
        if (activeCount > 0) {
            throw BusinessException.badRequest("该书仍有 " + activeCount + " 本未归还，无法删除");
        }
        long activeReservations = reservationService.countActiveForBook(id);
        if (activeReservations > 0) {
            throw BusinessException.badRequest("该书仍有 " + activeReservations + " 个有效预约，请先处理");
        }
        bookMapper.deleteById(id);
        return OperationResult.ok("图书已删除");
    }

    public Book requireBook(Long id) {
        Book book = bookMapper.selectById(id);
        if (book == null) {
            throw BusinessException.notFound("图书不存在或已下架");
        }
        return book;
    }

    private long countActiveLoans(Long bookId) {
        Long count = borrowRecordMapper.selectCount(Wrappers.<BorrowRecord>lambdaQuery()
                .eq(BorrowRecord::getBookId, bookId)
                .in(BorrowRecord::getStatus, List.of(BorrowStatus.BORROWED, BorrowStatus.OVERDUE)));
        return count == null ? 0 : count;
    }

    /**
     * 把请求字段写入实体并做唯一性与合法性校验。
     */
    private void applyRequest(Book book, BookRequest request, Long currentId) {
        String isbn = TextUtils.trimToEmpty(request.getIsbn())
                .replaceAll("[-\\s]", "")
                .toUpperCase(Locale.ROOT);
        String title = TextUtils.trimToEmpty(request.getTitle());
        String author = TextUtils.trimToEmpty(request.getAuthor());

        if (title.isEmpty()) {
            throw BusinessException.badRequest("书名不能为空");
        }
        if (author.isEmpty()) {
            throw BusinessException.badRequest("作者不能为空");
        }
        if (isbn.isEmpty()) {
            throw BusinessException.badRequest("ISBN 不能为空");
        }
        if (!isbn.matches("(?i)^(97[89])?\\d{9}[\\dX]$")) {
            throw BusinessException.badRequest("ISBN 格式不正确，应为 10 位或 13 位数字");
        }
        if (request.getTotalCopies() == null || request.getTotalCopies() < 1) {
            throw BusinessException.badRequest("馆藏总量至少为 1 本");
        }
        if (request.getCategoryId() == null || bookCategoryMapper.selectById(request.getCategoryId()) == null) {
            throw BusinessException.badRequest("请选择有效的图书分类");
        }
        LambdaQueryWrapper<Book> duplicateWrapper = Wrappers.<Book>lambdaQuery()
                .eq(Book::getIsbn, isbn)
                .ne(currentId != null, Book::getId, currentId);
        Long duplicate = bookMapper.selectCount(duplicateWrapper);
        if (duplicate != null && duplicate > 0) {
            throw BusinessException.conflict("ISBN 已存在，请检查");
        }

        book.setIsbn(isbn);
        book.setTitle(title);
        book.setAuthor(author);
        book.setPublisher(TextUtils.trimToEmpty(request.getPublisher()));
        book.setCategoryId(request.getCategoryId());
        book.setTotalCopies(request.getTotalCopies());
        book.setPrice(request.getPrice() == null ? BigDecimal.ZERO : request.getPrice());
        book.setPublishYear(request.getPublishYear() == null
                ? LocalDate.now().getYear() : request.getPublishYear());
        book.setLocation(TextUtils.trimToEmpty(request.getLocation()));
        book.setDescription(TextUtils.trimToEmpty(request.getDescription()));
    }

    /**
     * 排序：与前端 mock 的 recommended/newest/rating/popular/title 语义一致。
     */
    private void sortBooks(List<Book> books, String sort) {
        String order = TextUtils.isBlank(sort) ? "recommended" : sort;
        Map<Long, Long> borrowCountMap = borrowRecordMapper.countByBook().stream()
                .collect(Collectors.toMap(BookBorrowCount::getBookId,
                        count -> count.getTotalCount() == null ? 0L : count.getTotalCount(), (a, b) -> a));

        Comparator<Book> comparator = switch (order) {
            case SORT_NEWEST -> Comparator
                    .comparing((Book book) -> book.getPublishYear() == null ? 0 : book.getPublishYear())
                    .reversed()
                    .thenComparing(Book::getId, Comparator.reverseOrder());
            case SORT_RATING -> Comparator
                    .comparing((Book book) -> book.getRating() == null ? BigDecimal.ZERO : book.getRating())
                    .reversed()
                    .thenComparing(Book::getId);
            case SORT_POPULAR -> Comparator
                    .comparing((Book book) -> borrowCountMap.getOrDefault(book.getId(), 0L))
                    .reversed()
                    .thenComparing(Book::getId);
            case SORT_TITLE -> {
                Collator collator = Collator.getInstance(Locale.CHINA);
                yield (a, b) -> collator.compare(TextUtils.trimToEmpty(a.getTitle()),
                        TextUtils.trimToEmpty(b.getTitle()));
            }
            default -> Comparator.comparing(Book::getId);
        };
        books.sort(comparator);
    }

    /**
     * 用于视图转换的辅助方法（保留给未来批量场景）。
     */
    public List<BookVO> toBookVOs(List<Book> books) {
        return viewService.toBookVOs(books);
    }
}

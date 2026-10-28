package com.university.library.service.support;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.university.library.common.constant.BorrowStatus;
import com.university.library.common.constant.PenaltyStatus;
import com.university.library.common.constant.ReaderType;
import com.university.library.common.util.TextUtils;
import com.university.library.entity.Book;
import com.university.library.entity.BookCategory;
import com.university.library.entity.BorrowRecord;
import com.university.library.entity.Penalty;
import com.university.library.entity.Reader;
import com.university.library.entity.User;
import com.university.library.mapper.BookCategoryMapper;
import com.university.library.mapper.BookMapper;
import com.university.library.mapper.BorrowRecordMapper;
import com.university.library.mapper.PenaltyMapper;
import com.university.library.mapper.ReaderMapper;
import com.university.library.mapper.model.BookBorrowCount;
import com.university.library.mapper.model.ReaderBorrowCount;
import com.university.library.mapper.model.ReaderUnpaidSum;
import com.university.library.vo.BookVO;
import com.university.library.vo.BorrowRecordVO;
import com.university.library.vo.PenaltyVO;
import com.university.library.vo.ReaderVO;
import com.university.library.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 视图装配服务：把 entity 转换为 VO，并补齐前端约定的派生字段。
 *
 * <p>集中处理“实体 → 出参”的转换，避免各 Service 重复拼装；
 * 同时保证 Controller 永远不会直接返回 entity。</p>
 */
@Component
@RequiredArgsConstructor
public class ViewService {

    private final ReaderMapper readerMapper;
    private final BookMapper bookMapper;
    private final BookCategoryMapper bookCategoryMapper;
    private final BorrowRecordMapper borrowRecordMapper;
    private final PenaltyMapper penaltyMapper;

    // ---------------------------------------------------------------------
    // 读者
    // ---------------------------------------------------------------------

    public ReaderVO toReaderVO(Reader reader) {
        if (reader == null) {
            return null;
        }
        List<BorrowRecord> records = borrowRecordMapper.selectList(
                Wrappers.<BorrowRecord>lambdaQuery().eq(BorrowRecord::getReaderId, reader.getId()));
        long active = records.stream().filter(r -> BorrowStatus.isActive(r.getStatus())).count();
        long overdue = records.stream().filter(r -> BorrowStatus.OVERDUE.equals(r.getStatus())).count();
        BigDecimal unpaid = penaltyMapper.selectList(Wrappers.<Penalty>lambdaQuery()
                        .eq(Penalty::getReaderId, reader.getId())
                        .eq(Penalty::getStatus, PenaltyStatus.UNPAID))
                .stream()
                .map(Penalty::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return buildReaderVO(reader, active, overdue, (long) records.size(), unpaid);
    }

    public List<ReaderVO> toReaderVOs(List<Reader> readers) {
        if (readers == null || readers.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, ReaderBorrowCount> countMap = borrowRecordMapper.countByReader().stream()
                .collect(Collectors.toMap(ReaderBorrowCount::getReaderId, Function.identity(), (a, b) -> a));
        Map<Long, BigDecimal> unpaidMap = penaltyMapper.sumUnpaidByReader().stream()
                .collect(Collectors.toMap(ReaderUnpaidSum::getReaderId, ReaderUnpaidSum::getUnpaidAmount, (a, b) -> a));
        return readers.stream().map(reader -> {
            ReaderBorrowCount count = countMap.get(reader.getId());
            long active = count == null ? 0 : nullSafe(count.getActiveCount());
            long overdue = count == null ? 0 : nullSafe(count.getOverdueCount());
            long total = count == null ? 0 : nullSafe(count.getTotalCount());
            return buildReaderVO(reader, active, overdue, total, unpaidMap.get(reader.getId()));
        }).toList();
    }

    private ReaderVO buildReaderVO(Reader reader, long active, long overdue, long total, BigDecimal unpaid) {
        ReaderType readerType = ReaderType.from(reader.getType());
        ReaderVO vo = new ReaderVO();
        BeanUtils.copyProperties(reader, vo);
        vo.setTypeLabel(readerType.getLabel());
        vo.setMaxBorrow(readerType.getMaxBorrow());
        vo.setBorrowDays(readerType.getBorrowDays());
        vo.setActiveBorrowCount(active);
        vo.setOverdueCount(overdue);
        vo.setAvailableQuota(Math.max(readerType.getMaxBorrow() - active, 0));
        vo.setUnpaidAmount(money(unpaid));
        vo.setTotalBorrowCount(total);
        return vo;
    }

    // ---------------------------------------------------------------------
    // 图书
    // ---------------------------------------------------------------------

    public BookVO toBookVO(Book book) {
        if (book == null) {
            return null;
        }
        long active = borrowRecordMapper.selectCount(Wrappers.<BorrowRecord>lambdaQuery()
                .eq(BorrowRecord::getBookId, book.getId())
                .in(BorrowRecord::getStatus, List.of(BorrowStatus.BORROWED, BorrowStatus.OVERDUE)));
        return buildBookVO(book, bookCategoryMapper.selectById(book.getCategoryId()), active);
    }

    public List<BookVO> toBookVOs(List<Book> books) {
        if (books == null || books.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, Long> activeMap = borrowRecordMapper.countByBook().stream()
                .collect(Collectors.toMap(BookBorrowCount::getBookId,
                        c -> nullSafe(c.getActiveCount()), (a, b) -> a));
        Map<Long, BookCategory> categoryMap = bookCategoryMapper.selectList(null).stream()
                .collect(Collectors.toMap(BookCategory::getId, Function.identity(), (a, b) -> a));
        return books.stream()
                .map(book -> buildBookVO(book, categoryMap.get(book.getCategoryId()),
                        activeMap.getOrDefault(book.getId(), 0L)))
                .toList();
    }

    private BookVO buildBookVO(Book book, BookCategory category, long activeLoans) {
        BookVO vo = new BookVO();
        BeanUtils.copyProperties(book, vo);
        vo.setCategoryName(category == null ? "未分类" : category.getName());
        vo.setCategoryCode(category == null ? "" : TextUtils.trimToEmpty(category.getCode()));
        vo.setCategoryColor(category == null ? "#6f7f79" : category.getColor());
        vo.setActiveLoans(activeLoans);
        vo.setAvailable(book.getAvailableCopies() != null && book.getAvailableCopies() > 0);
        return vo;
    }

    // ---------------------------------------------------------------------
    // 借阅记录
    // ---------------------------------------------------------------------

    public BorrowRecordVO toRecordVO(BorrowRecord record) {
        if (record == null) {
            return null;
        }
        return buildRecordVO(record, readerMap(), bookMap(), categoryMap());
    }

    public List<BorrowRecordVO> toRecordVOs(List<BorrowRecord> records) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, Reader> readers = readerMap();
        Map<Long, Book> books = bookMap();
        Map<Long, BookCategory> categories = categoryMap();
        return records.stream().map(record -> buildRecordVO(record, readers, books, categories)).toList();
    }

    private BorrowRecordVO buildRecordVO(BorrowRecord record, Map<Long, Reader> readers,
                                         Map<Long, Book> books, Map<Long, BookCategory> categories) {
        BorrowRecordVO vo = new BorrowRecordVO();
        BeanUtils.copyProperties(record, vo);

        Reader reader = readers.get(record.getReaderId());
        Book book = books.get(record.getBookId());
        BookCategory category = book == null ? null : categories.get(book.getCategoryId());

        vo.setReaderName(reader != null ? reader.getName()
                : TextUtils.defaultIfBlank(record.getReaderName(), "已注销读者"));
        vo.setReaderNo(reader != null ? reader.getStudentNo()
                : TextUtils.defaultIfBlank(record.getReaderNo(), "—"));
        vo.setReaderType(reader == null ? null : reader.getType());
        vo.setBookTitle(book != null ? book.getTitle()
                : TextUtils.defaultIfBlank(record.getBookTitle(), "已下架图书"));
        vo.setBookIsbn(book != null ? book.getIsbn()
                : TextUtils.defaultIfBlank(record.getBookIsbn(), "—"));
        vo.setBookAuthor(book == null ? "—" : book.getAuthor());
        vo.setCategoryName(category == null ? "未分类" : category.getName());
        vo.setCategoryColor(category == null ? "#6f7f79" : category.getColor());

        long overdueDays = 0;
        if (BorrowStatus.isActive(record.getStatus())
                && record.getDueDate() != null
                && record.getDueDate().isBefore(LocalDate.now())) {
            overdueDays = ChronoUnit.DAYS.between(record.getDueDate(), LocalDate.now());
        }
        vo.setOverdueDays(overdueDays);
        int renewCount = record.getRenewCount() == null ? 0 : record.getRenewCount();
        vo.setCanRenew(BorrowStatus.isActive(record.getStatus())
                && renewCount < 1
                && !BorrowStatus.OVERDUE.equals(record.getStatus()));
        return vo;
    }

    /**
     * 记录 → VO 时的关联数据（供仪表盘/统计复用）。
     */
    public Map<Long, Reader> readerMap() {
        return readerMapper.selectList(null).stream()
                .collect(Collectors.toMap(Reader::getId, Function.identity(), (a, b) -> a));
    }

    public Map<Long, Book> bookMap() {
        return bookMapper.selectList(null).stream()
                .collect(Collectors.toMap(Book::getId, Function.identity(), (a, b) -> a));
    }

    public Map<Long, BookCategory> categoryMap() {
        return bookCategoryMapper.selectList(null).stream()
                .collect(Collectors.toMap(BookCategory::getId, Function.identity(), (a, b) -> a));
    }

    // ---------------------------------------------------------------------
    // 罚款
    // ---------------------------------------------------------------------

    public PenaltyVO toPenaltyVO(Penalty penalty) {
        if (penalty == null) {
            return null;
        }
        return toPenaltyVOs(List.of(penalty)).get(0);
    }

    public List<PenaltyVO> toPenaltyVOs(List<Penalty> penalties) {
        if (penalties == null || penalties.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> recordIds = penalties.stream().map(Penalty::getRecordId)
                .filter(Objects::nonNull).distinct().toList();
        Map<Long, BorrowRecord> recordMap = recordIds.isEmpty() ? Collections.emptyMap()
                : borrowRecordMapper.selectByIds(recordIds).stream()
                .collect(Collectors.toMap(BorrowRecord::getId, Function.identity(), (a, b) -> a));
        Map<Long, Reader> readers = readerMap();
        Map<Long, Book> books = bookMap();
        Map<Long, BookCategory> categories = categoryMap();

        return penalties.stream().map(penalty -> {
            PenaltyVO vo = new PenaltyVO();
            BeanUtils.copyProperties(penalty, vo);
            BorrowRecord record = recordMap.get(penalty.getRecordId());
            Reader reader = readers.get(penalty.getReaderId());
            Book book = books.get(penalty.getBookId());
            BookCategory category = book == null ? null : categories.get(book.getCategoryId());

            vo.setReaderName(reader != null ? reader.getName()
                    : record != null ? TextUtils.defaultIfBlank(record.getReaderName(), "已注销读者") : "—");
            vo.setReaderNo(reader != null ? reader.getStudentNo()
                    : record != null ? TextUtils.defaultIfBlank(record.getReaderNo(), "—") : "—");
            vo.setBookTitle(book != null ? book.getTitle()
                    : record != null ? TextUtils.defaultIfBlank(record.getBookTitle(), "已下架图书") : "—");
            vo.setBookIsbn(book != null ? book.getIsbn()
                    : record != null ? TextUtils.defaultIfBlank(record.getBookIsbn(), "—") : "—");
            if (record != null) {
                vo.setBorrowDate(record.getBorrowDate());
                vo.setDueDate(record.getDueDate());
                vo.setReturnDate(record.getReturnDate());
                vo.setRecordStatus(record.getStatus());
            }
            vo.setAmount(money(penalty.getAmount()));
            vo.setRuleRate(penalty.getRuleRate() == null ? null : money(penalty.getRuleRate()));
            return vo;
        }).toList();
    }

    // ---------------------------------------------------------------------
    // 用户
    // ---------------------------------------------------------------------

    public UserVO toUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        Reader reader = readerMapper.selectOne(
                Wrappers.<Reader>lambdaQuery().eq(Reader::getUserId, user.getId()));
        if (reader != null) {
            vo.setReaderId(reader.getId());
            vo.setReader(toReaderVO(reader));
        }
        return vo;
    }

    // ---------------------------------------------------------------------

    public static BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private static long nullSafe(Long value) {
        return value == null ? 0L : value;
    }
}

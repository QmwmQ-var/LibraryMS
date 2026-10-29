package com.university.library.service;

import com.university.library.common.BusinessException;
import com.university.library.common.config.PenaltyProperties;
import com.university.library.common.constant.BorrowStatus;
import com.university.library.common.constant.PenaltyStatus;
import com.university.library.common.constant.ReaderType;
import com.university.library.common.security.UserPrincipal;
import com.university.library.common.util.TextUtils;
import com.university.library.entity.Book;
import com.university.library.entity.BookCategory;
import com.university.library.entity.BorrowRecord;
import com.university.library.entity.Penalty;
import com.university.library.entity.Reader;
import com.university.library.mapper.BookCategoryMapper;
import com.university.library.mapper.BookMapper;
import com.university.library.mapper.BorrowRecordMapper;
import com.university.library.mapper.PenaltyMapper;
import com.university.library.mapper.ReaderMapper;
import com.university.library.service.support.ViewService;
import com.university.library.vo.ActivityVO;
import com.university.library.vo.AdminOverviewVO;
import com.university.library.vo.BookRankVO;
import com.university.library.vo.BorrowRecordVO;
import com.university.library.vo.CategoryStatVO;
import com.university.library.vo.MonthlyOverdueTrendVO;
import com.university.library.vo.MonthlyTrendVO;
import com.university.library.vo.NotificationItemVO;
import com.university.library.vo.NotificationVO;
import com.university.library.vo.OverdueBucketVO;
import com.university.library.vo.OverdueStatsVO;
import com.university.library.vo.OverdueSummaryVO;
import com.university.library.vo.RankingVO;
import com.university.library.vo.ReaderOverviewVO;
import com.university.library.vo.ReaderRankVO;
import com.university.library.vo.TrendsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 统计服务：概览、8 个月趋势、分类构成、图书/读者排行、逾期统计与通知。
 *
 * <p>校园图书数据量相对有限，统计采用“批量加载 + 内存计算”，保证与前端 mock
 * 的统计口径完全一致；生产环境可将聚合下沉为 SQL。</p>
 */
@Service
@RequiredArgsConstructor
public class StatsService {

    private final BookMapper bookMapper;
    private final ReaderMapper readerMapper;
    private final BookCategoryMapper bookCategoryMapper;
    private final BorrowRecordMapper borrowRecordMapper;
    private final PenaltyMapper penaltyMapper;
    private final ViewService viewService;
    private final OverdueService overdueService;
    private final PenaltyProperties penaltyProperties;
    private final ReservationService reservationService;

    // ---------------------------------------------------------------------
    // 概览
    // ---------------------------------------------------------------------

    public AdminOverviewVO adminOverview(LocalDate startDate, LocalDate endDate) {
        overdueService.refresh();
        List<Book> books = books();
        List<Reader> readers = readers();
        List<BorrowRecord> records = records();
        List<Penalty> penalties = penalties();

        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.now();
        List<BorrowRecord> currentMonthRecords = records.stream()
                .filter(record -> record.getBorrowDate() != null
                        && YearMonth.from(record.getBorrowDate()).equals(currentMonth))
                .toList();
        List<BorrowRecord> lastMonthRecords = records.stream()
                .filter(record -> record.getBorrowDate() != null
                        && YearMonth.from(record.getBorrowDate()).equals(currentMonth.minusMonths(1)))
                .toList();
        double monthDelta = lastMonthRecords.isEmpty()
                ? 100.0
                : round1((currentMonthRecords.size() - lastMonthRecords.size()) * 100.0
                / lastMonthRecords.size());

        List<BorrowRecord> activeRecords = records.stream()
                .filter(record -> BorrowStatus.isActive(record.getStatus()))
                .toList();
        List<Penalty> unpaid = penalties.stream()
                .filter(penalty -> PenaltyStatus.UNPAID.equals(penalty.getStatus()))
                .toList();
        long rangeBorrowCount = records.stream()
                .filter(record -> startDate == null && endDate == null
                        || inPeriod(record.getBorrowDate(), startDate, endDate))
                .count();

        AdminOverviewVO overview = new AdminOverviewVO();
        overview.setTotalTitles((long) books.size());
        overview.setTotalCopies(books.stream().map(Book::getTotalCopies)
                .filter(Objects::nonNull).mapToLong(Integer::longValue).sum());
        overview.setAvailableCopies(books.stream().map(Book::getAvailableCopies)
                .filter(Objects::nonNull).mapToLong(Integer::longValue).sum());
        overview.setReaderCount((long) readers.size());
        overview.setActiveLoans((long) activeRecords.size());
        overview.setOverdueLoans(records.stream()
                .filter(record -> BorrowStatus.OVERDUE.equals(record.getStatus())).count());
        overview.setTodayBorrows(records.stream()
                .filter(record -> today.equals(record.getBorrowDate())).count());
        overview.setMonthBorrows((long) currentMonthRecords.size());
        overview.setMonthBorrowDelta(monthDelta);
        overview.setActiveMonthReaders(currentMonthRecords.stream()
                .map(BorrowRecord::getReaderId).filter(Objects::nonNull).distinct().count());
        overview.setPendingFineCount((long) unpaid.size());
        overview.setPendingFineAmount(ViewService.money(sumPenalty(unpaid)));
        overview.setLowStockCount(books.stream()
                .filter(book -> book.getAvailableCopies() != null && book.getAvailableCopies() <= 1).count());
        overview.setRangeBorrowCount(rangeBorrowCount);
        var reservationSummary = reservationService.adminSummary();
        overview.setPendingReservations(reservationSummary.getPendingCount());
        overview.setReadyReservations(reservationSummary.getReadyCount());
        return overview;
    }

    public ReaderOverviewVO readerOverview(UserPrincipal principal) {
        if (principal.readerId() == null) {
            throw BusinessException.badRequest("当前账号不是读者账号");
        }
        overdueService.refresh();
        Reader reader = readerMapper.selectById(principal.readerId());
        ReaderType readerType = ReaderType.from(reader == null ? null : reader.getType());
        List<BorrowRecord> history = records().stream()
                .filter(record -> Objects.equals(record.getReaderId(), principal.readerId()))
                .toList();
        List<BorrowRecord> active = history.stream()
                .filter(record -> BorrowStatus.isActive(record.getStatus())).toList();
        long overdue = active.stream()
                .filter(record -> BorrowStatus.OVERDUE.equals(record.getStatus())).count();
        LocalDate today = LocalDate.now();
        long dueSoon = active.stream()
                .filter(record -> !BorrowStatus.OVERDUE.equals(record.getStatus()))
                .filter(record -> record.getDueDate() != null)
                .filter(record -> !record.getDueDate().isBefore(today)
                        && !record.getDueDate().isAfter(today.plusDays(3)))
                .count();
        List<Penalty> unpaid = penalties().stream()
                .filter(penalty -> Objects.equals(penalty.getReaderId(), principal.readerId()))
                .filter(penalty -> PenaltyStatus.UNPAID.equals(penalty.getStatus()))
                .toList();

        Map<String, Long> categoryCounts = new LinkedHashMap<>();
        Map<Long, Book> bookMap = books().stream()
                .collect(Collectors.toMap(Book::getId, book -> book, (a, b) -> a));
        Map<Long, BookCategory> categoryMap = categories().stream()
                .collect(Collectors.toMap(BookCategory::getId, category -> category, (a, b) -> a));
        for (BorrowRecord record : history) {
            Book book = bookMap.get(record.getBookId());
            BookCategory category = book == null ? null : categoryMap.get(book.getCategoryId());
            if (category != null) {
                categoryCounts.merge(category.getName(), 1L, Long::sum);
            }
        }
        Map.Entry<String, Long> favorite = categoryCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElse(null);

        ReaderOverviewVO overview = new ReaderOverviewVO();
        overview.setActiveLoans((long) active.size());
        overview.setOverdueLoans(overdue);
        overview.setDueSoon(dueSoon);
        overview.setAvailableQuota((long) Math.max(readerType.getMaxBorrow() - active.size(), 0));
        overview.setMaxBorrow(readerType.getMaxBorrow());
        overview.setUnpaidCount((long) unpaid.size());
        overview.setUnpaidAmount(ViewService.money(sumPenalty(unpaid)));
        overview.setTotalRead((long) history.size());
        overview.setFavoriteCategory(favorite == null ? "暂无" : favorite.getKey());
        overview.setFavoriteCategoryCount(favorite == null ? 0L : favorite.getValue());
        overview.setCardStatus(reader == null ? "ACTIVE" : reader.getCardStatus());
        var reservationSummary = reservationService.readerSummary(principal.readerId());
        overview.setPendingReservations(reservationSummary.getPendingCount());
        overview.setReadyReservations(reservationSummary.getReadyCount());
        return overview;
    }

    // ---------------------------------------------------------------------
    // 趋势与构成
    // ---------------------------------------------------------------------

    /**
     * 最近 N 个月的借还趋势。
     */
    public MonthlyTrendVO monthlyTrend(int months) {
        int safeMonths = Math.max(1, months);
        List<BorrowRecord> records = records();
        YearMonth base = YearMonth.now();
        MonthlyTrendVO trend = new MonthlyTrendVO();
        for (int offset = safeMonths - 1; offset >= 0; offset -= 1) {
            YearMonth cursor = base.minusMonths(offset);
            trend.getLabels().add(cursor.getMonthValue() + "月");
            trend.getBorrows().add(records.stream()
                    .filter(record -> record.getBorrowDate() != null
                            && YearMonth.from(record.getBorrowDate()).equals(cursor))
                    .count());
            trend.getReturns().add(records.stream()
                    .filter(record -> record.getReturnDate() != null
                            && YearMonth.from(record.getReturnDate()).equals(cursor))
                    .count());
        }
        return trend;
    }

    /**
     * 分类借阅构成：每个分类的借阅次数（不含已删除图书的记录）。
     */
    public List<CategoryStatVO> categoryDistribution() {
        List<BookCategory> categories = categories();
        Map<Long, Book> bookMap = books().stream()
                .collect(Collectors.toMap(Book::getId, book -> book, (a, b) -> a));
        List<BorrowRecord> records = records();
        return categories.stream().map(category -> {
            long value = records.stream()
                    .filter(record -> {
                        Book book = bookMap.get(record.getBookId());
                        return book != null && Objects.equals(book.getCategoryId(), category.getId());
                    })
                    .count();
            return new CategoryStatVO(category.getId(), category.getName(), value,
                    TextUtils.defaultIfBlank(category.getColor(), "#6f7f79"));
        }).toList();
    }

    public TrendsVO trends() {
        return new TrendsVO(monthlyTrend(8), categoryDistribution());
    }

    // ---------------------------------------------------------------------
    // 排行
    // ---------------------------------------------------------------------

    /**
     * 图书借阅排行：统计期（可选）内借阅次数降序，次数相同按评分降序。
     */
    public List<BookRankVO> bookRanking(int limit, LocalDate startDate, LocalDate endDate) {
        int safeLimit = normalizeLimit(limit);
        List<BorrowRecord> records = records();
        Map<Long, BookCategory> categoryMap = categories().stream()
                .collect(Collectors.toMap(BookCategory::getId, category -> category, (a, b) -> a));

        List<BookRankVO> ranking = books().stream().map(book -> {
            BookRankVO vo = new BookRankVO();
            BeanUtils.copyProperties(book, vo);
            BookCategory category = categoryMap.get(book.getCategoryId());
            vo.setCategoryName(category == null ? "未分类" : category.getName());
            vo.setCategoryColor(category == null ? "#6f7f79" : category.getColor());
            vo.setBorrowCount(records.stream()
                    .filter(record -> Objects.equals(record.getBookId(), book.getId()))
                    .filter(record -> startDate == null && endDate == null
                            || inPeriod(record.getBorrowDate(), startDate, endDate))
                    .count());
            return vo;
        }).sorted(Comparator
                .comparing(BookRankVO::getBorrowCount, Comparator.reverseOrder())
                .thenComparing(BookRankVO::getRating,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        return ranking.stream().limit(safeLimit).toList();
    }

    /**
     * 读者借阅排行：累计借阅次数降序。
     */
    public List<ReaderRankVO> readerRanking(int limit) {
        int safeLimit = normalizeLimit(limit);
        List<BorrowRecord> records = records();
        List<ReaderRankVO> ranking = readers().stream().map(reader -> {
            ReaderRankVO vo = new ReaderRankVO();
            BeanUtils.copyProperties(reader, vo);
            ReaderType readerType = ReaderType.from(reader.getType());
            vo.setTypeLabel(readerType.getLabel());
            vo.setBorrowCount(records.stream()
                    .filter(record -> Objects.equals(record.getReaderId(), reader.getId())).count());
            vo.setActiveCount(records.stream()
                    .filter(record -> Objects.equals(record.getReaderId(), reader.getId()))
                    .filter(record -> BorrowStatus.isActive(record.getStatus())).count());
            return vo;
        }).sorted(Comparator.comparing(ReaderRankVO::getBorrowCount, Comparator.reverseOrder()))
                .toList();
        return ranking.stream().limit(safeLimit).toList();
    }

    public RankingVO ranking(Integer limit, LocalDate startDate, LocalDate endDate) {
        int safeLimit = limit == null || limit <= 0 ? 8 : Math.min(limit, 20);
        return new RankingVO(bookRanking(safeLimit, startDate, endDate), readerRanking(safeLimit));
    }

    // ---------------------------------------------------------------------
    // 逾期统计
    // ---------------------------------------------------------------------

    public OverdueStatsVO overdueStats(int months) {
        overdueService.refresh();
        List<BorrowRecord> allRecords = records();
        List<BorrowRecordVO> recordVOs = viewService.toRecordVOs(allRecords);
        List<BorrowRecordVO> overdueRecords = recordVOs.stream()
                .filter(record -> BorrowStatus.OVERDUE.equals(record.getStatus()))
                .sorted(Comparator.comparing(BorrowRecordVO::getOverdueDays,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        int safeMonths = Math.max(1, months);
        List<Penalty> penalties = penalties();
        YearMonth base = YearMonth.now();
        MonthlyOverdueTrendVO trend = new MonthlyOverdueTrendVO();
        for (int offset = safeMonths - 1; offset >= 0; offset -= 1) {
            YearMonth cursor = base.minusMonths(offset);
            trend.getLabels().add(cursor.getMonthValue() + "月");
            List<Penalty> monthly = penalties.stream()
                    .filter(penalty -> penalty.getCreatedAt() != null
                            && YearMonth.from(penalty.getCreatedAt()).equals(cursor))
                    .toList();
            trend.getCounts().add((long) monthly.size());
            trend.getAmounts().add(ViewService.money(sumPenalty(monthly)));
        }

        List<OverdueBucketVO> buckets = new ArrayList<>();
        buckets.add(new OverdueBucketVO("1–7 天", 1, 7, "#d5a25c", countBucket(overdueRecords, 1, 7)));
        buckets.add(new OverdueBucketVO("8–30 天", 8, 30, "#bd7a4e", countBucket(overdueRecords, 8, 30)));
        buckets.add(new OverdueBucketVO("30 天以上", 31, null, "#b45b52",
                countBucket(overdueRecords, 31, Integer.MAX_VALUE)));

        List<Penalty> unpaid = penalties.stream()
                .filter(penalty -> PenaltyStatus.UNPAID.equals(penalty.getStatus()))
                .toList();
        long activeCount = allRecords.stream()
                .filter(record -> BorrowStatus.isActive(record.getStatus())).count();

        OverdueSummaryVO summary = new OverdueSummaryVO();
        summary.setCount((long) overdueRecords.size());
        summary.setMaxDays(overdueRecords.isEmpty() ? 0L
                : overdueRecords.get(0).getOverdueDays());
        summary.setPendingAmount(ViewService.money(sumPenalty(unpaid)));
        summary.setTotalRecords((long) allRecords.size());
        summary.setOverdueRate(activeCount == 0 ? 0.0
                : round1(overdueRecords.size() * 100.0 / activeCount));

        List<Penalty> recentUnpaid = unpaid.stream()
                .skip(Math.max(unpaid.size() - 5, 0))
                .toList();

        OverdueStatsVO result = new OverdueStatsVO();
        result.setSummary(summary);
        result.setTrend(trend);
        result.setBuckets(buckets);
        result.setRecords(overdueRecords.stream().limit(8).toList());
        result.setRecentPenalties(viewService.toPenaltyVOs(recentUnpaid));
        return result;
    }

    // ---------------------------------------------------------------------
    // 动态与通知
    // ---------------------------------------------------------------------

    /**
     * 最近借还动态。
     */
    public List<ActivityVO> recentActivities(UserPrincipal principal, int limit) {
        overdueService.refresh();
        List<BorrowRecord> records = records();
        if (principal != null && principal.isReader()) {
            records = records.stream()
                    .filter(record -> Objects.equals(record.getReaderId(), principal.readerId()))
                    .toList();
        }
        List<BorrowRecord> sorted = records.stream()
                .sorted(Comparator.comparing(BorrowRecord::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(Math.max(limit, 0))
                .toList();
        List<BorrowRecordVO> recordVOs = viewService.toRecordVOs(sorted);
        return recordVOs.stream().map(record -> {
            ActivityVO activity = new ActivityVO();
            activity.setId("record-" + record.getId());
            boolean returned = record.getReturnDate() != null;
            activity.setType(returned ? "RETURN" : "BORROW");
            activity.setTitle(returned
                    ? record.getReaderName() + " 归还了《" + record.getBookTitle() + "》"
                    : record.getReaderName() + " 借阅了《" + record.getBookTitle() + "》");
            activity.setDescription(record.getReaderNo() + " · " + record.getCategoryName());
            activity.setTime(returned
                    ? record.getReturnDate() + "T16:20:00"
                    : record.getCreatedAt() == null ? null : record.getCreatedAt().toString());
            activity.setTone(returned ? "green" : "copper");
            return activity;
        }).toList();
    }

    /**
     * 仪表盘通知：按角色返回待处理事项。
     */
    public NotificationVO notifications(UserPrincipal principal) {
        overdueService.refresh();
        List<NotificationItemVO> items = new ArrayList<>();
        String rateLabel = penaltyProperties.getRatePerDay()
                .setScale(2, RoundingMode.HALF_UP).toPlainString();

        if (principal.isAdmin()) {
            List<BorrowRecord> records = records();
            List<Penalty> unpaid = penalties().stream()
                    .filter(penalty -> PenaltyStatus.UNPAID.equals(penalty.getStatus())).toList();
            long overdue = records.stream()
                    .filter(record -> BorrowStatus.OVERDUE.equals(record.getStatus())).count();
            long lowStock = books().stream()
                    .filter(book -> book.getAvailableCopies() != null && book.getAvailableCopies() <= 1).count();
            var reservationSummary = reservationService.adminSummary();
            if (reservationSummary.getPendingCount() != null && reservationSummary.getPendingCount() > 0) {
                items.add(new NotificationItemVO("reservations", "info",
                        reservationSummary.getPendingCount() + " 位读者正在等待预约",
                        "可查看到书、排队与领取状态，及时处理留书。", "/reservations-manage"));
            }
            if (overdue > 0) {
                items.add(new NotificationItemVO("overdue", "danger", overdue + " 笔借阅已逾期",
                        "建议尽快联系读者办理归还，逾期将按日累计罚款。", "/circulation?status=OVERDUE"));
            }
            if (!unpaid.isEmpty()) {
                items.add(new NotificationItemVO("fines", "warning", unpaid.size() + " 笔罚款待确认",
                        "待收金额 ¥" + ViewService.money(sumPenalty(unpaid)).toPlainString()
                                + "，可在罚款管理中处理。", "/penalties-manage"));
            }
            if (lowStock > 0) {
                items.add(new NotificationItemVO("stock", "info", lowStock + " 种图书可借量不足",
                        "可在图书管理中调整馆藏数量或安排补采。", "/books-manage?availability=UNAVAILABLE"));
            }
            if (items.isEmpty()) {
                items.add(new NotificationItemVO("calm", "success", "今日流通一切正常",
                        "暂无逾期、待收罚款或低库存提醒。", "/dashboard"));
            }
        } else {
            Long readerId = principal.readerId();
            List<BorrowRecord> active = records().stream()
                    .filter(record -> Objects.equals(record.getReaderId(), readerId))
                    .filter(record -> BorrowStatus.isActive(record.getStatus()))
                    .toList();
            long overdue = active.stream()
                    .filter(record -> BorrowStatus.OVERDUE.equals(record.getStatus())).count();
            LocalDate today = LocalDate.now();
            long dueSoon = active.stream()
                    .filter(record -> !BorrowStatus.OVERDUE.equals(record.getStatus()))
                    .filter(record -> record.getDueDate() != null)
                    .filter(record -> !record.getDueDate().isBefore(today)
                            && !record.getDueDate().isAfter(today.plusDays(3)))
                    .count();
            List<Penalty> unpaid = penalties().stream()
                    .filter(penalty -> Objects.equals(penalty.getReaderId(), readerId))
                    .filter(penalty -> PenaltyStatus.UNPAID.equals(penalty.getStatus()))
                    .toList();
            if (overdue > 0) {
                items.add(new NotificationItemVO("my-overdue", "danger", overdue + " 本图书已逾期",
                        "请尽快归还，逾期罚款将按每日 ¥" + rateLabel + " 计算。", "/my-borrows"));
            }
            if (dueSoon > 0) {
                items.add(new NotificationItemVO("my-due", "warning", dueSoon + " 本图书即将到期",
                        "可在「我的借阅」中办理续借或归还。", "/my-borrows"));
            }
            if (!unpaid.isEmpty()) {
                items.add(new NotificationItemVO("my-fines", "warning", "有 " + unpaid.size() + " 笔罚款待缴纳",
                        "合计 ¥" + ViewService.money(sumPenalty(unpaid)).toPlainString() + "。", "/penalties"));
            }
            var reservationSummary = reservationService.readerSummary(readerId);
            if (reservationSummary.getReadyCount() != null && reservationSummary.getReadyCount() > 0) {
                items.add(new NotificationItemVO("my-reservations", "success",
                        "有 " + reservationSummary.getReadyCount() + " 本预约图书已到书",
                        "请及时到馆借阅，留书将在 48 小时后过期。", "/my-reservations"));
            }
            if (items.isEmpty()) {
                items.add(new NotificationItemVO("reader-calm", "success", "借阅状态良好",
                        "暂无临期、逾期或待缴罚款，继续享受阅读吧。", "/my-borrows"));
            }
        }
        return new NotificationVO(items, items.size());
    }

    // ---------------------------------------------------------------------
    // 内部辅助
    // ---------------------------------------------------------------------

    private List<Book> books() {
        return bookMapper.selectList(null);
    }

    private List<Reader> readers() {
        return readerMapper.selectList(null);
    }

    private List<BookCategory> categories() {
        return bookCategoryMapper.selectList(null);
    }

    private List<BorrowRecord> records() {
        return borrowRecordMapper.selectList(null);
    }

    private List<Penalty> penalties() {
        return penaltyMapper.selectList(null);
    }

    private BigDecimal sumPenalty(List<Penalty> penalties) {
        return penalties.stream()
                .map(Penalty::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private long countBucket(List<BorrowRecordVO> records, int min, int max) {
        return records.stream()
                .filter(record -> record.getOverdueDays() != null
                        && record.getOverdueDays() >= min && record.getOverdueDays() <= max)
                .count();
    }

    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return 8;
        }
        return Math.min(limit, 20);
    }

    private boolean inPeriod(LocalDate date, LocalDate startDate, LocalDate endDate) {
        if (date == null) {
            return false;
        }
        if (startDate != null && date.isBefore(startDate)) {
            return false;
        }
        if (endDate != null && date.isAfter(endDate)) {
            return false;
        }
        return true;
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}

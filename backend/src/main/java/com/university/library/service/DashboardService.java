package com.university.library.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.university.library.common.constant.BorrowStatus;
import com.university.library.common.constant.PenaltyStatus;
import com.university.library.common.security.UserPrincipal;
import com.university.library.entity.Announcement;
import com.university.library.entity.Book;
import com.university.library.entity.BorrowRecord;
import com.university.library.entity.Penalty;
import com.university.library.entity.Reader;
import com.university.library.mapper.AnnouncementMapper;
import com.university.library.mapper.BookMapper;
import com.university.library.mapper.BorrowRecordMapper;
import com.university.library.mapper.PenaltyMapper;
import com.university.library.mapper.ReaderMapper;
import com.university.library.service.support.ViewService;
import com.university.library.vo.AnnouncementVO;
import com.university.library.vo.BookRankVO;
import com.university.library.vo.BorrowRecordVO;
import com.university.library.vo.NotificationVO;
import com.university.library.vo.PenaltyVO;
import com.university.library.vo.ReaderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 仪表盘服务：按角色组装首页数据与通知。
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final List<String> ACTIVE_STATUSES =
            List.of(BorrowStatus.BORROWED, BorrowStatus.OVERDUE);

    private final ReaderMapper readerMapper;
    private final BookMapper bookMapper;
    private final BorrowRecordMapper borrowRecordMapper;
    private final PenaltyMapper penaltyMapper;
    private final AnnouncementMapper announcementMapper;
    private final StatsService statsService;
    private final ViewService viewService;
    private final OverdueService overdueService;

    /**
     * 仪表盘：READER 返回个人借阅视图，ADMIN 返回全馆运营视图。
     */
    public Map<String, Object> dashboard(UserPrincipal principal) {
        overdueService.refresh();
        Map<String, Object> data = new LinkedHashMap<>();
        List<AnnouncementVO> announcements = announcements(3);

        if (principal.isReader()) {
            data.put("role", "READER");
            Reader reader = principal.readerId() == null ? null : readerMapper.selectById(principal.readerId());
            ReaderVO readerVO = reader == null ? null : viewService.toReaderVO(reader);
            data.put("reader", readerVO);
            data.put("overview", statsService.readerOverview(principal));

            List<BorrowRecord> activeRecords = activeRecordsOfReader(principal.readerId());
            List<BorrowRecord> sortedActive = new ArrayList<>(activeRecords);
            sortedActive.sort(Comparator.comparing(BorrowRecord::getDueDate,
                    Comparator.nullsLast(Comparator.naturalOrder())));
            data.put("activeLoans", viewService.toRecordVOs(sortedActive));

            List<BorrowRecordVO> history = viewService.toRecordVOs(borrowRecordMapper.selectList(
                            Wrappers.<BorrowRecord>lambdaQuery()
                                    .eq(BorrowRecord::getReaderId, principal.readerId())))
                    .stream()
                    .sorted(Comparator.comparing(BorrowRecordVO::getBorrowDate,
                                    Comparator.nullsLast(Comparator.reverseOrder()))
                            .thenComparing(BorrowRecordVO::getId, Comparator.reverseOrder()))
                    .limit(4)
                    .toList();
            data.put("recentHistory", history);

            Set<Long> activeBookIds = activeRecords.stream()
                    .map(BorrowRecord::getBookId).collect(Collectors.toSet());
            List<BookRankVO> recommendations = statsService.bookRanking(8, null, null).stream()
                    .filter(book -> book.getAvailableCopies() != null && book.getAvailableCopies() > 0)
                    .filter(book -> !activeBookIds.contains(book.getId()))
                    .limit(4)
                    .toList();
            data.put("recommendations", recommendations);

            List<Penalty> unpaid = principal.readerId() == null ? List.of()
                    : penaltyMapper.selectList(Wrappers.<Penalty>lambdaQuery()
                    .eq(Penalty::getReaderId, principal.readerId())
                    .eq(Penalty::getStatus, PenaltyStatus.UNPAID)
                    .orderByAsc(Penalty::getId));
            List<PenaltyVO> unpaidPenalties = viewService.toPenaltyVOs(unpaid);
            data.put("unpaidPenalties", unpaidPenalties);

            data.put("activities", statsService.recentActivities(principal, 5));
            data.put("announcements", announcements);
            data.put("trend", statsService.monthlyTrend(6));
            return data;
        }

        data.put("role", "ADMIN");
        data.put("overview", statsService.adminOverview(null, null));
        data.put("trend", statsService.monthlyTrend(8));
        data.put("categoryDistribution", statsService.categoryDistribution().stream()
                .filter(item -> item.getValue() != null && item.getValue() > 0)
                .toList());
        data.put("popularBooks", statsService.bookRanking(5, null, null));

        List<BorrowRecord> allActive = borrowRecordMapper.selectList(Wrappers.<BorrowRecord>lambdaQuery()
                .in(BorrowRecord::getStatus, ACTIVE_STATUSES)
                .orderByAsc(BorrowRecord::getId));
        List<BorrowRecord> sortedActive = new ArrayList<>(allActive);
        sortedActive.sort(Comparator.comparing(BorrowRecord::getDueDate,
                Comparator.nullsLast(Comparator.naturalOrder())));
        List<BorrowRecordVO> activeVOs = viewService.toRecordVOs(sortedActive);
        data.put("activeLoans", activeVOs.stream().limit(6).toList());

        LocalDate today = LocalDate.now();
        List<BorrowRecordVO> dueSoon = activeVOs.stream()
                .filter(record -> record.getDueDate() != null)
                .filter(record -> !record.getDueDate().isBefore(today)
                        && !record.getDueDate().isAfter(today.plusDays(3)))
                .toList();
        data.put("dueSoon", dueSoon);

        List<Book> lowStockBooks = bookMapper.selectList(null).stream()
                .filter(book -> book.getAvailableCopies() != null && book.getAvailableCopies() <= 1)
                .limit(5)
                .toList();
        data.put("lowStock", viewService.toBookVOs(lowStockBooks));
        data.put("activities", statsService.recentActivities(principal, 6));
        data.put("announcements", announcements);
        return data;
    }

    /**
     * 通知接口。
     */
    public NotificationVO notifications(UserPrincipal principal) {
        return statsService.notifications(principal);
    }

    private List<BorrowRecord> activeRecordsOfReader(Long readerId) {
        if (readerId == null) {
            return List.of();
        }
        return borrowRecordMapper.selectList(Wrappers.<BorrowRecord>lambdaQuery()
                .eq(BorrowRecord::getReaderId, readerId)
                .in(BorrowRecord::getStatus, ACTIVE_STATUSES)
                .orderByAsc(BorrowRecord::getId));
    }

    private List<AnnouncementVO> announcements(int limit) {
        List<Announcement> announcements = announcementMapper.selectList(
                Wrappers.<Announcement>lambdaQuery()
                        .orderByDesc(Announcement::getCreatedAt)
                        .orderByDesc(Announcement::getId));
        return announcements.stream().limit(limit).map(announcement -> {
            AnnouncementVO vo = new AnnouncementVO();
            BeanUtils.copyProperties(announcement, vo);
            return vo;
        }).toList();
    }
}

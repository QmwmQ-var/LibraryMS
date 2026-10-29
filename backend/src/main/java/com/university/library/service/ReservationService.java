package com.university.library.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.university.library.common.BusinessException;
import com.university.library.common.OperationResult;
import com.university.library.common.PageResult;
import com.university.library.common.config.ReservationProperties;
import com.university.library.common.constant.BorrowStatus;
import com.university.library.common.constant.CardStatus;
import com.university.library.common.constant.ReservationStatus;
import com.university.library.common.security.UserPrincipal;
import com.university.library.common.util.PageUtils;
import com.university.library.common.util.TextUtils;
import com.university.library.dto.ReservationRequest;
import com.university.library.entity.Book;
import com.university.library.entity.BookCategory;
import com.university.library.entity.BorrowRecord;
import com.university.library.entity.Reader;
import com.university.library.entity.Reservation;
import com.university.library.mapper.BookMapper;
import com.university.library.mapper.BorrowRecordMapper;
import com.university.library.mapper.ReaderMapper;
import com.university.library.mapper.ReservationMapper;
import com.university.library.service.support.ViewService;
import com.university.library.vo.ReservationSummaryVO;
import com.university.library.vo.ReservationVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 预约服务：排队、到书留书、领取、取消与过期释放。
 *
 * <p>库存约定：READY 预约会从 available_copies 中留出一册（扣减），
 * 读者借阅 READY 图书时不再扣减库存；取消/过期时释放回 available_copies，
 * 并自动把下一位 PENDING 读者提升为 READY。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationMapper reservationMapper;
    private final BookMapper bookMapper;
    private final ReaderMapper readerMapper;
    private final BorrowRecordMapper borrowRecordMapper;
    private final ReservationProperties reservationProperties;
    private final ViewService viewService;

    // ---------------------------------------------------------------------
    // 查询
    // ---------------------------------------------------------------------

    /**
     * 预约分页：mine=true 仅当前读者，false 为管理员全量。
     */
    @Transactional
    public PageResult<ReservationVO> list(UserPrincipal principal, boolean mine, String status, String keyword,
                                          long page, long size) {
        expireReadyInternal();
        Long readerId = mine ? requireReaderId(principal) : null;

        List<Reservation> scope = reservationMapper.selectList(Wrappers.<Reservation>lambdaQuery()
                .eq(readerId != null, Reservation::getReaderId, readerId)
                .orderByAsc(Reservation::getRequestedAt)
                .orderByAsc(Reservation::getId));

        ReservationSummaryVO summary = buildSummary(scope);
        List<Reservation> filtered = scope.stream()
                .filter(reservation -> matchesStatus(reservation, status))
                .filter(reservation -> matchesKeyword(reservation, keyword))
                .sorted(reservationComparator())
                .toList();

        long currentPage = PageUtils.normalizePage(page);
        long pageSize = PageUtils.normalizeSize(size);
        List<Reservation> pageRecords = PageUtils.slice(filtered, currentPage, pageSize);
        List<ReservationVO> records = toVOs(pageRecords);
        return PageResult.withSummary(records, filtered.size(), currentPage, pageSize, summary);
    }

    /**
     * 当前用户视角的预约汇总（读者仅本人，管理员全馆）。
     */
    @Transactional
    public ReservationSummaryVO summary(UserPrincipal principal) {
        expireReadyInternal();
        return buildSummary(scopeOf(principal));
    }

    /**
     * 全馆预约汇总，供仪表盘统计。
     */
    @Transactional
    public ReservationSummaryVO adminSummary() {
        expireReadyInternal();
        return buildSummary(reservationMapper.selectList(null));
    }

    /**
     * 指定读者的预约汇总，供读者概览与通知使用。
     */
    @Transactional
    public ReservationSummaryVO readerSummary(Long readerId) {
        expireReadyInternal();
        return buildSummary(reservationMapper.selectList(Wrappers.<Reservation>lambdaQuery()
                .eq(Reservation::getReaderId, readerId)));
    }

    // ---------------------------------------------------------------------
    // 创建 / 取消
    // ---------------------------------------------------------------------

    /**
     * 读者预约：仅在无可借副本时允许，按预约时间排队。
     */
    @Transactional
    public OperationResult<ReservationVO> create(UserPrincipal principal, ReservationRequest request) {
        expireReadyInternal();
        Long readerId = requireReaderId(principal);
        // 锁定读者行，串行化同一读者的预约/借阅，避免重复预约。
        Reader reader = readerMapper.selectByIdForUpdate(readerId);
        if (reader == null) {
            throw BusinessException.notFound("读者不存在");
        }
        if (!CardStatus.ACTIVE.equals(reader.getCardStatus())) {
            throw BusinessException.badRequest("借阅证状态异常，暂不能预约，请联系图书馆管理员");
        }

        Book book = bookMapper.selectById(request.getBookId());
        if (book == null) {
            throw BusinessException.notFound("图书不存在或已下架");
        }
        if (book.getAvailableCopies() != null && book.getAvailableCopies() > 0) {
            throw BusinessException.badRequest("当前有可借副本，请直接借阅");
        }

        List<Reservation> existing = reservationMapper.selectList(Wrappers.<Reservation>lambdaQuery()
                .eq(Reservation::getReaderId, readerId)
                .eq(Reservation::getBookId, book.getId())
                .in(Reservation::getStatus, ReservationStatus.ACTIVE));
        if (existing.stream().anyMatch(item -> ReservationStatus.READY.equals(item.getStatus()))) {
            throw BusinessException.badRequest("该书已为您到书，请直接借阅");
        }
        if (!existing.isEmpty()) {
            throw BusinessException.badRequest("您已预约该书，请耐心等待");
        }

        Long activeBorrow = borrowRecordMapper.selectCount(Wrappers.<BorrowRecord>lambdaQuery()
                .eq(BorrowRecord::getReaderId, readerId)
                .eq(BorrowRecord::getBookId, book.getId())
                .in(BorrowRecord::getStatus, List.of(BorrowStatus.BORROWED, BorrowStatus.OVERDUE)));
        if (activeBorrow != null && activeBorrow > 0) {
            throw BusinessException.badRequest("您已借阅该书，请勿重复预约");
        }

        Long activeReservations = reservationMapper.selectCount(Wrappers.<Reservation>lambdaQuery()
                .eq(Reservation::getReaderId, readerId)
                .in(Reservation::getStatus, ReservationStatus.ACTIVE));
        if (activeReservations != null && activeReservations >= reservationProperties.getMaxActivePerReader()) {
            throw BusinessException.badRequest("同时最多持有 " + reservationProperties.getMaxActivePerReader() + " 个预约");
        }

        LocalDateTime now = LocalDateTime.now();
        Reservation reservation = new Reservation();
        reservation.setReaderId(reader.getId());
        reservation.setBookId(book.getId());
        reservation.setStatus(ReservationStatus.PENDING);
        reservation.setRequestedAt(now);
        reservation.setReaderName(reader.getName());
        reservation.setReaderNo(reader.getStudentNo());
        reservation.setBookTitle(book.getTitle());
        reservation.setBookIsbn(book.getIsbn());
        reservation.setCreatedAt(now);
        reservation.setUpdatedAt(now);
        reservationMapper.insert(reservation);
        bookMapper.adjustReservationCount(book.getId(), 1);

        ReservationVO vo = toVOs(List.of(reservation)).get(0);
        int position = vo.getQueuePosition() == null ? 1 : vo.getQueuePosition();
        return OperationResult.of(vo, "预约成功，当前排在第 " + position + " 位");
    }

    /**
     * 取消预约：读者本人或管理员；READY 取消会释放留书并顺延下一位。
     */
    @Transactional
    public OperationResult<ReservationVO> cancel(UserPrincipal principal, Long id) {
        expireReadyInternal();
        Reservation reservation = reservationMapper.selectByIdForUpdate(id);
        if (reservation == null) {
            throw BusinessException.notFound("预约记录不存在");
        }
        if (!principal.isAdmin()) {
            if (principal.readerId() == null || !principal.readerId().equals(reservation.getReaderId())) {
                throw BusinessException.forbidden("无权取消其他读者的预约");
            }
        }
        if (!ReservationStatus.isActive(reservation.getStatus())) {
            throw BusinessException.badRequest("该预约已结束，无法取消");
        }

        boolean wasReady = ReservationStatus.READY.equals(reservation.getStatus());
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setClosedAt(LocalDateTime.now());
        reservation.setCancelReason(principal.isAdmin() ? "管理员取消预约" : "读者主动取消");
        reservation.setUpdatedAt(LocalDateTime.now());
        reservationMapper.updateById(reservation);
        bookMapper.adjustReservationCount(reservation.getBookId(), -1);

        if (wasReady) {
            // READY 的副本此前已从库存扣除，取消后先释放，再顺延下一位。
            bookMapper.incrementAvailable(reservation.getBookId());
            promoteNextReady(reservation.getBookId());
        }
        return OperationResult.of(toVOs(List.of(reservation)).get(0), "预约已取消");
    }

    // ---------------------------------------------------------------------
    // 借阅联动
    // ---------------------------------------------------------------------

    /**
     * 借书时查找当前读者对该书的 READY 预约。
     */
    public Reservation findReadyReservation(Long readerId, Long bookId) {
        return reservationMapper.selectReady(readerId, bookId);
    }

    /**
     * 借书成功：把 READY 预约标记为已履约，并减少有效预约数。
     * READY 副本是从库存预留的，因此这里不再调整 available_copies。
     */
    @Transactional
    public void fulfill(Reservation reservation, Long borrowRecordId) {
        LocalDateTime now = LocalDateTime.now();
        reservation.setStatus(ReservationStatus.FULFILLED);
        reservation.setBorrowRecordId(borrowRecordId);
        reservation.setClosedAt(now);
        reservation.setUpdatedAt(now);
        reservationMapper.updateById(reservation);
        bookMapper.adjustReservationCount(reservation.getBookId(), -1);
    }

    /**
     * 还书后调用：若仍有可借副本且有人排队，则把队首提升为 READY 并留书。
     *
     * @return 被提升的预约；没有则返回 null
     */
    @Transactional
    public Reservation promoteNextReady(Long bookId) {
        Book book = bookMapper.selectById(bookId);
        if (book == null || book.getAvailableCopies() == null || book.getAvailableCopies() <= 0) {
            return null;
        }
        Reservation next = reservationMapper.selectOldestPendingForUpdate(bookId);
        if (next == null) {
            return null;
        }
        if (bookMapper.decrementAvailable(bookId) == 0) {
            return null;
        }
        LocalDateTime now = LocalDateTime.now();
        next.setStatus(ReservationStatus.READY);
        next.setReadyAt(now);
        next.setExpireAt(now.plusHours(reservationProperties.getReadyHoldHours()));
        next.setUpdatedAt(now);
        reservationMapper.updateById(next);
        return next;
    }

    /**
     * 借书/查询前刷新过期预约，释放留书并顺延下一位。
     */
    public int refreshExpired() {
        return expireReadyInternal();
    }

    /**
     * 每 10 分钟兜底清理过期到书预约。
     */
    @Scheduled(cron = "0 */10 * * * ?")
    @Transactional
    public void scheduledExpire() {
        int changed = expireReadyInternal();
        if (changed > 0) {
            log.info("定时任务：{} 个到书预约已过期并释放留书", changed);
        }
    }

    // ---------------------------------------------------------------------
    // 删除保护
    // ---------------------------------------------------------------------

    public long countActiveForBook(Long bookId) {
        Long count = reservationMapper.selectCount(Wrappers.<Reservation>lambdaQuery()
                .eq(Reservation::getBookId, bookId)
                .in(Reservation::getStatus, ReservationStatus.ACTIVE));
        return count == null ? 0 : count;
    }

    public long countActiveForReader(Long readerId) {
        Long count = reservationMapper.selectCount(Wrappers.<Reservation>lambdaQuery()
                .eq(Reservation::getReaderId, readerId)
                .in(Reservation::getStatus, ReservationStatus.ACTIVE));
        return count == null ? 0 : count;
    }

    /**
     * READY 预约占用的留书数量；编辑馆藏总量时用于计算真实可借数。
     */
    public long countReadyForBook(Long bookId) {
        Long count = reservationMapper.selectCount(Wrappers.<Reservation>lambdaQuery()
                .eq(Reservation::getBookId, bookId)
                .eq(Reservation::getStatus, ReservationStatus.READY));
        return count == null ? 0 : count;
    }

    // ---------------------------------------------------------------------
    // 内部方法
    // ---------------------------------------------------------------------

    private int expireReadyInternal() {
        LocalDateTime now = LocalDateTime.now();
        List<Reservation> candidates = reservationMapper.selectList(Wrappers.<Reservation>lambdaQuery()
                .eq(Reservation::getStatus, ReservationStatus.READY)
                .isNotNull(Reservation::getExpireAt)
                .lt(Reservation::getExpireAt, now));
        int changed = 0;
        for (Reservation candidate : candidates) {
            // 锁定并再次确认状态，避免与领取/取消并发。
            Reservation reservation = reservationMapper.selectByIdForUpdate(candidate.getId());
            if (reservation == null
                    || !ReservationStatus.READY.equals(reservation.getStatus())
                    || reservation.getExpireAt() == null
                    || !reservation.getExpireAt().isBefore(now)) {
                continue;
            }
            reservation.setStatus(ReservationStatus.EXPIRED);
            reservation.setClosedAt(now);
            reservation.setCancelReason("到书保留超时");
            reservation.setUpdatedAt(now);
            reservationMapper.updateById(reservation);
            bookMapper.adjustReservationCount(reservation.getBookId(), -1);
            bookMapper.incrementAvailable(reservation.getBookId());
            promoteNextReady(reservation.getBookId());
            changed++;
        }
        return changed;
    }

    private List<Reservation> scopeOf(UserPrincipal principal) {
        if (principal != null && principal.isAdmin()) {
            return reservationMapper.selectList(null);
        }
        Long readerId = requireReaderId(principal);
        return reservationMapper.selectList(Wrappers.<Reservation>lambdaQuery()
                .eq(Reservation::getReaderId, readerId));
    }

    private Long requireReaderId(UserPrincipal principal) {
        if (principal == null || principal.readerId() == null) {
            throw BusinessException.badRequest("当前账号不是读者账号");
        }
        return principal.readerId();
    }

    private boolean matchesStatus(Reservation reservation, String status) {
        if (TextUtils.isBlank(status) || "ALL".equalsIgnoreCase(status)) {
            return true;
        }
        if ("ACTIVE".equalsIgnoreCase(status)) {
            return ReservationStatus.isActive(reservation.getStatus());
        }
        return status.equalsIgnoreCase(reservation.getStatus());
    }

    private boolean matchesKeyword(Reservation reservation, String keyword) {
        return TextUtils.containsIgnoreCase(reservation.getReaderName(), keyword)
                || TextUtils.containsIgnoreCase(reservation.getReaderNo(), keyword)
                || TextUtils.containsIgnoreCase(reservation.getBookTitle(), keyword)
                || TextUtils.containsIgnoreCase(reservation.getBookIsbn(), keyword);
    }

    private Comparator<Reservation> reservationComparator() {
        return (a, b) -> {
            boolean aActive = ReservationStatus.isActive(a.getStatus());
            boolean bActive = ReservationStatus.isActive(b.getStatus());
            if (aActive != bActive) {
                return aActive ? -1 : 1;
            }
            if (aActive) {
                return Comparator.nullsLast(Comparator.<LocalDateTime>naturalOrder())
                        .compare(a.getRequestedAt(), b.getRequestedAt());
            }
            return Comparator.nullsLast(Comparator.<LocalDateTime>reverseOrder())
                    .compare(a.getRequestedAt(), b.getRequestedAt());
        };
    }

    private ReservationSummaryVO buildSummary(List<Reservation> reservations) {
        ReservationSummaryVO summary = new ReservationSummaryVO();
        summary.setPendingCount(reservations.stream()
                .filter(item -> ReservationStatus.PENDING.equals(item.getStatus())).count());
        summary.setReadyCount(reservations.stream()
                .filter(item -> ReservationStatus.READY.equals(item.getStatus())).count());
        summary.setFulfilledCount(reservations.stream()
                .filter(item -> ReservationStatus.FULFILLED.equals(item.getStatus())).count());
        summary.setCancelledCount(reservations.stream()
                .filter(item -> ReservationStatus.CANCELLED.equals(item.getStatus())).count());
        summary.setExpiredCount(reservations.stream()
                .filter(item -> ReservationStatus.EXPIRED.equals(item.getStatus())).count());
        summary.setActiveCount(reservations.stream()
                .filter(item -> ReservationStatus.isActive(item.getStatus())).count());
        summary.setTotalCount((long) reservations.size());
        return summary;
    }

    private List<ReservationVO> toVOs(List<Reservation> reservations) {
        if (reservations == null || reservations.isEmpty()) {
            return List.of();
        }
        Map<Long, Reader> readers = viewService.readerMap();
        Map<Long, Book> books = viewService.bookMap();
        Map<Long, BookCategory> categories = viewService.categoryMap();
        Set<Long> bookIds = reservations.stream()
                .filter(item -> ReservationStatus.isActive(item.getStatus()))
                .map(Reservation::getBookId)
                .collect(Collectors.toSet());

        Map<Long, List<Reservation>> activeByBook = bookIds.isEmpty()
                ? Map.of()
                : reservationMapper.selectList(Wrappers.<Reservation>lambdaQuery()
                        .in(Reservation::getBookId, bookIds)
                        .in(Reservation::getStatus, ReservationStatus.ACTIVE)
                        .orderByAsc(Reservation::getRequestedAt)
                        .orderByAsc(Reservation::getId))
                .stream()
                .collect(Collectors.groupingBy(Reservation::getBookId));

        LocalDateTime now = LocalDateTime.now();
        return reservations.stream().map(reservation -> {
            ReservationVO vo = new ReservationVO();
            Book book = books.get(reservation.getBookId());
            Reader reader = readers.get(reservation.getReaderId());
            BookCategory category = book == null ? null : categories.get(book.getCategoryId());

            vo.setId(reservation.getId());
            vo.setReaderId(reservation.getReaderId());
            vo.setReaderName(reader != null ? reader.getName()
                    : TextUtils.defaultIfBlank(reservation.getReaderName(), "已注销读者"));
            vo.setReaderNo(reader != null ? reader.getStudentNo()
                    : TextUtils.defaultIfBlank(reservation.getReaderNo(), "—"));
            vo.setReaderType(reader == null ? null : reader.getType());
            vo.setBookId(reservation.getBookId());
            vo.setBookTitle(book != null ? book.getTitle()
                    : TextUtils.defaultIfBlank(reservation.getBookTitle(), "已下架图书"));
            vo.setBookIsbn(book != null ? book.getIsbn()
                    : TextUtils.defaultIfBlank(reservation.getBookIsbn(), "—"));
            vo.setBookAuthor(book == null ? "—" : book.getAuthor());
            vo.setCategoryName(category == null ? "未分类" : category.getName());
            vo.setCategoryColor(category == null ? "#6f7f79" : category.getColor());
            vo.setStatus(reservation.getStatus());
            vo.setStatusLabel(ReservationStatus.label(reservation.getStatus()));
            vo.setAvailableCopies(book == null ? 0 : book.getAvailableCopies());
            vo.setReservationCount(book == null ? 0 : book.getReservationCount());
            vo.setRequestedAt(reservation.getRequestedAt());
            vo.setReadyAt(reservation.getReadyAt());
            vo.setExpireAt(reservation.getExpireAt());
            vo.setClosedAt(reservation.getClosedAt());
            vo.setBorrowRecordId(reservation.getBorrowRecordId());
            vo.setCancelReason(reservation.getCancelReason());

            if (ReservationStatus.isActive(reservation.getStatus())) {
                List<Reservation> queue = activeByBook.getOrDefault(reservation.getBookId(), List.of());
                int index = -1;
                for (int i = 0; i < queue.size(); i++) {
                    if (Objects.equals(queue.get(i).getId(), reservation.getId())) {
                        index = i;
                        break;
                    }
                }
                if (index >= 0) {
                    vo.setQueuePosition(index + 1);
                    vo.setAheadCount(index);
                }
            }
            if (ReservationStatus.READY.equals(reservation.getStatus())
                    && reservation.getExpireAt() != null) {
                long minutes = Math.max(Duration.between(now, reservation.getExpireAt()).toMinutes(), 0);
                vo.setRemainingHours(minutes == 0 ? 0 : (minutes + 59) / 60);
            }
            return vo;
        }).toList();
    }
}

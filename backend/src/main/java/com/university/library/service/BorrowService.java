package com.university.library.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.university.library.common.BusinessException;
import com.university.library.common.OperationResult;
import com.university.library.common.PageResult;
import com.university.library.common.config.PenaltyProperties;
import com.university.library.common.constant.BorrowStatus;
import com.university.library.common.constant.CardStatus;
import com.university.library.common.constant.PenaltyStatus;
import com.university.library.common.constant.ReaderType;
import com.university.library.common.security.UserPrincipal;
import com.university.library.common.util.DateUtils;
import com.university.library.common.util.PageUtils;
import com.university.library.common.util.TextUtils;
import com.university.library.dto.BorrowRequest;
import com.university.library.entity.Book;
import com.university.library.entity.BorrowRecord;
import com.university.library.entity.Penalty;
import com.university.library.entity.Reader;
import com.university.library.mapper.BookMapper;
import com.university.library.mapper.BorrowRecordMapper;
import com.university.library.mapper.PenaltyMapper;
import com.university.library.mapper.ReaderMapper;
import com.university.library.service.support.ViewService;
import com.university.library.vo.BookVO;
import com.university.library.vo.BorrowRecordDetailVO;
import com.university.library.vo.BorrowRecordVO;
import com.university.library.vo.BorrowResultVO;
import com.university.library.vo.BorrowSummaryVO;
import com.university.library.vo.PenaltyVO;
import com.university.library.vo.ReaderVO;
import com.university.library.vo.ReturnResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * 借阅服务：借书、还书、续借以及借阅记录查询。
 *
 * <p>借书/还书均使用 @Transactional：借阅记录与图书可借数量在同一事务中更新，
 * 任一步失败整体回滚；并通过行锁/条件更新保证并发安全。</p>
 */
@Service
@RequiredArgsConstructor
public class BorrowService {

    private static final List<String> ACTIVE_STATUSES =
            List.of(BorrowStatus.BORROWED, BorrowStatus.OVERDUE);

    private final BorrowRecordMapper borrowRecordMapper;
    private final BookMapper bookMapper;
    private final ReaderMapper readerMapper;
    private final PenaltyMapper penaltyMapper;
    private final ViewService viewService;
    private final OverdueService overdueService;
    private final PenaltyProperties penaltyProperties;

    // ---------------------------------------------------------------------
    // 查询
    // ---------------------------------------------------------------------

    /**
     * 借阅记录分页查询。
     *
     * @param mine true 表示“我的借阅”（强制按当前读者过滤），false 表示管理员全量查询
     */
    public PageResult<BorrowRecordVO> list(UserPrincipal principal, boolean mine, Long readerId, Long bookId,
                                           String status, String keyword, LocalDate startDate, LocalDate endDate,
                                           long page, long size) {
        overdueService.refresh();

        Long effectiveReaderId = readerId;
        if (mine) {
            effectiveReaderId = requireReaderId(principal);
        }

        LambdaQueryWrapper<BorrowRecord> wrapper = Wrappers.lambdaQuery();
        if (effectiveReaderId != null) {
            wrapper.eq(BorrowRecord::getReaderId, effectiveReaderId);
        }
        if (bookId != null) {
            wrapper.eq(BorrowRecord::getBookId, bookId);
        }
        if (startDate != null) {
            wrapper.ge(BorrowRecord::getBorrowDate, startDate);
        }
        if (endDate != null) {
            wrapper.le(BorrowRecord::getBorrowDate, endDate);
        }
        wrapper.orderByAsc(BorrowRecord::getId);

        List<BorrowRecordVO> records = new ArrayList<>(viewService.toRecordVOs(
                borrowRecordMapper.selectList(wrapper)));

        List<BorrowRecordVO> filtered = records.stream()
                .filter(record -> matchesStatus(record, status))
                .filter(record -> matchesKeyword(record, keyword))
                .toList();

        List<BorrowRecordVO> sorted = new ArrayList<>(filtered);
        if (mine) {
            sorted.sort(Comparator
                    .comparing((BorrowRecordVO record) -> BorrowStatus.isActive(record.getStatus()))
                    .reversed()
                    .thenComparing(BorrowRecordVO::getBorrowDate,
                            Comparator.nullsLast(Comparator.reverseOrder())));
        } else {
            sorted.sort(Comparator
                    .comparing((BorrowRecordVO record) -> BorrowStatus.RETURNED.equals(record.getStatus()))
                    .thenComparing(BorrowRecordVO::getBorrowDate,
                            Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(BorrowRecordVO::getId, Comparator.reverseOrder()));
        }

        long currentPage = PageUtils.normalizePage(page);
        long pageSize = PageUtils.normalizeSize(size);
        List<BorrowRecordVO> pageRecords = PageUtils.slice(sorted, currentPage, pageSize);

        if (!mine) {
            return PageResult.of(pageRecords, sorted.size(), currentPage, pageSize);
        }

        BorrowSummaryVO summary = new BorrowSummaryVO();
        // 页签计数使用当前读者的全部借阅记录，不受状态/关键字/日期筛选影响，
        // 避免切换到“借阅中”后其他页签计数变成 0。
        List<BorrowRecord> allOfReader = borrowRecordMapper.selectList(
                Wrappers.<BorrowRecord>lambdaQuery()
                        .eq(BorrowRecord::getReaderId, effectiveReaderId));
        summary.setActive(allOfReader.stream().filter(r -> BorrowStatus.isActive(r.getStatus())).count());
        summary.setOverdue(allOfReader.stream().filter(r -> BorrowStatus.OVERDUE.equals(r.getStatus())).count());
        summary.setReturned(allOfReader.stream().filter(r -> BorrowStatus.RETURNED.equals(r.getStatus())).count());
        summary.setTotal((long) allOfReader.size());
        return PageResult.withSummary(pageRecords, sorted.size(), currentPage, pageSize, summary);
    }

    /**
     * 借阅记录详情：读者仅能查看自己的记录。附带罚款信息（如有）。
     */
    public BorrowRecordDetailVO detail(UserPrincipal principal, Long id) {
        overdueService.refresh();
        BorrowRecord record = requireRecord(id);
        canOperate(principal, record);
        Penalty penalty = penaltyMapper.selectOne(
                Wrappers.<Penalty>lambdaQuery().eq(Penalty::getRecordId, record.getId()));
        BorrowRecordVO recordVO = viewService.toRecordVO(record);
        BorrowRecordDetailVO detail = new BorrowRecordDetailVO();
        BeanUtils.copyProperties(recordVO, detail);
        detail.setPenalty(penalty == null ? null : viewService.toPenaltyVO(penalty));
        return detail;
    }

    // ---------------------------------------------------------------------
    // 借书
    // ---------------------------------------------------------------------

    /**
     * 借书事务：
     * 1. 校验读者借阅证状态；2. 校验图书可借数量；3. 校验借阅上限与重复借阅；
     * 4. 扣减库存；5. 生成借阅记录。全部成功才提交。
     */
    @Transactional
    public OperationResult<BorrowResultVO> borrow(UserPrincipal principal, BorrowRequest request) {
        overdueService.refresh();

        Long readerId;
        if (principal.isReader()) {
            readerId = principal.readerId();
            if (readerId == null) {
                throw BusinessException.forbidden("当前账号未绑定读者信息，无法借书");
            }
            if (request.getReaderId() != null && !readerId.equals(request.getReaderId())) {
                throw BusinessException.forbidden("读者只能为自己办理借阅");
            }
        } else {
            readerId = request.getReaderId();
            if (readerId == null) {
                throw BusinessException.badRequest("请选择借阅读者");
            }
        }
        if (request.getBookId() == null) {
            throw BusinessException.badRequest("请选择要借阅的图书");
        }

        // 锁定读者行：同一读者的并发借书串行执行，避免同时突破借阅上限
        Reader reader = readerMapper.selectByIdForUpdate(readerId);
        if (reader == null) {
            throw BusinessException.notFound("读者不存在");
        }
        if (!CardStatus.ACTIVE.equals(reader.getCardStatus())) {
            throw BusinessException.badRequest(
                    "借阅证已" + CardStatus.label(reader.getCardStatus()) + "，暂不能借书，请联系图书馆管理员");
        }

        Book book = bookMapper.selectById(request.getBookId());
        if (book == null) {
            throw BusinessException.notFound("图书不存在或已下架");
        }
        if (book.getAvailableCopies() == null || book.getAvailableCopies() <= 0) {
            throw BusinessException.badRequest("该书暂无可借副本，请选择其他图书");
        }

        ReaderType readerType = ReaderType.from(reader.getType());
        List<BorrowRecord> activeRecords = borrowRecordMapper.selectList(Wrappers.<BorrowRecord>lambdaQuery()
                .eq(BorrowRecord::getReaderId, reader.getId())
                .in(BorrowRecord::getStatus, ACTIVE_STATUSES));
        if (activeRecords.size() >= readerType.getMaxBorrow()) {
            throw BusinessException.badRequest(
                    "已达借阅上限（" + readerType.getMaxBorrow() + " 本），请先归还部分图书");
        }
        if (activeRecords.stream().anyMatch(record -> Objects.equals(record.getBookId(), book.getId()))) {
            throw BusinessException.badRequest("该读者已借阅此书，请勿重复借阅");
        }

        // 条件更新：仍有库存才会成功，避免并发超借
        int updated = bookMapper.decrementAvailable(book.getId());
        if (updated == 0) {
            throw BusinessException.badRequest("该书暂无可借副本，请选择其他图书");
        }

        LocalDate today = LocalDate.now();
        BorrowRecord record = new BorrowRecord();
        record.setReaderId(reader.getId());
        record.setBookId(book.getId());
        record.setBorrowDate(today);
        record.setDueDate(today.plusDays(readerType.getBorrowDays()));
        record.setStatus(BorrowStatus.BORROWED);
        record.setRenewCount(0);
        record.setOperatorId(principal.userId());
        record.setReaderName(reader.getName());
        record.setReaderNo(reader.getStudentNo());
        record.setBookTitle(book.getTitle());
        record.setBookIsbn(book.getIsbn());
        borrowRecordMapper.insert(record);

        BorrowRecordVO recordVO = viewService.toRecordVO(record);
        ReaderVO readerVO = viewService.toReaderVO(reader);
        String message = "借阅成功，应还日期为 " + DateUtils.formatMonthDay(record.getDueDate());
        return OperationResult.of(new BorrowResultVO(recordVO, readerVO), message);
    }

    // ---------------------------------------------------------------------
    // 还书
    // ---------------------------------------------------------------------

    /**
     * 还书事务：更新归还日期与状态、恢复库存、按逾期天数生成罚款。
     *
     * <p>幂等性：锁定记录行后校验状态，仅 BORROWED/OVERDUE 可归还；
     * 罚款按 record_id 唯一，重复请求不会生成第二笔罚款。</p>
     */
    @Transactional
    public OperationResult<ReturnResultVO> returnBook(UserPrincipal principal, Long id) {
        overdueService.refresh();
        BorrowRecord record = borrowRecordMapper.selectByIdForUpdate(id);
        if (record == null) {
            throw BusinessException.notFound("借阅记录不存在");
        }
        canOperate(principal, record);
        if (!BorrowStatus.isActive(record.getStatus())) {
            throw BusinessException.badRequest("该借阅记录已归还，请勿重复操作");
        }

        LocalDate today = LocalDate.now();
        long overdueDays = 0;
        if (record.getDueDate() != null && record.getDueDate().isBefore(today)) {
            overdueDays = ChronoUnit.DAYS.between(record.getDueDate(), today);
        }

        record.setReturnDate(today);
        record.setStatus(BorrowStatus.RETURNED);
        record.setReturnedBy(principal.userId());
        borrowRecordMapper.updateById(record);

        // 恢复可借数量（幂等：不超过馆藏总量）
        bookMapper.incrementAvailable(record.getBookId());

        Penalty penalty = penaltyMapper.selectOne(
                Wrappers.<Penalty>lambdaQuery().eq(Penalty::getRecordId, record.getId()));
        if (overdueDays > 0 && penalty == null) {
            BigDecimal rate = penaltyProperties.getRatePerDay();
            Penalty created = new Penalty();
            created.setRecordId(record.getId());
            created.setReaderId(record.getReaderId());
            created.setBookId(record.getBookId());
            created.setOverdueDays((int) overdueDays);
            created.setAmount(rate.multiply(BigDecimal.valueOf(overdueDays))
                    .setScale(2, RoundingMode.HALF_UP));
            created.setStatus(PenaltyStatus.UNPAID);
            created.setRuleRate(rate);
            penaltyMapper.insert(created);
            penalty = created;
        }

        Book book = bookMapper.selectById(record.getBookId());
        BorrowRecordVO recordVO = viewService.toRecordVO(record);
        PenaltyVO penaltyVO = penalty == null ? null : viewService.toPenaltyVO(penalty);
        BookVO bookVO = book == null ? null : viewService.toBookVO(book);
        String message = overdueDays > 0
                ? "归还成功，已逾期 " + overdueDays + " 天，产生罚款 ¥"
                + (penalty == null ? "0.00" : penalty.getAmount().toPlainString())
                : "归还成功，感谢您按时归还图书";
        return OperationResult.of(new ReturnResultVO(recordVO, penaltyVO, bookVO), message);
    }

    // ---------------------------------------------------------------------
    // 续借
    // ---------------------------------------------------------------------

    /**
     * 续借：最多 1 次；已逾期不可续借；图书被预约时不可续借；延长一个借阅周期。
     */
    @Transactional
    public OperationResult<BorrowRecordVO> renew(UserPrincipal principal, Long id) {
        overdueService.refresh();
        BorrowRecord record = borrowRecordMapper.selectByIdForUpdate(id);
        if (record == null) {
            throw BusinessException.notFound("借阅记录不存在");
        }
        canOperate(principal, record);
        if (!BorrowStatus.isActive(record.getStatus())) {
            throw BusinessException.badRequest("该借阅记录已结束，无法续借");
        }
        if (BorrowStatus.OVERDUE.equals(record.getStatus())) {
            throw BusinessException.badRequest("图书已逾期，无法续借，请先归还并缴纳罚款");
        }
        if (record.getRenewCount() != null && record.getRenewCount() >= 1) {
            throw BusinessException.badRequest("每本图书最多续借 1 次");
        }
        Book book = bookMapper.selectById(record.getBookId());
        if (book == null) {
            throw BusinessException.notFound("图书已下架，无法续借");
        }
        if (book.getReservationCount() != null && book.getReservationCount() > 0) {
            throw BusinessException.badRequest("该书已被其他读者预约，暂不能续借");
        }
        Reader reader = readerMapper.selectById(record.getReaderId());
        ReaderType readerType = ReaderType.from(reader == null ? null : reader.getType());
        record.setDueDate(record.getDueDate().plusDays(readerType.getBorrowDays()));
        record.setRenewCount((record.getRenewCount() == null ? 0 : record.getRenewCount()) + 1);
        borrowRecordMapper.updateById(record);

        BorrowRecordVO recordVO = viewService.toRecordVO(record);
        return OperationResult.of(recordVO,
                "续借成功，新的应还日期为 " + DateUtils.formatMonthDay(record.getDueDate()));
    }

    // ---------------------------------------------------------------------
    // 内部方法
    // ---------------------------------------------------------------------

    private BorrowRecord requireRecord(Long id) {
        BorrowRecord record = borrowRecordMapper.selectById(id);
        if (record == null) {
            throw BusinessException.notFound("借阅记录不存在");
        }
        return record;
    }

    private void canOperate(UserPrincipal principal, BorrowRecord record) {
        if (principal.isAdmin()) {
            return;
        }
        if (principal.readerId() == null || !principal.readerId().equals(record.getReaderId())) {
            throw BusinessException.forbidden("无权操作其他读者的借阅记录");
        }
    }

    private Long requireReaderId(UserPrincipal principal) {
        if (principal.readerId() == null) {
            throw BusinessException.badRequest("当前账号不是读者账号");
        }
        return principal.readerId();
    }

    private boolean matchesStatus(BorrowRecordVO record, String status) {
        if (TextUtils.isBlank(status) || "ALL".equals(status)) {
            return true;
        }
        if ("ACTIVE".equals(status)) {
            return BorrowStatus.isActive(record.getStatus());
        }
        return status.equals(record.getStatus());
    }

    private boolean matchesKeyword(BorrowRecordVO record, String keyword) {
        return TextUtils.containsIgnoreCase(record.getReaderName(), keyword)
                || TextUtils.containsIgnoreCase(record.getReaderNo(), keyword)
                || TextUtils.containsIgnoreCase(record.getBookTitle(), keyword)
                || TextUtils.containsIgnoreCase(record.getBookIsbn(), keyword);
    }
}

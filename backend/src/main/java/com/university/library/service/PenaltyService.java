package com.university.library.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.university.library.common.BusinessException;
import com.university.library.common.OperationResult;
import com.university.library.common.PageResult;
import com.university.library.common.config.PenaltyProperties;
import com.university.library.common.constant.PenaltyStatus;
import com.university.library.common.security.UserPrincipal;
import com.university.library.common.util.PageUtils;
import com.university.library.common.util.TextUtils;
import com.university.library.entity.Penalty;
import com.university.library.mapper.PenaltyMapper;
import com.university.library.service.support.ViewService;
import com.university.library.vo.PenaltySummaryVO;
import com.university.library.vo.PenaltyVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * 罚款服务：罚款查询、汇总、确认缴纳。
 */
@Service
@RequiredArgsConstructor
public class PenaltyService {

    private final PenaltyMapper penaltyMapper;
    private final ViewService viewService;
    private final PenaltyProperties penaltyProperties;

    /**
     * 罚款汇总：管理员统计全量，读者统计本人。
     */
    public PenaltySummaryVO summary(UserPrincipal principal) {
        return buildSummary(penaltiesFor(principal));
    }

    /**
     * 罚款分页查询。
     *
     * @param mine true 表示读者本人罚款，false 表示管理员全量
     */
    public PageResult<PenaltyVO> list(UserPrincipal principal, boolean mine, String status, String keyword,
                                      long page, long size) {
        List<Penalty> penalties = mine ? penaltiesOfReader(requireReaderId(principal)) : penaltiesFor(principal);
        List<PenaltyVO> all = viewService.toPenaltyVOs(penalties);
        PenaltySummaryVO summary = buildSummary(penalties);

        List<PenaltyVO> filtered = new ArrayList<>();
        for (PenaltyVO penalty : all) {
            if (!TextUtils.isBlank(status) && !"ALL".equals(status) && !status.equals(penalty.getStatus())) {
                continue;
            }
            if (!TextUtils.containsIgnoreCase(penalty.getReaderName(), keyword)
                    && !TextUtils.containsIgnoreCase(penalty.getReaderNo(), keyword)
                    && !TextUtils.containsIgnoreCase(penalty.getBookTitle(), keyword)
                    && !TextUtils.containsIgnoreCase(penalty.getBookIsbn(), keyword)) {
                continue;
            }
            filtered.add(penalty);
        }

        filtered.sort(Comparator
                .comparing((PenaltyVO penalty) -> !PenaltyStatus.UNPAID.equals(penalty.getStatus()))
                .thenComparing(PenaltyVO::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())));

        long currentPage = PageUtils.normalizePage(page);
        long pageSize = PageUtils.normalizeSize(size);
        List<PenaltyVO> records = PageUtils.slice(filtered, currentPage, pageSize);
        return PageResult.withSummary(records, filtered.size(), currentPage, pageSize, summary);
    }

    /**
     * 确认缴纳：读者自助（ONLINE）或管理员代收（CASH）。
     */
    @Transactional
    public OperationResult<PenaltyVO> pay(UserPrincipal principal, Long id) {
        Penalty penalty = penaltyMapper.selectByIdForUpdate(id);
        if (penalty == null) {
            throw BusinessException.notFound("罚款记录不存在");
        }
        requireReaderOrAdmin(principal, penalty.getReaderId());
        if (PenaltyStatus.PAID.equals(penalty.getStatus())) {
            throw BusinessException.badRequest("该罚款已缴纳，请勿重复操作");
        }
        penalty.setStatus(PenaltyStatus.PAID);
        penalty.setPaidAt(LocalDateTime.now());
        penalty.setPaidBy(principal.userId());
        penalty.setPaymentChannel(principal.isAdmin() ? "CASH" : "ONLINE");
        penaltyMapper.updateById(penalty);

        String message = principal.isAdmin() ? "已确认缴纳，罚款状态已更新" : "缴纳成功，感谢您及时处理";
        return OperationResult.of(viewService.toPenaltyVO(penalty), message);
    }

    /**
     * 读者未缴罚款（供仪表盘使用）。
     */
    public List<Penalty> unpaidOfReader(Long readerId) {
        return penaltyMapper.selectList(Wrappers.<Penalty>lambdaQuery()
                .eq(Penalty::getReaderId, readerId)
                .eq(Penalty::getStatus, PenaltyStatus.UNPAID)
                .orderByAsc(Penalty::getId));
    }

    private List<Penalty> penaltiesFor(UserPrincipal principal) {
        if (principal.isAdmin()) {
            return penaltyMapper.selectList(Wrappers.<Penalty>lambdaQuery().orderByAsc(Penalty::getId));
        }
        if (principal.readerId() == null) {
            return List.of();
        }
        return penaltiesOfReader(principal.readerId());
    }

    private List<Penalty> penaltiesOfReader(Long readerId) {
        return penaltyMapper.selectList(Wrappers.<Penalty>lambdaQuery()
                .eq(Penalty::getReaderId, readerId)
                .orderByAsc(Penalty::getId));
    }

    private PenaltySummaryVO buildSummary(List<Penalty> penalties) {
        List<Penalty> unpaid = penalties.stream()
                .filter(penalty -> PenaltyStatus.UNPAID.equals(penalty.getStatus()))
                .toList();
        YearMonth currentMonth = YearMonth.now();
        List<Penalty> paidThisMonth = penalties.stream()
                .filter(penalty -> PenaltyStatus.PAID.equals(penalty.getStatus()))
                .filter(penalty -> penalty.getPaidAt() != null
                        && YearMonth.from(penalty.getPaidAt()).equals(currentMonth))
                .toList();
        List<Penalty> paid = penalties.stream()
                .filter(penalty -> PenaltyStatus.PAID.equals(penalty.getStatus()))
                .toList();

        PenaltySummaryVO summary = new PenaltySummaryVO();
        summary.setPendingCount((long) unpaid.size());
        summary.setPendingAmount(ViewService.money(sum(unpaid)));
        summary.setPaidThisMonth(ViewService.money(sum(paidThisMonth)));
        summary.setPaidThisMonthCount((long) paidThisMonth.size());
        summary.setCollectedTotal(ViewService.money(sum(paid)));
        summary.setRate(ViewService.money(penaltyProperties.getRatePerDay()));
        return summary;
    }

    private BigDecimal sum(List<Penalty> penalties) {
        return penalties.stream()
                .map(Penalty::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Long requireReaderId(UserPrincipal principal) {
        if (principal.readerId() == null) {
            throw BusinessException.badRequest("当前账号不是读者账号");
        }
        return principal.readerId();
    }

    private void requireReaderOrAdmin(UserPrincipal principal, Long readerId) {
        if (principal.isAdmin()) {
            return;
        }
        if (principal.readerId() == null || !principal.readerId().equals(readerId)) {
            throw BusinessException.forbidden("无权访问其他读者的数据");
        }
    }
}

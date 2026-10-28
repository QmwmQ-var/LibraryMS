package com.university.library.service;

import com.university.library.common.BusinessException;
import com.university.library.common.OperationResult;
import com.university.library.common.config.PenaltyProperties;
import com.university.library.common.security.UserPrincipal;
import com.university.library.entity.Penalty;
import com.university.library.mapper.PenaltyMapper;
import com.university.library.service.support.ViewService;
import com.university.library.vo.PenaltySummaryVO;
import com.university.library.vo.PenaltyVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 罚款缴纳规则单元测试。
 */
class PenaltyServiceTest {

    private PenaltyMapper penaltyMapper;
    private ViewService viewService;
    private PenaltyService penaltyService;

    @BeforeEach
    void setUp() {
        penaltyMapper = mock(PenaltyMapper.class);
        viewService = mock(ViewService.class);
        PenaltyProperties properties = new PenaltyProperties();
        properties.setRatePerDay(new BigDecimal("0.50"));
        penaltyService = new PenaltyService(penaltyMapper, viewService, properties);

        when(viewService.toPenaltyVO(any(Penalty.class))).thenAnswer(invocation -> {
            Penalty penalty = invocation.getArgument(0);
            PenaltyVO vo = new PenaltyVO();
            vo.setId(penalty.getId());
            vo.setStatus(penalty.getStatus());
            vo.setPaymentChannel(penalty.getPaymentChannel());
            vo.setPaidAt(penalty.getPaidAt());
            return vo;
        });
    }

    @Test
    @DisplayName("管理员确认缴纳：状态改为 PAID，渠道为 CASH")
    void adminPayPenalty() {
        Penalty penalty = penalty(1L, 1L, "UNPAID");
        when(penaltyMapper.selectByIdForUpdate(1L)).thenReturn(penalty);

        OperationResult<PenaltyVO> result = penaltyService.pay(
                new UserPrincipal(1L, "admin", "ADMIN", null), 1L);

        assertThat(result.data().getStatus()).isEqualTo("PAID");
        assertThat(result.data().getPaymentChannel()).isEqualTo("CASH");
        assertThat(result.message()).contains("已确认缴纳");
        assertThat(penalty.getPaidAt()).isNotNull();
        assertThat(penalty.getPaidBy()).isEqualTo(1L);
        verify(penaltyMapper).updateById(penalty);
    }

    @Test
    @DisplayName("读者自助缴纳：渠道为 ONLINE")
    void readerPayPenalty() {
        Penalty penalty = penalty(1L, 1L, "UNPAID");
        when(penaltyMapper.selectByIdForUpdate(1L)).thenReturn(penalty);

        OperationResult<PenaltyVO> result = penaltyService.pay(
                new UserPrincipal(2L, "2023001", "READER", 1L), 1L);

        assertThat(result.data().getPaymentChannel()).isEqualTo("ONLINE");
        assertThat(result.message()).contains("缴纳成功");
    }

    @Test
    @DisplayName("重复缴纳：返回 400")
    void payRejectedWhenAlreadyPaid() {
        when(penaltyMapper.selectByIdForUpdate(1L)).thenReturn(penalty(1L, 1L, "PAID"));

        BusinessException ex = catchThrowableOfType(
                () -> penaltyService.pay(new UserPrincipal(1L, "admin", "ADMIN", null), 1L),
                BusinessException.class);

        assertThat(ex.getCode()).isEqualTo(400);
        assertThat(ex.getMessage()).contains("已缴纳");
        verify(penaltyMapper, never()).updateById(any(Penalty.class));
    }

    @Test
    @DisplayName("越权缴纳：读者不能缴纳他人的罚款")
    void payRejectedWhenNotOwner() {
        when(penaltyMapper.selectByIdForUpdate(1L)).thenReturn(penalty(1L, 2L, "UNPAID"));

        BusinessException ex = catchThrowableOfType(
                () -> penaltyService.pay(new UserPrincipal(3L, "2023002", "READER", 1L), 1L),
                BusinessException.class);

        assertThat(ex.getCode()).isEqualTo(403);
        verify(penaltyMapper, never()).updateById(any(Penalty.class));
    }

    @Test
    @DisplayName("罚款汇总：待缴金额与本月已缴金额计算正确")
    void penaltySummary() {
        Penalty unpaid = penalty(1L, 1L, "UNPAID");
        unpaid.setAmount(new BigDecimal("2.50"));
        Penalty paidThisMonth = penalty(2L, 1L, "PAID");
        paidThisMonth.setAmount(new BigDecimal("1.50"));
        paidThisMonth.setPaidAt(LocalDateTime.now());
        when(penaltyMapper.selectList(any())).thenReturn(java.util.List.of(unpaid, paidThisMonth));

        PenaltySummaryVO summary = penaltyService.summary(new UserPrincipal(1L, "admin", "ADMIN", null));

        assertThat(summary.getPendingCount()).isEqualTo(1);
        assertThat(summary.getPendingAmount()).isEqualByComparingTo("2.50");
        assertThat(summary.getPaidThisMonth()).isEqualByComparingTo("1.50");
        assertThat(summary.getCollectedTotal()).isEqualByComparingTo("1.50");
        assertThat(summary.getRate()).isEqualByComparingTo("0.50");
    }

    private Penalty penalty(Long id, Long readerId, String status) {
        Penalty penalty = new Penalty();
        penalty.setId(id);
        penalty.setReaderId(readerId);
        penalty.setRecordId(100L + id);
        penalty.setBookId(10L);
        penalty.setAmount(new BigDecimal("2.50"));
        penalty.setOverdueDays(5);
        penalty.setRuleRate(new BigDecimal("0.50"));
        penalty.setStatus(status);
        return penalty;
    }
}

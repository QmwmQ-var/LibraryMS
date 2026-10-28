package com.university.library.service;

import com.university.library.mapper.BorrowRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 逾期状态刷新服务。
 *
 * <p>借阅状态显式落库：每次读取借阅相关数据前，将 due_date 已过且状态仍为
 * BORROWED 的记录更新为 OVERDUE；同时每天凌晨兜底执行一次。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OverdueService {

    private final BorrowRecordMapper borrowRecordMapper;

    @Transactional
    public int refresh() {
        return borrowRecordMapper.markOverdue();
    }

    /**
     * 每日 00:05 兜底刷新，保证即使无人访问，逾期状态也能及时落库。
     */
    @Scheduled(cron = "0 5 0 * * ?")
    public void scheduledRefresh() {
        int changed = refresh();
        if (changed > 0) {
            log.info("定时任务：{} 条借阅记录已标记为逾期", changed);
        }
    }
}

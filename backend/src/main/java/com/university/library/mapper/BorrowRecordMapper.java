package com.university.library.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.university.library.entity.BorrowRecord;
import com.university.library.mapper.model.BookBorrowCount;
import com.university.library.mapper.model.ReaderBorrowCount;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 借阅记录 Mapper。
 */
public interface BorrowRecordMapper extends BaseMapper<BorrowRecord> {

    /**
     * 还书/续借时锁定记录行，保证幂等与状态流转的原子性。
     */
    @Select("SELECT * FROM borrow_record WHERE id = #{id} FOR UPDATE")
    BorrowRecord selectByIdForUpdate(@Param("id") Long id);

    /**
     * 将已过应还日期且仍为 BORROWED 的记录标记为 OVERDUE。
     */
    @Update("""
            UPDATE borrow_record
               SET status = 'OVERDUE',
                   updated_at = NOW()
             WHERE status = 'BORROWED'
               AND due_date < CURRENT_DATE
            """)
    int markOverdue();

    /**
     * 按图书统计在借/总借阅次数，供图书列表、排行使用。
     */
    @Select("""
            SELECT book_id AS bookId,
                   SUM(CASE WHEN status IN ('BORROWED', 'OVERDUE') THEN 1 ELSE 0 END) AS activeCount,
                   COUNT(*) AS totalCount
              FROM borrow_record
             GROUP BY book_id
            """)
    List<BookBorrowCount> countByBook();

    /**
     * 按读者统计在借/逾期/总借阅次数，供读者列表使用。
     */
    @Select("""
            SELECT reader_id AS readerId,
                   SUM(CASE WHEN status IN ('BORROWED', 'OVERDUE') THEN 1 ELSE 0 END) AS activeCount,
                   SUM(CASE WHEN status = 'OVERDUE' THEN 1 ELSE 0 END) AS overdueCount,
                   COUNT(*) AS totalCount
              FROM borrow_record
             GROUP BY reader_id
            """)
    List<ReaderBorrowCount> countByReader();
}

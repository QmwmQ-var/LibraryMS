package com.university.library.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.university.library.entity.Reservation;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 预约 Mapper。
 */
public interface ReservationMapper extends BaseMapper<Reservation> {

    /**
     * 取消/履约/过期时锁定预约行，防止重复处理。
     */
    @Select("SELECT * FROM reservation WHERE id = #{id} FOR UPDATE")
    Reservation selectByIdForUpdate(@Param("id") Long id);

    /**
     * 取某本书最早的一条待排队预约（到书时提升为 READY）。
     */
    @Select("""
            SELECT * FROM reservation
             WHERE book_id = #{bookId}
               AND status = 'PENDING'
             ORDER BY requested_at ASC, id ASC
             LIMIT 1
             FOR UPDATE
            """)
    Reservation selectOldestPendingForUpdate(@Param("bookId") Long bookId);

    /**
     * 取某读者对某本书的有效预约，用于借书时识别“到书留书”。
     */
    @Select("""
            SELECT * FROM reservation
             WHERE reader_id = #{readerId}
               AND book_id = #{bookId}
               AND status = 'READY'
             ORDER BY id ASC
             LIMIT 1
            """)
    Reservation selectReady(@Param("readerId") Long readerId, @Param("bookId") Long bookId);
}

package com.university.library.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.university.library.entity.Book;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 图书 Mapper。
 */
public interface BookMapper extends BaseMapper<Book> {

    /**
     * 原子扣减可借数量：只有仍有可借副本时才会更新成功，防止并发超借。
     */
    @Update("""
            UPDATE book
               SET available_copies = available_copies - 1,
                   updated_at = NOW()
             WHERE id = #{id}
               AND deleted = 0
               AND available_copies > 0
            """)
    int decrementAvailable(@Param("id") Long id);

    /**
     * 原子归还库存：不超过馆藏总量。
     */
    @Update("""
            UPDATE book
               SET available_copies = available_copies + 1,
                   updated_at = NOW()
             WHERE id = #{id}
               AND deleted = 0
               AND available_copies < total_copies
            """)
    int incrementAvailable(@Param("id") Long id);

    /**
     * 原子调整有效预约数，保证不会出现负数。
     */
    @Update("""
            UPDATE book
               SET reservation_count = GREATEST(0, reservation_count + #{delta}),
                   updated_at = NOW()
             WHERE id = #{id}
               AND deleted = 0
            """)
    int adjustReservationCount(@Param("id") Long id, @Param("delta") int delta);
}

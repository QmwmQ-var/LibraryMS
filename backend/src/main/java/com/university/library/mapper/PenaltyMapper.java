package com.university.library.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.university.library.entity.Penalty;
import com.university.library.mapper.model.ReaderUnpaidSum;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 罚款 Mapper。
 */
public interface PenaltyMapper extends BaseMapper<Penalty> {

    /**
     * 缴纳罚款时锁定行，防止并发重复缴纳。
     */
    @Select("SELECT * FROM penalty WHERE id = #{id} FOR UPDATE")
    Penalty selectByIdForUpdate(@Param("id") Long id);

    /**
     * 按读者汇总未缴罚款金额。
     */
    @Select("""
            SELECT reader_id AS readerId,
                   COALESCE(SUM(amount), 0) AS unpaidAmount
              FROM penalty
             WHERE status = 'UNPAID'
             GROUP BY reader_id
            """)
    List<ReaderUnpaidSum> sumUnpaidByReader();
}

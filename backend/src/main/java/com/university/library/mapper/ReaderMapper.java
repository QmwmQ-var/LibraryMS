package com.university.library.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.university.library.entity.Reader;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 读者 Mapper。
 */
public interface ReaderMapper extends BaseMapper<Reader> {

    /**
     * 借书时锁定读者行，串行化同一读者的并发借阅，避免超出借阅上限。
     */
    @Select("SELECT * FROM reader WHERE id = #{id} AND deleted = 0 FOR UPDATE")
    Reader selectByIdForUpdate(@Param("id") Long id);
}

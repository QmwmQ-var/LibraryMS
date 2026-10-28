package com.university.library.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 借阅排行响应。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RankingVO {

    private List<BookRankVO> books;

    private List<ReaderRankVO> readers;
}

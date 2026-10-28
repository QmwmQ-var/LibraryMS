package com.university.library.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.university.library.common.BusinessException;
import com.university.library.common.OperationResult;
import com.university.library.common.config.PenaltyProperties;
import com.university.library.common.constant.BorrowStatus;
import com.university.library.common.security.UserPrincipal;
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
import com.university.library.vo.BorrowRecordVO;
import com.university.library.vo.BorrowResultVO;
import com.university.library.vo.PenaltyVO;
import com.university.library.vo.ReaderVO;
import com.university.library.vo.ReturnResultVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 借阅核心业务单元测试：借书规则、还书罚款、续借规则与幂等。
 */
class BorrowServiceTest {

    private BorrowRecordMapper borrowRecordMapper;
    private BookMapper bookMapper;
    private ReaderMapper readerMapper;
    private PenaltyMapper penaltyMapper;
    private ViewService viewService;
    private OverdueService overdueService;
    private BorrowService borrowService;

    private final UserPrincipal admin = new UserPrincipal(1L, "admin", "ADMIN", null);
    private final UserPrincipal readerPrincipal = new UserPrincipal(2L, "2023001", "READER", 1L);

    @BeforeEach
    void setUp() {
        borrowRecordMapper = mock(BorrowRecordMapper.class);
        bookMapper = mock(BookMapper.class);
        readerMapper = mock(ReaderMapper.class);
        penaltyMapper = mock(PenaltyMapper.class);
        viewService = mock(ViewService.class);
        overdueService = mock(OverdueService.class);

        PenaltyProperties penaltyProperties = new PenaltyProperties();
        penaltyProperties.setRatePerDay(new BigDecimal("0.50"));

        borrowService = new BorrowService(borrowRecordMapper, bookMapper, readerMapper, penaltyMapper,
                viewService, overdueService, penaltyProperties);

        when(viewService.toRecordVO(any(BorrowRecord.class))).thenAnswer(invocation -> {
            BorrowRecord record = invocation.getArgument(0);
            BorrowRecordVO vo = new BorrowRecordVO();
            vo.setId(record.getId());
            vo.setBookTitle(record.getBookTitle());
            vo.setStatus(record.getStatus());
            vo.setBorrowDate(record.getBorrowDate());
            vo.setDueDate(record.getDueDate());
            vo.setReturnDate(record.getReturnDate());
            return vo;
        });
        when(viewService.toReaderVO(any(Reader.class))).thenReturn(new ReaderVO());
        when(viewService.toPenaltyVO(any(Penalty.class))).thenAnswer(invocation -> {
            Penalty penalty = invocation.getArgument(0);
            PenaltyVO vo = new PenaltyVO();
            vo.setAmount(penalty.getAmount());
            vo.setStatus(penalty.getStatus());
            return vo;
        });
        when(viewService.toBookVO(any(Book.class))).thenReturn(new BookVO());
        when(borrowRecordMapper.insert(any(BorrowRecord.class))).thenAnswer(invocation -> {
            BorrowRecord record = invocation.getArgument(0);
            record.setId(100L);
            return 1;
        });
    }

    @Test
    @DisplayName("借书成功：生成 BORROWED 记录、应还日期 = 今天 + 借期，并扣减库存")
    void borrowSuccess() {
        Reader reader = reader(1L, "UNDERGRADUATE", "ACTIVE");
        Book book = book(10L, 2);
        when(readerMapper.selectByIdForUpdate(1L)).thenReturn(reader);
        when(bookMapper.selectById(10L)).thenReturn(book);
        when(borrowRecordMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(bookMapper.decrementAvailable(10L)).thenReturn(1);

        BorrowRequest request = new BorrowRequest();
        request.setBookId(10L);

        OperationResult<BorrowResultVO> result = borrowService.borrow(readerPrincipal, request);

        assertThat(result.data().getRecord().getStatus()).isEqualTo(BorrowStatus.BORROWED);
        ArgumentCaptor<BorrowRecord> captor = ArgumentCaptor.forClass(BorrowRecord.class);
        verify(borrowRecordMapper).insert(captor.capture());
        BorrowRecord inserted = captor.getValue();
        assertThat(inserted.getBorrowDate()).isEqualTo(LocalDate.now());
        assertThat(inserted.getDueDate()).isEqualTo(LocalDate.now().plusDays(30));
        assertThat(inserted.getReaderId()).isEqualTo(1L);
        assertThat(inserted.getBookId()).isEqualTo(10L);
        assertThat(inserted.getStatus()).isEqualTo(BorrowStatus.BORROWED);
        assertThat(inserted.getRenewCount()).isZero();
        assertThat(inserted.getReaderName()).isEqualTo("林知夏");
        verify(bookMapper).decrementAvailable(10L);
    }

    @Test
    @DisplayName("借书失败：借阅证挂失")
    void borrowRejectedWhenCardLost() {
        Reader reader = reader(1L, "UNDERGRADUATE", "LOST");
        when(readerMapper.selectByIdForUpdate(1L)).thenReturn(reader);

        BorrowRequest request = new BorrowRequest();
        request.setBookId(10L);

        BusinessException ex = catchThrowableOfType(
                () -> borrowService.borrow(readerPrincipal, request), BusinessException.class);

        assertThat(ex.getCode()).isEqualTo(400);
        assertThat(ex.getMessage()).contains("借阅证已挂失");
        verify(bookMapper, never()).decrementAvailable(anyLong());
    }

    @Test
    @DisplayName("借书失败：达到借阅上限")
    void borrowRejectedWhenReachLimit() {
        Reader reader = reader(1L, "UNDERGRADUATE", "ACTIVE");
        when(readerMapper.selectByIdForUpdate(1L)).thenReturn(reader);
        when(bookMapper.selectById(10L)).thenReturn(book(10L, 5));
        when(borrowRecordMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                activeRecord(1L, 1L), activeRecord(2L, 2L), activeRecord(3L, 3L),
                activeRecord(4L, 4L), activeRecord(5L, 5L)));

        BorrowRequest request = new BorrowRequest();
        request.setBookId(10L);

        BusinessException ex = catchThrowableOfType(
                () -> borrowService.borrow(readerPrincipal, request), BusinessException.class);

        assertThat(ex.getCode()).isEqualTo(400);
        assertThat(ex.getMessage()).contains("已达借阅上限（5 本）");
        verify(bookMapper, never()).decrementAvailable(anyLong());
    }

    @Test
    @DisplayName("借书失败：同一读者重复借同一本书")
    void borrowRejectedWhenDuplicate() {
        Reader reader = reader(1L, "UNDERGRADUATE", "ACTIVE");
        when(readerMapper.selectByIdForUpdate(1L)).thenReturn(reader);
        when(bookMapper.selectById(10L)).thenReturn(book(10L, 5));
        when(borrowRecordMapper.selectList(any(Wrapper.class))).thenReturn(List.of(activeRecord(1L, 10L)));

        BorrowRequest request = new BorrowRequest();
        request.setBookId(10L);

        BusinessException ex = catchThrowableOfType(
                () -> borrowService.borrow(readerPrincipal, request), BusinessException.class);

        assertThat(ex.getCode()).isEqualTo(400);
        assertThat(ex.getMessage()).contains("请勿重复借阅");
        verify(bookMapper, never()).decrementAvailable(anyLong());
    }

    @Test
    @DisplayName("还书：逾期 5 天生成 2.50 元罚款，库存恢复，状态改为 RETURNED")
    void returnBookGeneratesPenalty() {
        BorrowRecord record = new BorrowRecord();
        record.setId(1L);
        record.setReaderId(1L);
        record.setBookId(10L);
        record.setBorrowDate(LocalDate.now().minusDays(35));
        record.setDueDate(LocalDate.now().minusDays(5));
        record.setStatus(BorrowStatus.BORROWED);
        record.setRenewCount(0);
        record.setBookTitle("三体");
        when(borrowRecordMapper.selectByIdForUpdate(1L)).thenReturn(record);
        when(penaltyMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(penaltyMapper.insert(any(Penalty.class))).thenAnswer(invocation -> {
            Penalty penalty = invocation.getArgument(0);
            penalty.setId(9L);
            return 1;
        });
        when(bookMapper.selectById(10L)).thenReturn(book(10L, 2));

        OperationResult<ReturnResultVO> result = borrowService.returnBook(admin, 1L);

        assertThat(result.data().getRecord().getStatus()).isEqualTo(BorrowStatus.RETURNED);
        assertThat(record.getReturnDate()).isEqualTo(LocalDate.now());
        assertThat(result.data().getPenalty().getAmount()).isEqualByComparingTo("2.50");
        assertThat(result.message()).contains("已逾期 5 天").contains("¥2.50");
        verify(bookMapper).incrementAvailable(10L);
        ArgumentCaptor<Penalty> captor = ArgumentCaptor.forClass(Penalty.class);
        verify(penaltyMapper).insert(captor.capture());
        assertThat(captor.getValue().getOverdueDays()).isEqualTo(5);
        assertThat(captor.getValue().getStatus()).isEqualTo("UNPAID");
        assertThat(captor.getValue().getRuleRate()).isEqualByComparingTo("0.50");
    }

    @Test
    @DisplayName("还书幂等：已归还记录再次还书返回 400，且不重复生成罚款")
    void returnBookIsIdempotent() {
        BorrowRecord record = new BorrowRecord();
        record.setId(1L);
        record.setReaderId(1L);
        record.setBookId(10L);
        record.setStatus(BorrowStatus.RETURNED);
        record.setReturnDate(LocalDate.now().minusDays(1));
        when(borrowRecordMapper.selectByIdForUpdate(1L)).thenReturn(record);

        BusinessException ex = catchThrowableOfType(
                () -> borrowService.returnBook(admin, 1L), BusinessException.class);

        assertThat(ex.getCode()).isEqualTo(400);
        assertThat(ex.getMessage()).contains("已归还");
        verify(penaltyMapper, never()).insert(any(Penalty.class));
        verify(bookMapper, never()).incrementAvailable(anyLong());
    }

    @Test
    @DisplayName("续借成功：应还日期延长一个借阅周期，续借次数 +1")
    void renewSuccess() {
        BorrowRecord record = activeRecord(1L, 10L);
        record.setDueDate(LocalDate.now().plusDays(5));
        when(borrowRecordMapper.selectByIdForUpdate(1L)).thenReturn(record);
        Book book = book(10L, 1);
        book.setReservationCount(0);
        when(bookMapper.selectById(10L)).thenReturn(book);
        Reader reader = reader(1L, "GRADUATE", "ACTIVE");
        when(readerMapper.selectById(1L)).thenReturn(reader);

        OperationResult<BorrowRecordVO> result = borrowService.renew(admin, 1L);

        assertThat(record.getDueDate()).isEqualTo(LocalDate.now().plusDays(65));
        assertThat(record.getRenewCount()).isEqualTo(1);
        assertThat(result.message()).contains("续借成功");
        verify(borrowRecordMapper).updateById(record);
    }

    @Test
    @DisplayName("续借失败：已逾期")
    void renewRejectedWhenOverdue() {
        BorrowRecord record = activeRecord(1L, 10L);
        record.setStatus(BorrowStatus.OVERDUE);
        record.setDueDate(LocalDate.now().minusDays(2));
        when(borrowRecordMapper.selectByIdForUpdate(1L)).thenReturn(record);

        BusinessException ex = catchThrowableOfType(
                () -> borrowService.renew(admin, 1L), BusinessException.class);

        assertThat(ex.getCode()).isEqualTo(400);
        assertThat(ex.getMessage()).contains("逾期");
    }

    @Test
    @DisplayName("续借失败：已达到最大续借次数")
    void renewRejectedWhenLimitReached() {
        BorrowRecord record = activeRecord(1L, 10L);
        record.setRenewCount(1);
        when(borrowRecordMapper.selectByIdForUpdate(1L)).thenReturn(record);

        BusinessException ex = catchThrowableOfType(
                () -> borrowService.renew(admin, 1L), BusinessException.class);

        assertThat(ex.getCode()).isEqualTo(400);
        assertThat(ex.getMessage()).contains("最多续借 1 次");
    }

    @Test
    @DisplayName("权限：读者不能操作他人的借阅记录")
    void readerCannotOperateOthersRecord() {
        BorrowRecord record = activeRecord(1L, 10L);
        when(borrowRecordMapper.selectByIdForUpdate(1L)).thenReturn(record);

        UserPrincipal otherReader = new UserPrincipal(3L, "2023002", "READER", 2L);
        BusinessException ex = catchThrowableOfType(
                () -> borrowService.renew(otherReader, 1L), BusinessException.class);

        assertThat(ex.getCode()).isEqualTo(403);
        assertThat(ex.getMessage()).contains("无权操作");
    }

    private Reader reader(Long id, String type, String cardStatus) {
        Reader reader = new Reader();
        reader.setId(id);
        reader.setName("林知夏");
        reader.setStudentNo("2023001");
        reader.setType(type);
        reader.setCardStatus(cardStatus);
        return reader;
    }

    private Book book(Long id, int availableCopies) {
        Book book = new Book();
        book.setId(id);
        book.setTitle("三体");
        book.setIsbn("9787536692930");
        book.setTotalCopies(6);
        book.setAvailableCopies(availableCopies);
        book.setReservationCount(0);
        return book;
    }

    private BorrowRecord activeRecord(Long id, Long bookId) {
        BorrowRecord record = new BorrowRecord();
        record.setId(id);
        record.setReaderId(1L);
        record.setBookId(bookId);
        record.setBorrowDate(LocalDate.now().minusDays(10));
        record.setDueDate(LocalDate.now().plusDays(20));
        record.setStatus(BorrowStatus.BORROWED);
        record.setRenewCount(0);
        return record;
    }
}

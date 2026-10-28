package com.university.library.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.university.library.common.BusinessException;
import com.university.library.common.OperationResult;
import com.university.library.common.PageResult;
import com.university.library.common.constant.BorrowStatus;
import com.university.library.common.constant.CardStatus;
import com.university.library.common.constant.ReaderType;
import com.university.library.common.constant.RoleConstants;
import com.university.library.common.constant.UserStatus;
import com.university.library.common.security.UserPrincipal;
import com.university.library.common.util.PageUtils;
import com.university.library.common.util.TextUtils;
import com.university.library.dto.CardStatusRequest;
import com.university.library.dto.ReaderRequest;
import com.university.library.entity.BorrowRecord;
import com.university.library.entity.Penalty;
import com.university.library.entity.Reader;
import com.university.library.entity.User;
import com.university.library.mapper.BorrowRecordMapper;
import com.university.library.mapper.PenaltyMapper;
import com.university.library.mapper.ReaderMapper;
import com.university.library.mapper.UserMapper;
import com.university.library.service.support.ViewService;
import com.university.library.vo.BorrowRecordVO;
import com.university.library.vo.PenaltyVO;
import com.university.library.vo.ReaderDetailVO;
import com.university.library.vo.ReaderSummaryVO;
import com.university.library.vo.ReaderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * 读者服务：读者 CRUD、借阅证状态、读者自助信息、列表筛选与汇总。
 */
@Service
@RequiredArgsConstructor
public class ReaderService {

    private static final String DEFAULT_PASSWORD = "reader123";

    private final ReaderMapper readerMapper;
    private final UserMapper userMapper;
    private final BorrowRecordMapper borrowRecordMapper;
    private final PenaltyMapper penaltyMapper;
    private final PasswordEncoder passwordEncoder;
    private final ViewService viewService;
    private final OverdueService overdueService;

    /**
     * 读者列表（管理员）：支持类型、借阅证状态、是否有逾期、关键字筛选。
     */
    public PageResult<ReaderVO> list(String keyword, String type, String cardStatus, Boolean hasOverdue,
                                     long page, long size) {
        overdueService.refresh();
        List<Reader> readers = readerMapper.selectList(
                Wrappers.<Reader>lambdaQuery().orderByDesc(Reader::getId));
        List<ReaderVO> all = viewService.toReaderVOs(readers);
        ReaderSummaryVO summary = buildSummary(all);

        List<ReaderVO> filtered = new ArrayList<>();
        for (ReaderVO reader : all) {
            if (!TextUtils.isBlank(type) && !type.equals(reader.getType())) {
                continue;
            }
            if (!TextUtils.isBlank(cardStatus) && !cardStatus.equals(reader.getCardStatus())) {
                continue;
            }
            if (Boolean.TRUE.equals(hasOverdue) && reader.getOverdueCount() != null && reader.getOverdueCount() == 0) {
                continue;
            }
            if (!matchesKeyword(reader, keyword)) {
                continue;
            }
            filtered.add(reader);
        }

        long currentPage = PageUtils.normalizePage(page);
        long pageSize = PageUtils.normalizeSize(size);
        List<ReaderVO> records = PageUtils.slice(filtered, currentPage, pageSize);
        return PageResult.withSummary(records, filtered.size(), currentPage, pageSize, summary);
    }

    /**
     * 读者详情：管理员可查看全部；读者只能查看自己。
     */
    public ReaderDetailVO detail(UserPrincipal principal, Long id) {
        overdueService.refresh();
        Reader reader = requireReader(id);
        requireSelfOrAdmin(principal, reader.getId());
        ReaderVO readerVO = viewService.toReaderVO(reader);

        List<BorrowRecord> records = borrowRecordMapper.selectList(
                Wrappers.<BorrowRecord>lambdaQuery()
                        .eq(BorrowRecord::getReaderId, reader.getId())
                        .orderByAsc(BorrowRecord::getId));
        List<BorrowRecordVO> recordVOs = viewService.toRecordVOs(records);
        List<BorrowRecordVO> active = recordVOs.stream()
                .filter(record -> BorrowStatus.isActive(record.getStatus()))
                .toList();
        List<BorrowRecordVO> history = recordVOs.stream()
                .sorted(Comparator.comparing(BorrowRecordVO::getBorrowDate,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(BorrowRecordVO::getId, Comparator.reverseOrder()))
                .limit(6)
                .toList();
        List<Penalty> penalties = penaltyMapper.selectList(
                Wrappers.<Penalty>lambdaQuery()
                        .eq(Penalty::getReaderId, reader.getId())
                        .orderByAsc(Penalty::getId));
        List<PenaltyVO> penaltyVOs = viewService.toPenaltyVOs(penalties);

        ReaderDetailVO detail = new ReaderDetailVO();
        BeanUtils.copyProperties(readerVO, detail);
        detail.setActiveBorrows(active);
        detail.setRecentHistory(history);
        detail.setPenalties(penaltyVOs);
        return detail;
    }

    /**
     * 读者自助借阅证信息。
     */
    public ReaderVO me(UserPrincipal principal) {
        if (principal.readerId() == null) {
            throw BusinessException.badRequest("当前账号不是读者账号");
        }
        Reader reader = readerMapper.selectById(principal.readerId());
        if (reader == null) {
            throw BusinessException.notFound("借阅信息不存在");
        }
        return viewService.toReaderVO(reader);
    }

    /**
     * 新增读者：同时创建登录账号（初始密码 reader123，BCrypt 存储）。
     */
    @Transactional
    public OperationResult<ReaderVO> create(ReaderRequest request) {
        String studentNo = TextUtils.trimToEmpty(request.getStudentNo());
        String name = TextUtils.trimToEmpty(request.getName());
        validate(studentNo, name, request.getType(), request.getCardStatus());

        Long duplicateReader = readerMapper.selectCount(
                Wrappers.<Reader>lambdaQuery().eq(Reader::getStudentNo, studentNo));
        if (duplicateReader != null && duplicateReader > 0) {
            throw BusinessException.conflict("学号已存在，请检查");
        }
        Long duplicateUser = userMapper.selectCount(
                Wrappers.<User>lambdaQuery().eq(User::getUsername, studentNo));
        if (duplicateUser != null && duplicateUser > 0) {
            throw BusinessException.conflict("该学号已绑定登录账号");
        }

        ReaderType readerType = ReaderType.from(request.getType());
        User user = new User();
        user.setUsername(studentNo);
        user.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        user.setName(name);
        user.setRole(RoleConstants.READER);
        user.setTitle(readerType.getLabel());
        user.setEmail(TextUtils.trimToEmpty(request.getEmail()));
        user.setPhone(TextUtils.trimToEmpty(request.getPhone()));
        user.setAvatarColor("#527b70");
        user.setStatus(UserStatus.ACTIVE);
        user.setFailedAttempts(0);
        userMapper.insert(user);

        Reader reader = new Reader();
        reader.setUserId(user.getId());
        reader.setStudentNo(studentNo);
        reader.setName(name);
        reader.setGender(TextUtils.defaultIfBlank(request.getGender(), "未设置"));
        reader.setType(request.getType());
        reader.setCardStatus(request.getCardStatus());
        reader.setDepartment(TextUtils.trimToEmpty(request.getDepartment()));
        reader.setMajor(TextUtils.trimToEmpty(request.getMajor()));
        reader.setEmail(TextUtils.trimToEmpty(request.getEmail()));
        reader.setPhone(TextUtils.trimToEmpty(request.getPhone()));
        reader.setCardNo("LIB" + studentNo);
        reader.setCardIssuedAt(LocalDate.now());
        reader.setCardExpiresAt(LocalDate.now().plusYears(4));
        reader.setDeleted(0);
        readerMapper.insert(reader);

        return OperationResult.of(viewService.toReaderVO(reader), "读者新增成功，初始密码为 " + DEFAULT_PASSWORD);
    }

    /**
     * 修改读者：学号作为登录账号不可修改。
     */
    @Transactional
    public OperationResult<ReaderVO> update(Long id, ReaderRequest request) {
        Reader reader = requireReader(id);
        String studentNo = TextUtils.trimToEmpty(request.getStudentNo());
        String name = TextUtils.trimToEmpty(request.getName());
        validate(studentNo, name, request.getType(), request.getCardStatus());
        if (!Objects.equals(studentNo, reader.getStudentNo())) {
            throw BusinessException.badRequest("学号作为登录账号不可修改");
        }

        reader.setName(name);
        reader.setGender(TextUtils.defaultIfBlank(request.getGender(), "未设置"));
        reader.setType(request.getType());
        reader.setCardStatus(request.getCardStatus());
        reader.setDepartment(TextUtils.trimToEmpty(request.getDepartment()));
        reader.setMajor(TextUtils.trimToEmpty(request.getMajor()));
        reader.setEmail(TextUtils.trimToEmpty(request.getEmail()));
        reader.setPhone(TextUtils.trimToEmpty(request.getPhone()));
        readerMapper.updateById(reader);

        User user = userMapper.selectById(reader.getUserId());
        if (user != null) {
            user.setName(name);
            user.setEmail(reader.getEmail());
            user.setPhone(reader.getPhone());
            user.setTitle(ReaderType.from(reader.getType()).getLabel());
            userMapper.updateById(user);
        }
        return OperationResult.of(viewService.toReaderVO(reader), "读者信息已更新");
    }

    /**
     * 删除读者：有未归还图书时禁止删除；删除采用逻辑删除以保留历史借阅记录，
     * 同时停用其登录账号。
     */
    @Transactional
    public OperationResult<Void> delete(Long id) {
        Reader reader = requireReader(id);
        Long active = borrowRecordMapper.selectCount(Wrappers.<BorrowRecord>lambdaQuery()
                .eq(BorrowRecord::getReaderId, id)
                .in(BorrowRecord::getStatus, List.of(BorrowStatus.BORROWED, BorrowStatus.OVERDUE)));
        if (active != null && active > 0) {
            throw BusinessException.badRequest("该读者尚有 " + active + " 本图书未归还，无法删除");
        }
        readerMapper.deleteById(id);
        User user = userMapper.selectById(reader.getUserId());
        if (user != null) {
            user.setStatus(UserStatus.DISABLED);
            userMapper.updateById(user);
        }
        return OperationResult.ok("读者已删除，历史借阅记录已保留");
    }

    /**
     * 修改借阅证状态（挂失/注销后禁止借书）。
     */
    @Transactional
    public OperationResult<ReaderVO> updateCardStatus(Long id, CardStatusRequest request) {
        Reader reader = requireReader(id);
        if (!CardStatus.isValid(request.getCardStatus())) {
            throw BusinessException.badRequest("借阅证状态不合法");
        }
        reader.setCardStatus(request.getCardStatus());
        readerMapper.updateById(reader);
        return OperationResult.of(viewService.toReaderVO(reader), "借阅证状态已更新");
    }

    /**
     * 校验当前用户是否可访问指定读者数据：管理员放行，读者仅限本人。
     */
    public void requireSelfOrAdmin(UserPrincipal principal, Long readerId) {
        if (principal == null) {
            throw BusinessException.unauthorized("登录状态已失效，请重新登录");
        }
        if (principal.isAdmin()) {
            return;
        }
        if (principal.readerId() == null || !principal.readerId().equals(readerId)) {
            throw BusinessException.forbidden("无权访问其他读者的数据");
        }
    }

    public Reader requireReader(Long id) {
        Reader reader = readerMapper.selectById(id);
        if (reader == null) {
            throw BusinessException.notFound("读者不存在");
        }
        return reader;
    }

    private void validate(String studentNo, String name, String type, String cardStatus) {
        if (!studentNo.matches("\\d{6,12}")) {
            throw BusinessException.badRequest("学号应为 6–12 位数字");
        }
        if (name.isEmpty()) {
            throw BusinessException.badRequest("姓名不能为空");
        }
        if (!ReaderType.isValid(type)) {
            throw BusinessException.badRequest("请选择有效的读者类型");
        }
        if (!CardStatus.isValid(cardStatus)) {
            throw BusinessException.badRequest("请选择有效的借阅证状态");
        }
    }

    private boolean matchesKeyword(ReaderVO reader, String keyword) {
        return TextUtils.containsIgnoreCase(reader.getName(), keyword)
                || TextUtils.containsIgnoreCase(reader.getStudentNo(), keyword)
                || TextUtils.containsIgnoreCase(reader.getDepartment(), keyword)
                || TextUtils.containsIgnoreCase(reader.getMajor(), keyword)
                || TextUtils.containsIgnoreCase(reader.getPhone(), keyword);
    }

    private ReaderSummaryVO buildSummary(List<ReaderVO> readers) {
        ReaderSummaryVO summary = new ReaderSummaryVO();
        summary.setTotal((long) readers.size());
        summary.setActive(readers.stream().filter(r -> CardStatus.ACTIVE.equals(r.getCardStatus())).count());
        summary.setLost(readers.stream().filter(r -> CardStatus.LOST.equals(r.getCardStatus())).count());
        summary.setCancelled(readers.stream().filter(r -> CardStatus.CANCELLED.equals(r.getCardStatus())).count());
        summary.setUndergraduate(readers.stream().filter(r -> ReaderType.UNDERGRADUATE.name().equals(r.getType())).count());
        summary.setGraduate(readers.stream().filter(r -> ReaderType.GRADUATE.name().equals(r.getType())).count());
        summary.setTeacher(readers.stream().filter(r -> ReaderType.TEACHER.name().equals(r.getType())).count());
        summary.setActiveLoans(readers.stream()
                .map(ReaderVO::getActiveBorrowCount)
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .sum());
        summary.setOverdue(readers.stream()
                .map(ReaderVO::getOverdueCount)
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .sum());
        return summary;
    }
}

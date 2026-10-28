package com.university.library.service;

import com.university.library.common.BusinessException;
import com.university.library.common.OperationResult;
import com.university.library.common.ResultCode;
import com.university.library.common.config.LoginProperties;
import com.university.library.common.constant.UserStatus;
import com.university.library.common.security.JwtUtils;
import com.university.library.common.security.UserPrincipal;
import com.university.library.dto.ChangePasswordRequest;
import com.university.library.dto.LoginRequest;
import com.university.library.entity.LoginLog;
import com.university.library.entity.User;
import com.university.library.mapper.LoginLogMapper;
import com.university.library.mapper.UserMapper;
import com.university.library.service.support.ViewService;
import com.university.library.vo.LoginVO;
import com.university.library.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 认证服务：登录、登录失败锁定、登录日志、当前用户、修改密码。
 *
 * <p>锁定时长与失败次数由 application.yml 配置，使用 user.failed_attempts 与
 * user.locked_until 两个字段持久化，进程重启后仍然有效（无需 Redis）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final LoginLogMapper loginLogMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final LoginProperties loginProperties;
    private final ViewService viewService;

    /**
     * 登录。
     *
     * <p>此处使用 noRollbackFor：密码错误时先写入失败次数与登录日志，再抛出业务异常，
     * 异常不能被事务回滚，否则锁定计数会失效。</p>
     */
    @Transactional(noRollbackFor = BusinessException.class)
    public OperationResult<LoginVO> login(LoginRequest request, String ip) {
        String username = request.getUsername() == null ? "" : request.getUsername().trim();
        String password = request.getPassword() == null ? "" : request.getPassword();
        if (username.isEmpty() || password.isEmpty()) {
            throw BusinessException.badRequest("账号和密码不能为空");
        }

        User user = userMapper.selectByUsernameForUpdate(username);
        if (user == null) {
            writeLoginLog(null, username, null, ip, false);
            throw BusinessException.unauthorized("账号不存在，请检查后重试");
        }
        if (!UserStatus.ACTIVE.equals(user.getStatus())) {
            writeLoginLog(user, username, user.getRole(), ip, false);
            throw new BusinessException(ResultCode.FORBIDDEN, "该账号已停用，请联系图书馆管理员");
        }

        LocalDateTime now = LocalDateTime.now();
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) {
            long minutes = Math.max(1,
                    (long) Math.ceil(Duration.between(now, user.getLockedUntil()).toMillis() / 60000.0));
            throw new BusinessException(ResultCode.LOCKED, "账号已锁定，请在 " + minutes + " 分钟后再试");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            int attempts = (user.getFailedAttempts() == null ? 0 : user.getFailedAttempts()) + 1;
            int maxAttempts = loginProperties.getMaxFailAttempts();
            if (attempts >= maxAttempts) {
                user.setFailedAttempts(0);
                user.setLockedUntil(now.plusMinutes(loginProperties.getLockMinutes()));
                userMapper.updateById(user);
                writeLoginLog(user, username, user.getRole(), ip, false);
                throw new BusinessException(ResultCode.LOCKED,
                        "连续 " + maxAttempts + " 次密码错误，账号已锁定 " + loginProperties.getLockMinutes() + " 分钟");
            }
            user.setFailedAttempts(attempts);
            userMapper.updateById(user);
            writeLoginLog(user, username, user.getRole(), ip, false);
            throw new BusinessException(ResultCode.UNAUTHORIZED,
                    "密码错误，您还有 " + (maxAttempts - attempts) + " 次尝试机会");
        }

        // 登录成功：清空失败计数与锁定时间
        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        userMapper.updateById(user);
        writeLoginLog(user, username, user.getRole(), ip, true);

        String token = jwtUtils.generateToken(user);
        UserVO userVO = viewService.toUserVO(user);
        log.info("用户登录成功: username={}, role={}, ip={}", username, user.getRole(), ip);
        return OperationResult.of(new LoginVO(token, userVO), "登录成功");
    }

    /**
     * 当前登录用户信息（返回读者视图）。
     */
    public UserVO currentUser(UserPrincipal principal) {
        User user = userMapper.selectById(principal.userId());
        if (user == null) {
            throw BusinessException.unauthorized("登录状态已失效，请重新登录");
        }
        return viewService.toUserVO(user);
    }

    /**
     * 退出登录：JWT 无状态，服务端无需保存会话，前端删除本地 Token 即可。
     */
    public OperationResult<Void> logout() {
        return OperationResult.ok("已安全退出");
    }

    /**
     * 修改密码。
     */
    @Transactional
    public OperationResult<Void> changePassword(UserPrincipal principal, ChangePasswordRequest request) {
        User user = userMapper.selectById(principal.userId());
        if (user == null) {
            throw BusinessException.unauthorized("登录状态已失效，请重新登录");
        }
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw BusinessException.badRequest("原密码不正确");
        }
        int length = request.getNewPassword().length();
        if (length < 6 || length > 32) {
            throw BusinessException.badRequest("新密码长度需为 6–32 位");
        }
        if (request.getOldPassword().equals(request.getNewPassword())) {
            throw BusinessException.badRequest("新密码不能与原密码相同");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(user);
        return OperationResult.ok("密码修改成功，请保管好新密码");
    }

    private void writeLoginLog(User user, String username, String role, String ip, boolean success) {
        LoginLog loginLog = new LoginLog();
        loginLog.setUserId(user == null ? null : user.getId());
        loginLog.setUsername(username);
        loginLog.setRole(role);
        loginLog.setIp(ip);
        loginLog.setSuccess(success);
        loginLogMapper.insert(loginLog);
    }

    /**
     * 供安全过滤器/其他服务使用：按 ID 取有效用户。
     */
    public User requireUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.unauthorized("登录状态已失效，请重新登录");
        }
        return user;
    }
}

package com.university.library.service;

import com.university.library.common.BusinessException;
import com.university.library.common.OperationResult;
import com.university.library.common.config.LoginProperties;
import com.university.library.common.security.JwtProperties;
import com.university.library.common.security.JwtUtils;
import com.university.library.dto.ChangePasswordRequest;
import com.university.library.dto.LoginRequest;
import com.university.library.entity.LoginLog;
import com.university.library.entity.User;
import com.university.library.mapper.LoginLogMapper;
import com.university.library.mapper.UserMapper;
import com.university.library.service.support.ViewService;
import com.university.library.vo.LoginVO;
import com.university.library.vo.UserVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 登录锁定规则单元测试。
 */
class AuthServiceTest {

    private UserMapper userMapper;
    private LoginLogMapper loginLogMapper;
    private PasswordEncoder passwordEncoder;
    private ViewService viewService;
    private AuthService authService;
    private User admin;

    @BeforeEach
    void setUp() {
        userMapper = mock(UserMapper.class);
        loginLogMapper = mock(LoginLogMapper.class);
        passwordEncoder = new BCryptPasswordEncoder();
        viewService = mock(ViewService.class);

        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecret("unit-test-secret-key-0123456789-0123456789");
        jwtProperties.setExpireMinutes(60);
        jwtProperties.setIssuer("libraryms");
        JwtUtils jwtUtils = new JwtUtils(jwtProperties);

        LoginProperties loginProperties = new LoginProperties();
        loginProperties.setMaxFailAttempts(3);
        loginProperties.setLockMinutes(15);

        authService = new AuthService(userMapper, loginLogMapper, passwordEncoder, jwtUtils,
                loginProperties, viewService);

        admin = new User();
        admin.setId(1L);
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setName("顾清和");
        admin.setRole("ADMIN");
        admin.setStatus("ACTIVE");
        admin.setFailedAttempts(0);
        admin.setLockedUntil(null);

        when(userMapper.selectByUsernameForUpdate("admin")).thenReturn(admin);
        when(userMapper.selectById(1L)).thenReturn(admin);
        when(userMapper.updateById(any(User.class))).thenReturn(1);
        when(loginLogMapper.insert(any(LoginLog.class))).thenReturn(1);
        when(viewService.toUserVO(any(User.class))).thenReturn(new UserVO());
    }

    @Test
    @DisplayName("登录成功：签发 JWT、清空失败计数并记录成功日志")
    void loginSuccess() {
        admin.setFailedAttempts(2);
        admin.setLockedUntil(LocalDateTime.now().minusMinutes(1));

        OperationResult<LoginVO> result = authService.login(loginRequest("admin", "admin123"), "127.0.0.1");

        assertThat(result.data().getToken()).isNotBlank();
        assertThat(result.message()).isEqualTo("登录成功");
        assertThat(admin.getFailedAttempts()).isZero();
        assertThat(admin.getLockedUntil()).isNull();

        ArgumentCaptor<LoginLog> logCaptor = ArgumentCaptor.forClass(LoginLog.class);
        verify(loginLogMapper).insert(logCaptor.capture());
        assertThat(logCaptor.getValue().getSuccess()).isTrue();
        assertThat(logCaptor.getValue().getIp()).isEqualTo("127.0.0.1");
        assertThat(logCaptor.getValue().getRole()).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("密码错误：失败次数 +1，提示剩余次数")
    void loginWrongPasswordIncrementsAttempts() {
        BusinessException ex = catchThrowableOfType(
                () -> authService.login(loginRequest("admin", "wrong"), "127.0.0.1"),
                BusinessException.class);

        assertThat(ex.getCode()).isEqualTo(401);
        assertThat(ex.getMessage()).contains("还有 2 次尝试机会");
        assertThat(admin.getFailedAttempts()).isEqualTo(1);
        assertThat(admin.getLockedUntil()).isNull();

        ArgumentCaptor<LoginLog> logCaptor = ArgumentCaptor.forClass(LoginLog.class);
        verify(loginLogMapper).insert(logCaptor.capture());
        assertThat(logCaptor.getValue().getSuccess()).isFalse();
        verify(userMapper).updateById(admin);
    }

    @Test
    @DisplayName("连续第 3 次密码错误：锁定 15 分钟，失败计数归零")
    void loginThirdFailureLocksAccount() {
        admin.setFailedAttempts(2);

        BusinessException ex = catchThrowableOfType(
                () -> authService.login(loginRequest("admin", "wrong"), "127.0.0.1"),
                BusinessException.class);

        assertThat(ex.getCode()).isEqualTo(423);
        assertThat(ex.getMessage()).contains("锁定 15 分钟");
        assertThat(admin.getFailedAttempts()).isZero();
        assertThat(admin.getLockedUntil()).isAfter(LocalDateTime.now().plusMinutes(14));
        assertThat(admin.getLockedUntil()).isBefore(LocalDateTime.now().plusMinutes(16));
    }

    @Test
    @DisplayName("锁定期间登录：直接返回 423，且不校验密码")
    void loginRejectedWhenLocked() {
        admin.setLockedUntil(LocalDateTime.now().plusMinutes(10));

        BusinessException ex = catchThrowableOfType(
                () -> authService.login(loginRequest("admin", "admin123"), "127.0.0.1"),
                BusinessException.class);

        assertThat(ex.getCode()).isEqualTo(423);
        assertThat(ex.getMessage()).contains("账号已锁定");
        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    @DisplayName("停用账号禁止登录")
    void loginRejectedWhenDisabled() {
        admin.setStatus("DISABLED");

        BusinessException ex = catchThrowableOfType(
                () -> authService.login(loginRequest("admin", "admin123"), "127.0.0.1"),
                BusinessException.class);

        assertThat(ex.getCode()).isEqualTo(403);
        assertThat(ex.getMessage()).contains("已停用");
    }

    @Test
    @DisplayName("账号不存在：返回 401 并记录失败日志")
    void loginUnknownUserWritesLog() {
        when(userMapper.selectByUsernameForUpdate("nobody")).thenReturn(null);

        BusinessException ex = catchThrowableOfType(
                () -> authService.login(loginRequest("nobody", "whatever"), "10.0.0.8"),
                BusinessException.class);

        assertThat(ex.getCode()).isEqualTo(401);
        ArgumentCaptor<LoginLog> logCaptor = ArgumentCaptor.forClass(LoginLog.class);
        verify(loginLogMapper).insert(logCaptor.capture());
        assertThat(logCaptor.getValue().getUsername()).isEqualTo("nobody");
        assertThat(logCaptor.getValue().getUserId()).isNull();
        assertThat(logCaptor.getValue().getSuccess()).isFalse();
    }

    @Test
    @DisplayName("修改密码：原密码错误返回 400；新旧密码相同返回 400")
    void changePasswordValidation() {
        ChangePasswordRequest wrongOld = new ChangePasswordRequest();
        wrongOld.setOldPassword("bad");
        wrongOld.setNewPassword("newpass123");
        BusinessException ex1 = catchThrowableOfType(
                () -> authService.changePassword(
                        new com.university.library.common.security.UserPrincipal(1L, "admin", "ADMIN", null),
                        wrongOld),
                BusinessException.class);
        assertThat(ex1.getCode()).isEqualTo(400);
        assertThat(ex1.getMessage()).contains("原密码不正确");

        ChangePasswordRequest same = new ChangePasswordRequest();
        same.setOldPassword("admin123");
        same.setNewPassword("admin123");
        BusinessException ex2 = catchThrowableOfType(
                () -> authService.changePassword(
                        new com.university.library.common.security.UserPrincipal(1L, "admin", "ADMIN", null),
                        same),
                BusinessException.class);
        assertThat(ex2.getCode()).isEqualTo(400);
        assertThat(ex2.getMessage()).contains("不能与原密码相同");
    }

    private LoginRequest loginRequest(String username, String password) {
        LoginRequest request = new LoginRequest();
        request.setUsername(username);
        request.setPassword(password);
        return request;
    }
}

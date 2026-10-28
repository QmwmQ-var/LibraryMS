package com.university.library;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Spring 容器装配冒烟测试：使用 H2 内存数据库，验证数据源、MyBatis、
 * Spring Security、JWT 与全部 Service/Controller Bean 可以正常初始化。
 */
@SpringBootTest
@ActiveProfiles("test")
class LibraryManagementApplicationTests {

    @Test
    @DisplayName("Spring Boot 应用上下文可正常启动")
    void contextLoads() {
        // 上下文加载成功即通过
    }
}

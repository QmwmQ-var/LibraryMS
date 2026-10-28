package com.university.library;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 高校图书管理系统后端启动类。
 *
 * <p>技术栈：Spring Boot 3 + Spring Security(JWT 无状态) + MyBatis-Plus + MySQL。</p>
 *
 * <p>排除 Spring Security 默认的内存用户自动配置：本项目认证完全由 JWT 过滤器负责。</p>
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
@MapperScan("com.university.library.mapper")
@EnableScheduling
public class LibraryManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(LibraryManagementApplication.class, args);
    }
}

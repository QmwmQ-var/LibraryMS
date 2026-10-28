package com.university.library.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 端到端接口集成测试（H2 内存库）：
 * 登录 → 图书/统计 → 读者借书 → 重复借阅拦截 → 还书 → 罚款查询/缴纳 → 权限拦截。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiIntegrationTest {

    private static final String AUTH = "Authorization";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("管理员与读者完整业务链路")
    void fullBusinessFlow() throws Exception {
        String adminToken = login("admin", "admin123");
        String readerToken = login("2023001", "reader123");

        // 图书列表：分页结构 + 分类冗余字段
        mockMvc.perform(get("/api/books").header(AUTH, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.records[0].categoryName").exists())
                .andExpect(jsonPath("$.data.records[0].available").value(true));

        // 管理员统计概览
        mockMvc.perform(get("/api/stats/overview").header(AUTH, bearer(adminToken)))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.role").value("ADMIN"))
                .andExpect(jsonPath("$.data.totalTitles").value(3));

        // 管理员读者列表 + summary
        mockMvc.perform(get("/api/readers").header(AUTH, bearer(adminToken)))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.summary.total").value(1));

        // 管理员通知跳转路径必须命中真实路由，避免点击后 403/404
        mockMvc.perform(get("/api/notifications").header(AUTH, bearer(adminToken)))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.items.length()").value(3))
                .andExpect(jsonPath("$.data.items[0].path").value("/circulation?status=OVERDUE"))
                .andExpect(jsonPath("$.data.items[1].path").value("/penalties-manage"))
                .andExpect(jsonPath("$.data.items[2].path").value("/books-manage?availability=UNAVAILABLE"));

        // 读者禁止访问管理员统计接口
        mockMvc.perform(get("/api/stats/overview").header(AUTH, bearer(readerToken)))
                .andExpect(jsonPath("$.code").value(403));

        // 读者我的借阅：种子中有 1 条已归还 + 1 条逾期记录
        mockMvc.perform(get("/api/borrow-records/my").header(AUTH, bearer(readerToken)))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.summary.total").value(2))
                .andExpect(jsonPath("$.data.summary.active").value(1))
                .andExpect(jsonPath("$.data.summary.returned").value(1));

        // 借书：库存 3 → 2
        MvcResult borrowResult = mockMvc.perform(post("/api/borrow-records")
                        .header(AUTH, bearer(readerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\":1}"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.record.status").value("BORROWED"))
                .andReturn();
        JsonNode borrowJson = readBody(borrowResult);
        long recordId = borrowJson.path("data").path("record").path("id").asLong();
        assertThat(recordId).isPositive();

        mockMvc.perform(get("/api/books/1").header(AUTH, bearer(readerToken)))
                .andExpect(jsonPath("$.data.availableCopies").value(2));

        // 页签计数使用全量借阅记录，不应被 status 条件过滤
        mockMvc.perform(get("/api/borrow-records/my").param("status", "BORROWED")
                        .header(AUTH, bearer(readerToken)))
                .andExpect(jsonPath("$.data.records.length()").value(1))
                .andExpect(jsonPath("$.data.summary.active").value(2))
                .andExpect(jsonPath("$.data.summary.returned").value(1))
                .andExpect(jsonPath("$.data.summary.total").value(3));

        // 重复借阅同一本书被拦截
        mockMvc.perform(post("/api/borrow-records")
                        .header(AUTH, bearer(readerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\":1}"))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("该读者已借阅此书，请勿重复借阅"));

        // 还书：无逾期，不产生罚款；库存 2 → 3
        mockMvc.perform(post("/api/borrow-records/" + recordId + "/return")
                        .header(AUTH, bearer(readerToken)))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.record.status").value("RETURNED"))
                .andExpect(jsonPath("$.data.penalty").value(nullValue()));

        mockMvc.perform(get("/api/books/1").header(AUTH, bearer(readerToken)))
                .andExpect(jsonPath("$.data.availableCopies").value(3));

        // 罚款：种子中 1 笔 1.00 元待缴
        mockMvc.perform(get("/api/penalties/my").header(AUTH, bearer(readerToken)))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.summary.pendingCount").value(1))
                .andExpect(jsonPath("$.data.summary.pendingAmount").value(1.00));

        mockMvc.perform(post("/api/penalties/1/pay").header(AUTH, bearer(readerToken)))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("PAID"))
                .andExpect(jsonPath("$.data.paymentChannel").value("ONLINE"));

        // 重复缴纳被拦截
        mockMvc.perform(post("/api/penalties/1/pay").header(AUTH, bearer(readerToken)))
                .andExpect(jsonPath("$.code").value(400));

        // 读者禁止访问管理员罚款列表
        mockMvc.perform(get("/api/penalties").header(AUTH, bearer(readerToken)))
                .andExpect(jsonPath("$.code").value(403));

        // 仪表盘与通知
        mockMvc.perform(get("/api/dashboard").header(AUTH, bearer(readerToken)))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.role").value("READER"));
        mockMvc.perform(get("/api/notifications").header(AUTH, bearer(adminToken)))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.items").isArray());
    }

    @Test
    @DisplayName("未携带 Token 访问受保护接口返回 401（HTTP 200 + 业务码）")
    void unauthorizedRequest() throws Exception {
        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        JsonNode body = readBody(result);
        return body.path("data").path("token").asText();
    }

    private JsonNode readBody(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}

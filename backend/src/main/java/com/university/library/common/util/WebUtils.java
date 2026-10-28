package com.university.library.common.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Web 请求工具。
 */
public final class WebUtils {

    private static final String UNKNOWN = "unknown";

    private WebUtils() {
    }

    /**
     * 获取客户端 IP：优先取反向代理头，便于 Nginx 部署后仍能记录真实来源。
     */
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return UNKNOWN;
        }
        String[] headers = {"X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP", "WL-Proxy-Client-IP"};
        for (String header : headers) {
            String value = request.getHeader(header);
            if (value != null && !value.isBlank() && !UNKNOWN.equalsIgnoreCase(value)) {
                int comma = value.indexOf(',');
                return (comma > 0 ? value.substring(0, comma) : value).trim();
            }
        }
        return request.getRemoteAddr();
    }
}

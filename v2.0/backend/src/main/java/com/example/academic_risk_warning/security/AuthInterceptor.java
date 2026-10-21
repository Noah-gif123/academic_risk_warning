package com.example.academic_risk_warning.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/** 鉴权拦截器：解析令牌 → 写入 UserContext → 按 @RequireRole 做角色校验 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final TokenStore tokenStore;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        if (token == null || token.isEmpty()) {
            token = request.getHeader("token");
        }
        LoginUser user = tokenStore.find(token);
        if (user == null) {
            writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, 401, "未登录或登录已过期");
            return false;
        }
        UserContext.set(user);

        // 角色校验：方法或类上有 @RequireRole 时，当前角色必须在允许列表内
        if (handler instanceof HandlerMethod hm) {
            RequireRole require = hm.getMethodAnnotation(RequireRole.class);
            if (require == null) {
                require = hm.getBeanType().getAnnotation(RequireRole.class);
            }
            if (require != null && Arrays.stream(require.value()).noneMatch(r -> r.equals(user.getRole()))) {
                writeJson(response, HttpServletResponse.SC_FORBIDDEN, 403, "无权访问该功能");
                return false;
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    private void writeJson(HttpServletResponse response, int httpStatus, int code, String message) throws Exception {
        response.setStatus(httpStatus);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"code\":" + code + ",\"message\":\"" + message + "\",\"data\":null}");
    }
}

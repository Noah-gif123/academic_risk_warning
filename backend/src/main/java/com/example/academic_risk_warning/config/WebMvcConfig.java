package com.example.academic_risk_warning.config;

import com.example.academic_risk_warning.common.BusinessException;
import com.example.academic_risk_warning.context.TeacherContext;
import com.example.academic_risk_warning.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 登录鉴权拦截：除登录接口外，需在 Header 携带 Authorization: Bearer {token}
 * 支持教师Token和管理员Token
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final TokenService tokenService;

    public WebMvcConfig(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AuthInterceptor(tokenService))
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/teacher/login", "/api/admin/login", "/api/student/login");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 映射 uploads 目录，让上传的文件可以通过 /uploads/ 路径访问
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }

    static class AuthInterceptor implements HandlerInterceptor {

        private final TokenService tokenService;

        AuthInterceptor(TokenService tokenService) {
            this.tokenService = tokenService;
        }

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            String auth = request.getHeader("Authorization");
            String token = extractToken(auth);

            // 先验证教师token
            Long teacherId = tokenService.validateAndGetTeacherId(token);
            if (teacherId != null) {
                TeacherContext.setTeacherId(teacherId);
                return true;
            }

            // 再验证管理员token（管理员id存入TeacherContext以便Controller使用）
            Long adminId = tokenService.validateAndGetAdminId(token);
            if (adminId != null) {
                TeacherContext.setTeacherId(adminId);
                return true;
            }

            throw BusinessException.of(401, "未登录或 Token 已失效，请重新登录");
        }

        @Override
        public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                    Object handler, Exception ex) {
            TeacherContext.clear();
        }

        private String extractToken(String authorization) {
            if (authorization == null || authorization.isBlank()) {
                return null;
            }
            if (authorization.startsWith("Bearer ")) {
                return authorization.substring(7).trim();
            }
            return authorization.trim();
        }
    }
}

package com.example.academic_risk_warning.service;

import com.example.academic_risk_warning.config.AuthProperties;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 简易 Token 管理（内存存储，重启后需重新登录）
 */
@Service
public class TokenService {

    private final Map<String, TokenInfo> tokenStore = new ConcurrentHashMap<>();
    private final Map<String, TokenInfo> adminTokenStore = new ConcurrentHashMap<>();
    private final AuthProperties authProperties;

    public TokenService(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    // ========== 教师/学生 Token ==========

    public String createToken(Long teacherId) {
        String token = UUID.randomUUID().toString().replace("-", "");
        long expireAt = Instant.now().getEpochSecond()
                + authProperties.getTokenExpireHours() * 3600L;
        tokenStore.put(token, new TokenInfo(teacherId, expireAt));
        return token;
    }

    public Long validateAndGetTeacherId(String token) {
        return validateAndGetUserId(token);
    }

    public Long validateAndGetStudentId(String token) {
        return validateAndGetUserId(token);
    }

    private Long validateAndGetUserId(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        TokenInfo info = tokenStore.get(token);
        if (info == null) {
            return null;
        }
        if (Instant.now().getEpochSecond() > info.expireAt()) {
            tokenStore.remove(token);
            return null;
        }
        return info.teacherId();
    }

    public void removeToken(String token) {
        if (token != null) {
            tokenStore.remove(token);
        }
    }

    /**
     * 刷新 Token：验证旧 Token 有效后，签发新 Token 并销毁旧 Token
     * @return 新 Token，验证失败返回 null
     */
    public String refreshToken(String oldToken) {
        if (oldToken == null || oldToken.isBlank()) return null;
        TokenInfo info = tokenStore.get(oldToken);
        if (info == null) return null;
        if (Instant.now().getEpochSecond() > info.expireAt()) {
            tokenStore.remove(oldToken);
            return null;
        }
        // 销毁旧 Token，基于同一 user ID 签发新 Token
        Long userId = info.teacherId();
        tokenStore.remove(oldToken);
        return createToken(userId);
    }

    // ========== 管理员 Token ==========

    public String createAdminToken(Long adminId) {
        String token = UUID.randomUUID().toString().replace("-", "");
        long expireAt = Instant.now().getEpochSecond()
                + authProperties.getTokenExpireHours() * 3600L;
        adminTokenStore.put(token, new TokenInfo(adminId, expireAt));
        return token;
    }

    public Long validateAndGetAdminId(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        TokenInfo info = adminTokenStore.get(token);
        if (info == null) {
            return null;
        }
        if (Instant.now().getEpochSecond() > info.expireAt()) {
            adminTokenStore.remove(token);
            return null;
        }
        return info.teacherId();
    }

    public void removeAdminToken(String token) {
        if (token != null) {
            adminTokenStore.remove(token);
        }
    }

    private record TokenInfo(Long teacherId, long expireAt) {
    }
}

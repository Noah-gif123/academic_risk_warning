package com.example.academic_risk_warning.service;

import com.example.academic_risk_warning.config.AuthProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TokenService 单元测试
 * <p>
 * TokenService 基于 ConcurrentHashMap 存储，无需 Spring 容器即可测试。
 * 通过 Mock AuthProperties 控制 Token 过期时间。
 */
@DisplayName("TokenService 单元测试")
class TokenServiceTest {

    private TokenService tokenService;
    private AuthProperties authProperties;

    @BeforeEach
    void setUp() {
        authProperties = new AuthProperties();
        authProperties.setTokenExpireHours(24);
        tokenService = new TokenService(authProperties);
    }

    // ==================== 教师/学生 Token ====================

    @Nested
    @DisplayName("createToken — 创建 Token")
    class CreateToken {

        @Test
        @DisplayName("应返回非空的UUID格式Token")
        void shouldReturnNonEmptyToken() {
            String token = tokenService.createToken(1L);
            assertNotNull(token);
            assertFalse(token.isBlank());
            assertEquals(32, token.length()); // UUID without dashes = 32 hex chars
        }

        @Test
        @DisplayName("不同用户ID应生成不同Token")
        void shouldGenerateDifferentTokensForDifferentUsers() {
            String token1 = tokenService.createToken(1L);
            String token2 = tokenService.createToken(2L);
            assertNotEquals(token1, token2);
        }

        @Test
        @DisplayName("相同用户ID多次调用应生成不同Token")
        void shouldGenerateDifferentTokensForSameUser() {
            String token1 = tokenService.createToken(1L);
            String token2 = tokenService.createToken(1L);
            assertNotEquals(token1, token2);
        }
    }

    @Nested
    @DisplayName("validateAndGetTeacherId — 验证Token并获取教师ID")
    class ValidateToken {

        @Test
        @DisplayName("有效Token → 返回用户ID")
        void shouldReturnUserIdForValidToken() {
            String token = tokenService.createToken(100L);
            Long id = tokenService.validateAndGetTeacherId(token);
            assertEquals(100L, id);
        }

        @Test
        @DisplayName("null Token → 返回null")
        void shouldReturnNullForNullToken() {
            assertNull(tokenService.validateAndGetTeacherId(null));
        }

        @Test
        @DisplayName("空白Token → 返回null")
        void shouldReturnNullForBlankToken() {
            assertNull(tokenService.validateAndGetTeacherId("   "));
        }

        @Test
        @DisplayName("不存在的Token → 返回null")
        void shouldReturnNullForNonExistentToken() {
            assertNull(tokenService.validateAndGetTeacherId("nonexistent"));
        }

        @Test
        @DisplayName("已过期的Token → 返回null")
        void shouldReturnNullForExpiredToken() {
            // 设置过期时间为负数(已过期)
            authProperties.setTokenExpireHours(-1);
            String token = tokenService.createToken(1L);
            assertNull(tokenService.validateAndGetTeacherId(token));
        }
    }

    @Nested
    @DisplayName("validateAndGetStudentId — 验证学生Token")
    class ValidateStudentToken {

        @Test
        @DisplayName("有效Token → 返回用户ID")
        void shouldReturnUserIdForValidToken() {
            String token = tokenService.createToken(200L);
            Long id = tokenService.validateAndGetStudentId(token);
            assertEquals(200L, id);
        }
    }

    @Nested
    @DisplayName("removeToken — 移除Token")
    class RemoveToken {

        @Test
        @DisplayName("移除后验证应返回null")
        void shouldInvalidateAfterRemoval() {
            String token = tokenService.createToken(1L);
            tokenService.removeToken(token);
            assertNull(tokenService.validateAndGetTeacherId(token));
        }

        @Test
        @DisplayName("移除null Token不抛异常")
        void shouldNotThrowForNullToken() {
            assertDoesNotThrow(() -> tokenService.removeToken(null));
        }
    }

    @Nested
    @DisplayName("refreshToken — 刷新Token")
    class RefreshToken {

        @Test
        @DisplayName("有效旧Token → 返回新Token且旧Token失效")
        void shouldReturnNewTokenAndInvalidateOld() {
            String oldToken = tokenService.createToken(1L);
            String newToken = tokenService.refreshToken(oldToken);

            assertNotNull(newToken);
            assertNotEquals(oldToken, newToken);

            // 旧Token应失效
            assertNull(tokenService.validateAndGetTeacherId(oldToken));
            // 新Token应有效且返回相同用户ID
            assertEquals(1L, tokenService.validateAndGetTeacherId(newToken));
        }

        @Test
        @DisplayName("null Token → 返回null")
        void shouldReturnNullForNullToken() {
            assertNull(tokenService.refreshToken(null));
        }

        @Test
        @DisplayName("空白Token → 返回null")
        void shouldReturnNullForBlankToken() {
            assertNull(tokenService.refreshToken("  "));
        }

        @Test
        @DisplayName("不存在的Token → 返回null")
        void shouldReturnNullForNonExistentToken() {
            assertNull(tokenService.refreshToken("nonexistent"));
        }

        @Test
        @DisplayName("过期Token → 返回null且旧Token从存储移除")
        void shouldReturnNullForExpiredToken() {
            authProperties.setTokenExpireHours(-1);
            String token = tokenService.createToken(1L);
            assertNull(tokenService.refreshToken(token));
            // 过期Token应被清理
            assertNull(tokenService.validateAndGetTeacherId(token));
        }
    }

    // ==================== 管理员 Token ====================

    @Nested
    @DisplayName("管理员Token — createAdminToken / validateAndGetAdminId / removeAdminToken")
    class AdminToken {

        @Test
        @DisplayName("创建 → 验证 → 返回管理员ID")
        void shouldCreateAndValidateAdminToken() {
            String token = tokenService.createAdminToken(999L);
            Long id = tokenService.validateAndGetAdminId(token);
            assertEquals(999L, id);
        }

        @Test
        @DisplayName("移除 → 验证返回null")
        void shouldInvalidateAfterRemoval() {
            String token = tokenService.createAdminToken(999L);
            tokenService.removeAdminToken(token);
            assertNull(tokenService.validateAndGetAdminId(token));
        }

        @Test
        @DisplayName("管理员Token与教师Token存储隔离")
        void shouldIsolateAdminAndTeacherTokens() {
            String teacherToken = tokenService.createToken(1L);
            String adminToken = tokenService.createAdminToken(2L);

            // 用 teacher store 查 admin token → null
            assertNull(tokenService.validateAndGetTeacherId(adminToken));
            // 用 admin store 查 teacher token → null
            assertNull(tokenService.validateAndGetAdminId(teacherToken));
        }
    }
}

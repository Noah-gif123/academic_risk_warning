/* gen_infra.js — 生成 v2.0 后端骨架（common / config / security / service / controller / dto / vo / 启动类 / 配置） */
'use strict';
const fs = require('fs');
const path = require('path');

const BE = 'E:/JavaDEVIDEA/workspace/JavaProject/academic_risk_warning/v2.0/backend';
const J = path.join(BE, 'src/main/java/com/example/academic_risk_warning');
const R = path.join(BE, 'src/main/resources');
const P = 'com.example.academic_risk_warning';

function w(rel, content) {
  const p = path.join(J, rel);
  fs.mkdirSync(path.dirname(p), { recursive: true });
  fs.writeFileSync(p, content.replace(/^\n/, ''), 'utf8');
}

// ---------------------------------------------------------------- 启动类
w('AcademicRiskWarningApplication.java', `
package ${P};

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 学情预警系统 v2.0 启动类
 * <p>四端（学生 / 教师 / 导员 / 教务）统一入口，端口 8080。</p>
 */
@EnableScheduling
@SpringBootApplication
@MapperScan("${P}.mapper")
public class AcademicRiskWarningApplication {
    public static void main(String[] args) {
        SpringApplication.run(AcademicRiskWarningApplication.class, args);
    }
}
`);

// ---------------------------------------------------------------- common
w('common/Result.java', `
package ${P}.common;

import lombok.Data;

/** 统一返回体：业务错误保持 HTTP 200 + code，未登录返回真实 401 */
@Data
public class Result<T> {
    private int code;
    private String message;
    private T data;

    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.code = 0;
        r.message = "ok";
        r.data = data;
        return r;
    }

    public static <T> Result<T> ok() {
        return ok(null);
    }

    public static <T> Result<T> ok(String message, T data) {
        Result<T> r = ok(data);
        r.message = message;
        return r;
    }

    public static <T> Result<T> fail(int code, String message) {
        Result<T> r = new Result<>();
        r.code = code;
        r.message = message;
        return r;
    }
}
`);

w('common/BizException.java', `
package ${P}.common;

/** 业务异常：由全局异常处理器转成统一返回体 */
public class BizException extends RuntimeException {
    private final int code;

    public BizException(String message) {
        this(400, message);
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
`);

w('common/GlobalExceptionHandler.java', `
package ${P}.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 全局异常处理：参数校验、业务异常、兜底异常 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ResponseEntity<Result<Void>> handleBiz(BizException e) {
        HttpStatus status = e.getCode() == 401 ? HttpStatus.UNAUTHORIZED : HttpStatus.OK;
        return ResponseEntity.status(status).body(Result.fail(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValid(MethodArgumentNotValidException e) {
        FieldError fe = e.getBindingResult().getFieldError();
        String msg = fe == null ? "参数校验失败" : fe.getField() + " " + fe.getDefaultMessage();
        return Result.fail(400, msg);
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleOther(Exception e) {
        log.error("未处理异常", e);
        return Result.fail(500, "服务内部错误：" + e.getMessage());
    }
}
`);

// ---------------------------------------------------------------- config
w('config/MybatisPlusConfig.java', `
package ${P}.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** MyBatis-Plus 配置：只注册分页插件（逻辑删除由业务字段 is_void 自行处理） */
@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
`);

w('config/WebConfig.java', `
package ${P}.config;

import ${P}.security.AuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Web 配置：跨域（前端 5173）+ 鉴权拦截器 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/login", "/api/health");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
`);

// ---------------------------------------------------------------- security
w('security/PasswordHasher.java', `
package ${P}.security;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 口令哈希：PBKDF2WithHmacSHA256（JDK 自带，零依赖）
 * <p>存储格式：pbkdf2$迭代次数$Base64(盐)$Base64(哈希)</p>
 */
public final class PasswordHasher {

    private static final String ALGO = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 120_000;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_LENGTH = 16;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder ENC = Base64.getEncoder();
    private static final Base64.Decoder DEC = Base64.getDecoder();

    private PasswordHasher() {
    }

    public static String hash(String rawPassword) {
        byte[] salt = new byte[SALT_LENGTH];
        RANDOM.nextBytes(salt);
        byte[] hash = pbkdf2(rawPassword.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
        return "pbkdf2$" + ITERATIONS + "$" + ENC.encodeToString(salt) + "$" + ENC.encodeToString(hash);
    }

    public static boolean matches(String rawPassword, String stored) {
        if (rawPassword == null || stored == null) {
            return false;
        }
        String[] parts = stored.split("\\\\$");
        if (parts.length != 4 || !"pbkdf2".equals(parts[0])) {
            return false;
        }
        int iterations = Integer.parseInt(parts[1]);
        byte[] salt = DEC.decode(parts[2]);
        byte[] expected = DEC.decode(parts[3]);
        byte[] actual = pbkdf2(rawPassword.toCharArray(), salt, iterations, expected.length * 8);
        return java.security.MessageDigest.isEqual(expected, actual);
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLength) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLength);
            return SecretKeyFactory.getInstance(ALGO).generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("口令哈希失败", e);
        }
    }
}
`);

w('security/LoginUser.java', `
package ${P}.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 登录用户（来自令牌）：角色 + 归属，用于限定数据范围 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginUser {
    /** 账号ID（sys_account.id） */
    private Long accountId;
    /** 登录号（学号 / 工号 / 教务账号） */
    private String loginNo;
    /** 角色：STUDENT / TEACHER / COUNSELOR / ADMIN */
    private String role;
    /** 归属ID：student.id / teacher.id / counselor.id / admin.id */
    private Long refId;
    /** 姓名 */
    private String realName;
}
`);

w('security/TokenStore.java', `
package ${P}.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 令牌存储：内存实现（单人部署够用；后续可换成数据库或 Redis） */
@Component
public class TokenStore {

    private static class Entry {
        LoginUser user;
        long expireAt;
    }

    private final Map<String, Entry> tokens = new ConcurrentHashMap<>();

    @Value("\${app.auth.token-expire-hours:12}")
    private long expireHours;

    public String issue(LoginUser user) {
        String token = UUID.randomUUID().toString().replace("-", "");
        Entry e = new Entry();
        e.user = user;
        e.expireAt = System.currentTimeMillis() + expireHours * 3600_000L;
        tokens.put(token, e);
        return token;
    }

    public LoginUser find(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        Entry e = tokens.get(token);
        if (e == null) {
            return null;
        }
        if (e.expireAt < System.currentTimeMillis()) {
            tokens.remove(token);
            return null;
        }
        return e.user;
    }

    public void revoke(String token) {
        if (token != null) {
            tokens.remove(token);
        }
    }
}
`);

w('security/UserContext.java', `
package ${P}.security;

/** 当前请求的登录用户（线程内可见） */
public final class UserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    public static LoginUser require() {
        LoginUser user = HOLDER.get();
        if (user == null) {
            throw new IllegalStateException("未登录");
        }
        return user;
    }

    public static void clear() {
        HOLDER.remove();
    }
}
`);

w('security/AuthInterceptor.java', `
package ${P}.security;

import ${P}.common.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

/** 鉴权拦截器：解析令牌 → 写入 UserContext；失败返回真实 401 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final TokenStore tokenStore;
    private final ObjectMapper objectMapper;

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
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write(objectMapper.writeValueAsString(Result.fail(401, "未登录或登录已过期")));
            return false;
        }
        UserContext.set(user);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}
`);

// ---------------------------------------------------------------- dto / vo
w('dto/LoginRequest.java', `
package ${P}.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 登录请求：四端共用（账号为学号 / 工号 / 教务账号，默认密码 000000） */
@Data
public class LoginRequest {
    @NotBlank(message = "登录号不能为空")
    private String loginNo;

    @NotBlank(message = "密码不能为空")
    private String password;
}
`);

w('dto/ChangePasswordRequest.java', `
package ${P}.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 修改密码 */
@Data
public class ChangePasswordRequest {
    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 32, message = "新密码长度需在 6~32 位之间")
    private String newPassword;
}
`);

w('vo/LoginVO.java', `
package com.example.academic_risk_warning.vo;

import lombok.Builder;
import lombok.Data;

/** 登录返回：令牌 + 角色 + 归属 + 是否首次登录 */
@Data
@Builder
public class LoginVO {
    private String token;
    private String role;
    private String loginNo;
    private String realName;
    private Long refId;
    private boolean firstLogin;
}
`);

// ---------------------------------------------------------------- service
w('service/AuthService.java', `
package ${P}.service;

import ${P}.common.BizException;
import ${P}.dto.ChangePasswordRequest;
import ${P}.dto.LoginRequest;
import ${P}.entity.Admin;
import ${P}.entity.Counselor;
import ${P}.entity.Student;
import ${P}.entity.SysAccount;
import ${P}.entity.Teacher;
import ${P}.mapper.AdminMapper;
import ${P}.mapper.CounselorMapper;
import ${P}.mapper.StudentMapper;
import ${P}.mapper.SysAccountMapper;
import ${P}.mapper.TeacherMapper;
import ${P}.security.LoginUser;
import ${P}.security.PasswordHasher;
import ${P}.security.TokenStore;
import ${P}.vo.LoginVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** 统一登录与口令管理（四端共用一套实现） */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysAccountMapper accountMapper;
    private final StudentMapper studentMapper;
    private final TeacherMapper teacherMapper;
    private final CounselorMapper counselorMapper;
    private final AdminMapper adminMapper;
    private final TokenStore tokenStore;

    @Transactional
    public LoginVO login(LoginRequest request) {
        SysAccount account = accountMapper.selectOne(
                Wrappers.<SysAccount>lambdaQuery().eq(SysAccount::getLoginNo, request.getLoginNo()));
        if (account == null) {
            throw new BizException(401, "账号不存在");
        }
        if (!"ACTIVE".equals(account.getStatus())) {
            throw new BizException(403, "账号已停用或锁定，请联系教务管理员");
        }
        if (!PasswordHasher.matches(request.getPassword(), account.getPasswordHash())) {
            throw new BizException(401, "密码错误");
        }
        Long refId = resolveRefId(account);
        LoginUser user = LoginUser.builder()
                .accountId(account.getId())
                .loginNo(account.getLoginNo())
                .role(account.getRole())
                .refId(refId)
                .realName(account.getRealName())
                .build();
        String token = tokenStore.issue(user);

        account.setLastLoginTime(LocalDateTime.now());
        accountMapper.updateById(account);

        return LoginVO.builder()
                .token(token)
                .role(account.getRole())
                .loginNo(account.getLoginNo())
                .realName(account.getRealName())
                .refId(refId)
                .firstLogin(account.getPwdUpdateTime() == null)
                .build();
    }

    public void logout(String token) {
        tokenStore.revoke(token);
    }

    @Transactional
    public void changePassword(Long accountId, ChangePasswordRequest request) {
        SysAccount account = accountMapper.selectById(accountId);
        if (account == null) {
            throw new BizException(401, "账号不存在");
        }
        if (!PasswordHasher.matches(request.getOldPassword(), account.getPasswordHash())) {
            throw new BizException(400, "旧密码不正确");
        }
        if (PasswordHasher.matches(request.getNewPassword(), account.getPasswordHash())) {
            throw new BizException(400, "新密码不能与旧密码相同");
        }
        account.setPasswordHash(PasswordHasher.hash(request.getNewPassword()));
        account.setPwdUpdateTime(LocalDateTime.now());
        accountMapper.updateById(account);
    }

    public LoginVO me(LoginUser user) {
        return LoginVO.builder()
                .role(user.getRole())
                .loginNo(user.getLoginNo())
                .realName(user.getRealName())
                .refId(user.getRefId())
                .build();
    }

    /** 按角色解析归属ID（学生/教师/导员/教务各自的档案主键） */
    private Long resolveRefId(SysAccount account) {
        Long accountId = account.getId();
        switch (account.getRole()) {
            case "STUDENT": {
                Student s = studentMapper.selectOne(Wrappers.<Student>lambdaQuery().eq(Student::getAccountId, accountId));
                return s == null ? null : s.getId();
            }
            case "TEACHER": {
                Teacher t = teacherMapper.selectOne(Wrappers.<Teacher>lambdaQuery().eq(Teacher::getAccountId, accountId));
                return t == null ? null : t.getId();
            }
            case "COUNSELOR": {
                Counselor c = counselorMapper.selectOne(Wrappers.<Counselor>lambdaQuery().eq(Counselor::getAccountId, accountId));
                return c == null ? null : c.getId();
            }
            case "ADMIN": {
                Admin a = adminMapper.selectOne(Wrappers.<Admin>lambdaQuery().eq(Admin::getAccountId, accountId));
                return a == null ? null : a.getId();
            }
            default:
                return null;
        }
    }
}
`);

// ---------------------------------------------------------------- controller
w('controller/AuthController.java', `
package ${P}.controller;

import ${P}.common.Result;
import ${P}.dto.ChangePasswordRequest;
import ${P}.dto.LoginRequest;
import ${P}.security.LoginUser;
import ${P}.security.UserContext;
import ${P}.service.AuthService;
import ${P}.vo.LoginVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** 统一登录接口：四端共用 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
    }

    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        authService.logout(token);
        return Result.ok();
    }

    @GetMapping("/me")
    public Result<LoginVO> me() {
        LoginUser user = UserContext.require();
        return Result.ok(authService.me(user));
    }

    @PostMapping("/change-password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        LoginUser user = UserContext.require();
        authService.changePassword(user.getAccountId(), request);
        return Result.ok();
    }
}
`);

// ---------------------------------------------------------------- bootstrap
w('bootstrap/AccountBootstrap.java', `
package ${P}.bootstrap;

import ${P}.entity.Admin;
import ${P}.entity.Counselor;
import ${P}.entity.Student;
import ${P}.entity.SysAccount;
import ${P}.entity.Teacher;
import ${P}.mapper.AdminMapper;
import ${P}.mapper.CounselorMapper;
import ${P}.mapper.StudentMapper;
import ${P}.mapper.SysAccountMapper;
import ${P}.mapper.TeacherMapper;
import ${P}.security.PasswordHasher;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 账号初始化任务（幂等）：为没有 account 的档案创建统一账号并回填 account_id
 * <p>登录号 = 学号 / 工号 / 教务号；初始口令取 app.bootstrap.default-password（默认 000000）。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccountBootstrap implements CommandLineRunner {

    private final SysAccountMapper accountMapper;
    private final StudentMapper studentMapper;
    private final TeacherMapper teacherMapper;
    private final CounselorMapper counselorMapper;
    private final AdminMapper adminMapper;

    @Value("\${app.bootstrap.enabled:false}")
    private boolean enabled;

    @Value("\${app.bootstrap.default-password:000000}")
    private String defaultPassword;

    @Override
    @Transactional
    public void run(String... args) {
        if (!enabled) {
            log.info("[账号初始化] 已禁用（app.bootstrap.enabled=false）");
            return;
        }
        String hash = PasswordHasher.hash(defaultPassword);
        int created = 0;

        for (Student s : studentMapper.selectList(Wrappers.<Student>lambdaQuery().isNull(Student::getAccountId))) {
            SysAccount a = newAccount(s.getStudentNo(), "STUDENT", s.getStudentName(), hash);
            s.setAccountId(a.getId());
            studentMapper.updateById(s);
            created++;
        }
        for (Teacher t : teacherMapper.selectList(Wrappers.<Teacher>lambdaQuery().isNull(Teacher::getAccountId))) {
            SysAccount a = newAccount(t.getTeacherNo(), "TEACHER", t.getTeacherName(), hash);
            t.setAccountId(a.getId());
            teacherMapper.updateById(t);
            created++;
        }
        for (Counselor c : counselorMapper.selectList(Wrappers.<Counselor>lambdaQuery().isNull(Counselor::getAccountId))) {
            SysAccount a = newAccount(c.getCounselorNo(), "COUNSELOR", c.getCounselorName(), hash);
            c.setAccountId(a.getId());
            counselorMapper.updateById(c);
            created++;
        }
        for (Admin ad : adminMapper.selectList(Wrappers.<Admin>lambdaQuery().isNull(Admin::getAccountId))) {
            SysAccount a = newAccount(ad.getAdminNo(), "ADMIN", ad.getAdminName(), hash);
            ad.setAccountId(a.getId());
            adminMapper.updateById(ad);
            created++;
        }
        log.info("[账号初始化] 完成，新建账号 {} 个（初始口令 {}）", created, defaultPassword);
    }

    private SysAccount newAccount(String loginNo, String role, String realName, String hash) {
        SysAccount a = new SysAccount();
        a.setLoginNo(loginNo);
        a.setPasswordHash(hash);
        a.setRole(role);
        a.setRealName(realName);
        a.setStatus("ACTIVE");
        accountMapper.insert(a);
        return a;
    }
}
`);

// ---------------------------------------------------------------- application.yml
fs.mkdirSync(R, { recursive: true });
fs.writeFileSync(path.join(R, 'application.yml'), `
server:
  port: 8080
  servlet:
    encoding:
      charset: UTF-8
      force: true

spring:
  application:
    name: academic-risk-warning
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/study_warning_system_v2?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
    username: \${DB_USERNAME:root}
    password: \${DB_PASSWORD:root}
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      maximum-pool-size: 10
      minimum-idle: 2
      connection-timeout: 10000
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: Asia/Shanghai
    default-property-inclusion: non_null

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
  global-config:
    banner: false
    db-config:
      id-type: auto

app:
  auth:
    # 令牌有效期（小时）
    token-expire-hours: 12
  bootstrap:
    # 首次启动时为档案建统一账号（幂等，可关掉）
    enabled: true
    default-password: "000000"

logging:
  level:
    root: info
    com.example.academic_risk_warning: debug
`.replace(/^\n/, ''), 'utf8');

console.log('后端骨架生成完成');

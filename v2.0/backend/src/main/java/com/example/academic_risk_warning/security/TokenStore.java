package com.example.academic_risk_warning.security;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.academic_risk_warning.entity.Admin;
import com.example.academic_risk_warning.entity.Counselor;
import com.example.academic_risk_warning.entity.Student;
import com.example.academic_risk_warning.entity.SysAccount;
import com.example.academic_risk_warning.entity.Teacher;
import com.example.academic_risk_warning.mapper.AdminMapper;
import com.example.academic_risk_warning.mapper.CounselorMapper;
import com.example.academic_risk_warning.mapper.StudentMapper;
import com.example.academic_risk_warning.mapper.SysAccountMapper;
import com.example.academic_risk_warning.mapper.TeacherMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 令牌存储：**落库 + 内存缓存**。
 * 落库的意义：后端进程重启后已登录用户不会被踢下线（之前用纯内存 Map，一重启全员 401「未登录或登录已过期」）。
 * 单点登录：一个账号同时只保留一个有效令牌，新登录会顶掉旧令牌。
 */
@Component
@RequiredArgsConstructor
public class TokenStore {

    private final SysAccountMapper accountMapper;
    private final StudentMapper studentMapper;
    private final TeacherMapper teacherMapper;
    private final CounselorMapper counselorMapper;
    private final AdminMapper adminMapper;

    @Value("${app.auth.token-expire-hours:12}")
    private long expireHours;

    /** token → 用户（命中不必查库；注销/过期时移除） */
    private final Map<String, LoginUser> cache = new ConcurrentHashMap<>();

    public String issue(LoginUser user) {
        String token = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime expireAt = LocalDateTime.now().plusHours(expireHours);
        SysAccount account = accountMapper.selectById(user.getAccountId());
        if (account != null) {
            account.setToken(token);
            account.setTokenExpireTime(expireAt);
            accountMapper.updateById(account);
        }
        cache.put(token, user);
        return token;
    }

    public LoginUser find(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        LoginUser cached = cache.get(token);
        if (cached != null) {
            return cached;
        }
        SysAccount account = accountMapper.selectOne(Wrappers.<SysAccount>lambdaQuery()
                .eq(SysAccount::getToken, token).last("limit 1"));
        if (account == null) {
            return null;
        }
        if (account.getTokenExpireTime() != null && account.getTokenExpireTime().isBefore(LocalDateTime.now())) {
            return null;
        }
        if (!"ACTIVE".equals(account.getStatus())) {
            return null;
        }
        LoginUser user = LoginUser.builder()
                .accountId(account.getId())
                .loginNo(account.getLoginNo())
                .role(account.getRole())
                .realName(account.getRealName())
                .refId(resolveRefId(account))
                .build();
        cache.put(token, user);
        return user;
    }

    public void revoke(String token) {
        if (token == null || token.isEmpty()) {
            return;
        }
        cache.remove(token);
        SysAccount account = accountMapper.selectOne(Wrappers.<SysAccount>lambdaQuery()
                .eq(SysAccount::getToken, token).last("limit 1"));
        if (account != null) {
            // updateById 会跳过 null 字段，这里用 UpdateWrapper 显式置空
            accountMapper.update(null, Wrappers.<SysAccount>lambdaUpdate()
                    .set(SysAccount::getToken, null)
                    .set(SysAccount::getTokenExpireTime, null)
                    .eq(SysAccount::getId, account.getId()));
        }
    }

    /** 按角色解析归属ID（student.id / teacher.id / counselor.id / admin.id） */
    private Long resolveRefId(SysAccount account) {
        Long accountId = account.getId();
        switch (account.getRole() == null ? "" : account.getRole()) {
            case "STUDENT": {
                Student s = studentMapper.selectOne(Wrappers.<Student>lambdaQuery()
                        .eq(Student::getAccountId, accountId));
                return s == null ? null : s.getId();
            }
            case "TEACHER": {
                Teacher t = teacherMapper.selectOne(Wrappers.<Teacher>lambdaQuery()
                        .eq(Teacher::getAccountId, accountId));
                return t == null ? null : t.getId();
            }
            case "COUNSELOR": {
                Counselor c = counselorMapper.selectOne(Wrappers.<Counselor>lambdaQuery()
                        .eq(Counselor::getAccountId, accountId));
                return c == null ? null : c.getId();
            }
            case "ADMIN": {
                Admin a = adminMapper.selectOne(Wrappers.<Admin>lambdaQuery()
                        .eq(Admin::getAccountId, accountId));
                return a == null ? null : a.getId();
            }
            default:
                return null;
        }
    }
}

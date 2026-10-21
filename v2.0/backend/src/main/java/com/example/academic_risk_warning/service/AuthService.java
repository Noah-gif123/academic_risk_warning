package com.example.academic_risk_warning.service;

import com.example.academic_risk_warning.common.BizException;
import com.example.academic_risk_warning.dto.ChangePasswordRequest;
import com.example.academic_risk_warning.dto.LoginRequest;
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
import com.example.academic_risk_warning.security.LoginUser;
import com.example.academic_risk_warning.security.PasswordHasher;
import com.example.academic_risk_warning.security.TokenStore;
import com.example.academic_risk_warning.vo.LoginVO;
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
    private final CaptchaService captchaService;

    @Transactional
    public LoginVO login(LoginRequest request) {
        captchaService.verify(request.getCaptchaId(), request.getCaptchaCode());
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

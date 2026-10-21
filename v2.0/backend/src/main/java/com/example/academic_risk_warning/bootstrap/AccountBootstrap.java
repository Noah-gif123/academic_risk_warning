package com.example.academic_risk_warning.bootstrap;

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
import com.example.academic_risk_warning.security.PasswordHasher;
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

    @Value("${app.bootstrap.enabled:false}")
    private boolean enabled;

    @Value("${app.bootstrap.default-password:000000}")
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

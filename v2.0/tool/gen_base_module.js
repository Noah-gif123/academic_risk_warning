/* gen_base_module.js — 第一阶段 1-3 基础数据模块（学期与周历 / 专业 / 行政班 / 辅导员分配） */
'use strict';
const fs = require('fs');
const path = require('path');

const BE = 'E:/JavaDEVIDEA/workspace/JavaProject/academic_risk_warning/v2.0/backend';
const J = path.join(BE, 'src/main/java/com/example/academic_risk_warning');
const P = 'com.example.academic_risk_warning';

function w(rel, content) {
  const p = path.join(J, rel);
  fs.mkdirSync(path.dirname(p), { recursive: true });
  fs.writeFileSync(p, content.replace(/^\n/, ''), 'utf8');
}

// ---------------------------------------------------------------- 权限注解与检查
w('security/RequireRole.java', `
package ${P}.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 角色校验注解：标在 Controller 类或方法上，允许访问的角色列表 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {
    /** 允许的角色：STUDENT / TEACHER / COUNSELOR / ADMIN */
    String[] value();
}
`);

// ---------------------------------------------------------------- common
w('common/PageResult.java', `
package ${P}.common;

import lombok.Data;

import java.util.List;

/** 简易分页结果（本阶段数据量小，采用内存切片；后续可换成 MyBatis-Plus 分页插件） */
@Data
public class PageResult<T> {
    private long total;
    private int page;
    private int size;
    private List<T> records;

    public static <T> PageResult<T> of(List<T> all, int page, int size) {
        int p = Math.max(page, 1);
        int s = size <= 0 ? 20 : size;
        int from = Math.min((p - 1) * s, all.size());
        int to = Math.min(from + s, all.size());
        PageResult<T> r = new PageResult<>();
        r.total = all.size();
        r.page = p;
        r.size = s;
        r.records = all.subList(from, to);
        return r;
    }
}
`);

// ---------------------------------------------------------------- audit
w('service/AuditService.java', `
package ${P}.service;

import ${P}.entity.AuditLog;
import ${P}.mapper.AuditLogMapper;
import ${P}.security.LoginUser;
import ${P}.security.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** 审计日志：改分、改考勤、归档、上报、基础数据变更等关键操作统一落库 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogMapper auditLogMapper;

    public void record(String bizType, Long bizId, String action, String beforeJson, String afterJson, String detail) {
        LoginUser user = UserContext.get();
        AuditLog log = new AuditLog();
        log.setBizType(bizType);
        log.setBizId(bizId);
        log.setAction(action);
        if (user != null) {
            log.setOperatorId(user.getAccountId());
            log.setOperatorRole(user.getRole());
        }
        log.setBeforeJson(beforeJson);
        log.setAfterJson(afterJson);
        log.setDetail(detail);
        auditLogMapper.insert(log);
    }
}
`);

// ---------------------------------------------------------------- dto
w('dto/TermSaveRequest.java', `
package ${P}.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/** 新增 / 修改学期 */
@Data
public class TermSaveRequest {
    private Long id;
    @NotBlank(message = "学期编码不能为空")
    private String termCode;
    @NotBlank(message = "学年不能为空")
    private String schoolYear;
    @NotNull(message = "学期序号不能为空")
    private Integer termNo;
    @NotBlank(message = "学期名称不能为空")
    private String termName;
    @NotNull(message = "开始日期不能为空")
    private LocalDate startDate;
    @NotNull(message = "结束日期不能为空")
    private LocalDate endDate;
    @NotNull(message = "总周数不能为空")
    private Integer totalWeeks;
}
`);

w('dto/CalendarWeekRequest.java', `
package ${P}.dto;

import lombok.Data;

/** 修改某一周的周历类型（教学周 / 假期 / 考试周…） */
@Data
public class CalendarWeekRequest {
    private String weekType;
    private Integer holidayDays;
    private String note;
}
`);

w('dto/MajorSaveRequest.java', `
package ${P}.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 新增 / 修改专业 */
@Data
public class MajorSaveRequest {
    private Long id;
    @NotBlank(message = "专业编码不能为空")
    private String majorCode;
    @NotBlank(message = "专业名称不能为空")
    private String majorName;
    private String collegeCode;
    private String collegeName;
}
`);

w('dto/AdminClassSaveRequest.java', `
package ${P}.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 新增 / 修改行政班 */
@Data
public class AdminClassSaveRequest {
    private Long id;
    @NotBlank(message = "班级编码不能为空")
    private String classCode;
    @NotBlank(message = "班级名称不能为空")
    private String className;
    @NotNull(message = "所属专业不能为空")
    private Long majorId;
    @NotNull(message = "年级不能为空")
    private Integer gradeYear;
}
`);

w('dto/CounselorAssignRequest.java', `
package ${P}.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/** 辅导员分配：把若干行政班分配给某位导员（按学期） */
@Data
public class CounselorAssignRequest {
    @NotNull(message = "学期不能为空")
    private Long termId;
    @NotNull(message = "辅导员不能为空")
    private Long counselorId;
    @NotNull(message = "行政班不能为空")
    private List<Long> adminClassIds;
}
`);

// ---------------------------------------------------------------- vo
w('vo/TermVO.java', `
package ${P}.vo;

import lombok.Data;

import java.time.LocalDate;

/** 学期视图：带“是否当前学期”与周历情况 */
@Data
public class TermVO {
    private Long id;
    private String termCode;
    private String schoolYear;
    private Integer termNo;
    private String termName;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer totalWeeks;
    private boolean current;
    private String status;
    private Integer calendarWeeks;
}
`);

w('vo/CounselorClassVO.java', `
package ${P}.vo;

import lombok.Data;

/** 导员带班视图 */
@Data
public class CounselorClassVO {
    private Long id;
    private Long termId;
    private Long counselorId;
    private String counselorNo;
    private String counselorName;
    private Long adminClassId;
    private String classCode;
    private String className;
    private Integer gradeYear;
    private Integer studentCount;
    private Integer isPrimary;
}
`);

// ---------------------------------------------------------------- service: 学期
w('service/TermService.java', `
package ${P}.service;

import ${P}.common.BizException;
import ${P}.dto.TermSaveRequest;
import ${P}.entity.Term;
import ${P}.mapper.TermCalendarMapper;
import ${P}.mapper.TermMapper;
import ${P}.vo.TermVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/** 学期管理：全库时间轴 */
@Service
@RequiredArgsConstructor
public class TermService {

    private final TermMapper termMapper;
    private final TermCalendarMapper calendarMapper;
    private final AuditService auditService;

    public List<TermVO> list() {
        List<Term> terms = termMapper.selectList(new LambdaQueryWrapper<Term>().orderByDesc(Term::getStartDate));
        return terms.stream().map(this::toVO).sorted(Comparator.comparing(TermVO::getStartDate).reversed())
                .collect(Collectors.toList());
    }

    public TermVO current() {
        Term t = termMapper.selectOne(Wrappers.<Term>lambdaQuery().eq(Term::getIsCurrent, 1).last("limit 1"));
        return t == null ? null : toVO(t);
    }

    @Transactional
    public TermVO save(TermSaveRequest req) {
        if (req.getEndDate().isBefore(req.getStartDate())) {
            throw new BizException("结束日期不能早于开始日期");
        }
        Term t;
        boolean create = req.getId() == null;
        if (create) {
            if (termMapper.selectCount(Wrappers.<Term>lambdaQuery().eq(Term::getTermCode, req.getTermCode())) > 0) {
                throw new BizException("学期编码已存在：" + req.getTermCode());
            }
            t = new Term();
        } else {
            t = termMapper.selectById(req.getId());
            if (t == null) {
                throw new BizException("学期不存在");
            }
        }
        t.setTermCode(req.getTermCode());
        t.setSchoolYear(req.getSchoolYear());
        t.setTermNo(req.getTermNo());
        t.setTermName(req.getTermName());
        t.setStartDate(req.getStartDate());
        t.setEndDate(req.getEndDate());
        t.setTotalWeeks(req.getTotalWeeks());
        if (create) {
            t.setIsCurrent(0);
            t.setStatus("ACTIVE");
            termMapper.insert(t);
        } else {
            termMapper.updateById(t);
        }
        auditService.record("TERM", t.getId(), create ? "CREATE" : "UPDATE", null, null,
                (create ? "新增学期 " : "修改学期 ") + t.getTermName());
        return toVO(t);
    }

    @Transactional
    public void setCurrent(Long id) {
        Term t = termMapper.selectById(id);
        if (t == null) {
            throw new BizException("学期不存在");
        }
        termMapper.update(null, Wrappers.<Term>lambdaUpdate().set(Term::getIsCurrent, 0).eq(Term::getIsCurrent, 1));
        termMapper.update(null, Wrappers.<Term>lambdaUpdate().set(Term::getIsCurrent, 1).eq(Term::getId, id));
        auditService.record("TERM", id, "SET_CURRENT", null, null, "设为当前学期：" + t.getTermName());
    }

    private TermVO toVO(Term t) {
        TermVO vo = new TermVO();
        vo.setId(t.getId());
        vo.setTermCode(t.getTermCode());
        vo.setSchoolYear(t.getSchoolYear());
        vo.setTermNo(t.getTermNo());
        vo.setTermName(t.getTermName());
        vo.setStartDate(t.getStartDate());
        vo.setEndDate(t.getEndDate());
        vo.setTotalWeeks(t.getTotalWeeks());
        vo.setCurrent(t.getIsCurrent() != null && t.getIsCurrent() == 1);
        vo.setStatus(t.getStatus());
        vo.setCalendarWeeks(calendarMapper.selectCount(Wrappers.lambdaQuery(${P}.entity.TermCalendar.class)
                .eq(${P}.entity.TermCalendar::getTermId, t.getId())).intValue());
        return vo;
    }
}
`);

// ---------------------------------------------------------------- service: 周历
w('service/TermCalendarService.java', `
package ${P}.service;

import ${P}.common.BizException;
import ${P}.dto.CalendarWeekRequest;
import ${P}.entity.Term;
import ${P}.entity.TermCalendar;
import ${P}.mapper.TermCalendarMapper;
import ${P}.mapper.TermMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** 教学周历：按学期起止日期与总周数生成逐周记录，供“应完成课次”计算 */
@Service
@RequiredArgsConstructor
public class TermCalendarService {

    private final TermMapper termMapper;
    private final TermCalendarMapper calendarMapper;
    private final AuditService auditService;

    public List<TermCalendar> listByTerm(Long termId) {
        return calendarMapper.selectList(Wrappers.<TermCalendar>lambdaQuery()
                .eq(TermCalendar::getTermId, termId).orderByAsc(TermCalendar::getWeekNo));
    }

    /** 生成 / 重建周历（按 startDate + totalWeeks，一周 7 天） */
    @Transactional
    public int generate(Long termId, boolean force) {
        Term term = termMapper.selectById(termId);
        if (term == null) {
            throw new BizException("学期不存在");
        }
        long exists = calendarMapper.selectCount(Wrappers.<TermCalendar>lambdaQuery().eq(TermCalendar::getTermId, termId));
        if (exists > 0 && !force) {
            throw new BizException("该学期周历已存在，如需重建请选择强制重建");
        }
        if (exists > 0) {
            calendarMapper.delete(Wrappers.<TermCalendar>lambdaQuery().eq(TermCalendar::getTermId, termId));
        }
        int weeks = term.getTotalWeeks() == null ? 0 : term.getTotalWeeks();
        List<TermCalendar> list = new ArrayList<>();
        for (int i = 1; i <= weeks; i++) {
            TermCalendar c = new TermCalendar();
            c.setTermId(termId);
            c.setWeekNo(i);
            LocalDate start = term.getStartDate().plusDays(7L * (i - 1));
            c.setStartDate(start);
            c.setEndDate(start.plusDays(6));
            c.setWeekType("TEACHING");
            c.setHolidayDays(0);
            calendarMapper.insert(c);
            list.add(c);
        }
        auditService.record("TERM_CALENDAR", termId, "GENERATE", null, null,
                "生成周历 " + list.size() + " 周（学期：" + term.getTermName() + "）");
        return list.size();
    }

    @Transactional
    public TermCalendar updateWeek(Long weekId, CalendarWeekRequest req) {
        TermCalendar c = calendarMapper.selectById(weekId);
        if (c == null) {
            throw new BizException("周次不存在");
        }
        if (req.getWeekType() != null) {
            c.setWeekType(req.getWeekType());
        }
        if (req.getHolidayDays() != null) {
            c.setHolidayDays(req.getHolidayDays());
        }
        if (req.getNote() != null) {
            c.setNote(req.getNote());
        }
        calendarMapper.updateById(c);
        auditService.record("TERM_CALENDAR", c.getTermId(), "UPDATE_WEEK", null, null,
                "修改第 " + c.getWeekNo() + " 周：" + c.getWeekType() + "，假日 " + c.getHolidayDays() + " 天");
        return c;
    }
}
`);

// ---------------------------------------------------------------- service: 专业 / 行政班
w('service/MajorService.java', `
package ${P}.service;

import ${P}.common.BizException;
import ${P}.dto.MajorSaveRequest;
import ${P}.entity.Major;
import ${P}.mapper.MajorMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 专业（含学院）维护 */
@Service
@RequiredArgsConstructor
public class MajorService {

    private final MajorMapper majorMapper;
    private final AuditService auditService;

    public List<Major> list() {
        return majorMapper.selectList(Wrappers.<Major>lambdaQuery().orderByAsc(Major::getMajorCode));
    }

    @Transactional
    public Major save(MajorSaveRequest req) {
        Major m;
        boolean create = req.getId() == null;
        if (create) {
            if (majorMapper.selectCount(Wrappers.<Major>lambdaQuery().eq(Major::getMajorCode, req.getMajorCode())) > 0) {
                throw new BizException("专业编码已存在：" + req.getMajorCode());
            }
            m = new Major();
        } else {
            m = majorMapper.selectById(req.getId());
            if (m == null) {
                throw new BizException("专业不存在");
            }
        }
        m.setMajorCode(req.getMajorCode());
        m.setMajorName(req.getMajorName());
        m.setCollegeCode(req.getCollegeCode());
        m.setCollegeName(req.getCollegeName());
        if (create) {
            m.setStatus("ACTIVE");
            majorMapper.insert(m);
        } else {
            majorMapper.updateById(m);
        }
        auditService.record("MAJOR", m.getId(), create ? "CREATE" : "UPDATE", null, null,
                (create ? "新增专业 " : "修改专业 ") + m.getMajorName());
        return m;
    }
}
`);

w('service/AdminClassService.java', `
package ${P}.service;

import ${P}.common.BizException;
import ${P}.dto.AdminClassSaveRequest;
import ${P}.entity.AdministrativeClass;
import ${P}.entity.Major;
import ${P}.mapper.AdministrativeClassMapper;
import ${P}.mapper.MajorMapper;
import ${P}.mapper.StudentMapper;
import ${P}.entity.Student;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 行政班维护（导员按行政班管理学生） */
@Service
@RequiredArgsConstructor
public class AdminClassService {

    private final AdministrativeClassMapper classMapper;
    private final MajorMapper majorMapper;
    private final StudentMapper studentMapper;
    private final AuditService auditService;

    /** 班级列表，附带专业名与学生人数 */
    public List<Map<String, Object>> list(Long majorId, Integer gradeYear) {
        List<AdministrativeClass> classes = classMapper.selectList(Wrappers.<AdministrativeClass>lambdaQuery()
                .eq(majorId != null, AdministrativeClass::getMajorId, majorId)
                .eq(gradeYear != null, AdministrativeClass::getGradeYear, gradeYear)
                .orderByAsc(AdministrativeClass::getGradeYear)
                .orderByAsc(AdministrativeClass::getClassCode));
        Map<Long, String> majorNames = majorMapper.selectList(null).stream()
                .collect(Collectors.toMap(Major::getId, Major::getMajorName, (a, b) -> a));
        Map<Long, Long> counts = studentMapper.selectList(Wrappers.<Student>query()
                        .select("id", "admin_class_id")).stream()
                .filter(s -> s.getAdminClassId() != null)
                .collect(Collectors.groupingBy(Student::getAdminClassId, Collectors.counting()));
        List<Map<String, Object>> out = new ArrayList<>();
        for (AdministrativeClass c : classes) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("classCode", c.getClassCode());
            m.put("className", c.getClassName());
            m.put("majorId", c.getMajorId());
            m.put("majorName", majorNames.getOrDefault(c.getMajorId(), ""));
            m.put("gradeYear", c.getGradeYear());
            m.put("status", c.getStatus());
            m.put("studentCount", counts.getOrDefault(c.getId(), 0L));
            out.add(m);
        }
        return out;
    }

    @Transactional
    public AdministrativeClass save(AdminClassSaveRequest req) {
        if (majorMapper.selectById(req.getMajorId()) == null) {
            throw new BizException("所选专业不存在");
        }
        AdministrativeClass c;
        boolean create = req.getId() == null;
        if (create) {
            if (classMapper.selectCount(Wrappers.<AdministrativeClass>lambdaQuery()
                    .eq(AdministrativeClass::getClassCode, req.getClassCode())) > 0) {
                throw new BizException("班级编码已存在：" + req.getClassCode());
            }
            c = new AdministrativeClass();
        } else {
            c = classMapper.selectById(req.getId());
            if (c == null) {
                throw new BizException("班级不存在");
            }
        }
        c.setClassCode(req.getClassCode());
        c.setClassName(req.getClassName());
        c.setMajorId(req.getMajorId());
        c.setGradeYear(req.getGradeYear());
        if (create) {
            c.setStatus("ACTIVE");
            classMapper.insert(c);
        } else {
            classMapper.updateById(c);
        }
        auditService.record("ADMIN_CLASS", c.getId(), create ? "CREATE" : "UPDATE", null, null,
                (create ? "新增行政班 " : "修改行政班 ") + c.getClassName());
        return c;
    }
}
`);

// ---------------------------------------------------------------- service: 辅导员分配
w('service/CounselorAssignService.java', `
package ${P}.service;

import ${P}.common.BizException;
import ${P}.dto.CounselorAssignRequest;
import ${P}.entity.AdministrativeClass;
import ${P}.entity.Counselor;
import ${P}.entity.CounselorClass;
import ${P}.entity.Student;
import ${P}.mapper.AdministrativeClassMapper;
import ${P}.mapper.CounselorClassMapper;
import ${P}.mapper.CounselorMapper;
import ${P}.mapper.StudentMapper;
import ${P}.vo.CounselorClassVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 辅导员分配：按学期把行政班分配给导员（换人留痕，旧关系保留） */
@Service
@RequiredArgsConstructor
public class CounselorAssignService {

    private final CounselorClassMapper counselorClassMapper;
    private final CounselorMapper counselorMapper;
    private final AdministrativeClassMapper classMapper;
    private final StudentMapper studentMapper;
    private final AuditService auditService;

    public List<CounselorClassVO> list(Long termId, Long counselorId) {
        List<CounselorClass> rows = counselorClassMapper.selectList(Wrappers.<CounselorClass>lambdaQuery()
                .eq(termId != null, CounselorClass::getTermId, termId)
                .eq(counselorId != null, CounselorClass::getCounselorId, counselorId)
                .orderByAsc(CounselorClass::getCounselorId));
        if (rows.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, Counselor> counselors = counselorMapper.selectBatchIds(
                        rows.stream().map(CounselorClass::getCounselorId).distinct().collect(Collectors.toList()))
                .stream().collect(Collectors.toMap(Counselor::getId, c -> c, (a, b) -> a));
        Map<Long, AdministrativeClass> classes = classMapper.selectBatchIds(
                        rows.stream().map(CounselorClass::getAdminClassId).distinct().collect(Collectors.toList()))
                .stream().collect(Collectors.toMap(AdministrativeClass::getId, c -> c, (a, b) -> a));
        Map<Long, Long> counts = studentMapper.selectList(Wrappers.<Student>query()
                        .select("id", "admin_class_id")).stream()
                .filter(s -> s.getAdminClassId() != null)
                .collect(Collectors.groupingBy(Student::getAdminClassId, Collectors.counting()));
        List<CounselorClassVO> out = new ArrayList<>();
        for (CounselorClass r : rows) {
            CounselorClassVO vo = new CounselorClassVO();
            vo.setId(r.getId());
            vo.setTermId(r.getTermId());
            vo.setCounselorId(r.getCounselorId());
            Counselor c = counselors.get(r.getCounselorId());
            if (c != null) {
                vo.setCounselorNo(c.getCounselorNo());
                vo.setCounselorName(c.getCounselorName());
            }
            vo.setAdminClassId(r.getAdminClassId());
            AdministrativeClass ac = classes.get(r.getAdminClassId());
            if (ac != null) {
                vo.setClassCode(ac.getClassCode());
                vo.setClassName(ac.getClassName());
                vo.setGradeYear(ac.getGradeYear());
            }
            vo.setStudentCount(counts.getOrDefault(r.getAdminClassId(), 0L).intValue());
            vo.setIsPrimary(r.getIsPrimary());
            out.add(vo);
        }
        return out;
    }

    @Transactional
    public int assign(CounselorAssignRequest req) {
        if (counselorMapper.selectById(req.getCounselorId()) == null) {
            throw new BizException("辅导员不存在");
        }
        int created = 0;
        for (Long classId : req.getAdminClassIds()) {
            if (classMapper.selectById(classId) == null) {
                throw new BizException("行政班不存在：" + classId);
            }
            long exists = counselorClassMapper.selectCount(Wrappers.<CounselorClass>lambdaQuery()
                    .eq(CounselorClass::getTermId, req.getTermId())
                    .eq(CounselorClass::getAdminClassId, classId));
            if (exists > 0) {
                continue;
            }
            CounselorClass cc = new CounselorClass();
            cc.setCounselorId(req.getCounselorId());
            cc.setAdminClassId(classId);
            cc.setTermId(req.getTermId());
            cc.setIsPrimary(1);
            counselorClassMapper.insert(cc);
            created++;
        }
        auditService.record("COUNSELOR_CLASS", req.getCounselorId(), "ASSIGN", null, null,
                "学期 " + req.getTermId() + " 新增带班 " + created + " 个");
        return created;
    }

    @Transactional
    public void remove(Long id) {
        CounselorClass cc = counselorClassMapper.selectById(id);
        if (cc == null) {
            throw new BizException("记录不存在");
        }
        counselorClassMapper.deleteById(id);
        auditService.record("COUNSELOR_CLASS", cc.getCounselorId(), "UNASSIGN", null, null,
                "解除带班：行政班 " + cc.getAdminClassId() + "（学期 " + cc.getTermId() + "）");
    }
}
`);

console.log('基础数据模块：Java 代码生成完成');

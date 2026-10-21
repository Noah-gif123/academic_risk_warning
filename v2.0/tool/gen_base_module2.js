/* gen_base_module2.js — 控制器 + 鉴权拦截器（加角色校验） */
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

// ---------------------------------------------------------------- 学期与周历
w('controller/admin/AdminTermController.java', `
package ${P}.controller.admin;

import ${P}.common.Result;
import ${P}.dto.CalendarWeekRequest;
import ${P}.dto.TermSaveRequest;
import ${P}.entity.TermCalendar;
import ${P}.security.RequireRole;
import ${P}.service.TermCalendarService;
import ${P}.service.TermService;
import ${P}.vo.TermVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 教务端 · 学期与教学周历 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@RequireRole("ADMIN")
public class AdminTermController {

    private final TermService termService;
    private final TermCalendarService calendarService;

    @GetMapping("/terms")
    public Result<List<TermVO>> list() {
        return Result.ok(termService.list());
    }

    @GetMapping("/terms/current")
    public Result<TermVO> current() {
        return Result.ok(termService.current());
    }

    @PostMapping("/terms")
    public Result<TermVO> save(@Valid @RequestBody TermSaveRequest request) {
        return Result.ok(termService.save(request));
    }

    @PostMapping("/terms/{id}/current")
    public Result<Void> setCurrent(@PathVariable Long id) {
        termService.setCurrent(id);
        return Result.ok();
    }

    @GetMapping("/terms/{id}/calendar")
    public Result<List<TermCalendar>> calendar(@PathVariable Long id) {
        return Result.ok(calendarService.listByTerm(id));
    }

    @PostMapping("/terms/{id}/calendar/generate")
    public Result<Integer> generate(@PathVariable Long id, @RequestParam(defaultValue = "false") boolean force) {
        return Result.ok(calendarService.generate(id, force));
    }

    @PostMapping("/calendar/{weekId}")
    public Result<TermCalendar> updateWeek(@PathVariable Long weekId, @RequestBody CalendarWeekRequest request) {
        return Result.ok(calendarService.updateWeek(weekId, request));
    }
}
`);

// ---------------------------------------------------------------- 专业
w('controller/admin/AdminMajorController.java', `
package ${P}.controller.admin;

import ${P}.common.Result;
import ${P}.dto.MajorSaveRequest;
import ${P}.entity.Major;
import ${P}.security.RequireRole;
import ${P}.service.MajorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 教务端 · 专业维护 */
@RestController
@RequestMapping("/api/admin/majors")
@RequiredArgsConstructor
@RequireRole("ADMIN")
public class AdminMajorController {

    private final MajorService majorService;

    @GetMapping
    public Result<List<Major>> list() {
        return Result.ok(majorService.list());
    }

    @PostMapping
    public Result<Major> save(@Valid @RequestBody MajorSaveRequest request) {
        return Result.ok(majorService.save(request));
    }
}
`);

// ---------------------------------------------------------------- 行政班
w('controller/admin/AdminClassController.java', `
package ${P}.controller.admin;

import ${P}.common.Result;
import ${P}.dto.AdminClassSaveRequest;
import ${P}.entity.AdministrativeClass;
import ${P}.security.RequireRole;
import ${P}.service.AdminClassService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 教务端 · 行政班维护 */
@RestController
@RequestMapping("/api/admin/classes")
@RequiredArgsConstructor
@RequireRole("ADMIN")
public class AdminClassController {

    private final AdminClassService classService;

    @GetMapping
    public Result<List<Map<String, Object>>> list(@RequestParam(required = false) Long majorId,
                                                 @RequestParam(required = false) Integer gradeYear) {
        return Result.ok(classService.list(majorId, gradeYear));
    }

    @PostMapping
    public Result<AdministrativeClass> save(@Valid @RequestBody AdminClassSaveRequest request) {
        return Result.ok(classService.save(request));
    }
}
`);

// ---------------------------------------------------------------- 辅导员分配
w('controller/admin/AdminCounselorController.java', `
package ${P}.controller.admin;

import ${P}.common.Result;
import ${P}.dto.CounselorAssignRequest;
import ${P}.entity.Counselor;
import ${P}.mapper.CounselorMapper;
import ${P}.security.RequireRole;
import ${P}.service.CounselorAssignService;
import ${P}.vo.CounselorClassVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 教务端 · 辅导员（导员）分配 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@RequireRole("ADMIN")
public class AdminCounselorController {

    private final CounselorAssignService assignService;
    private final CounselorMapper counselorMapper;

    /** 导员下拉列表 */
    @GetMapping("/counselors")
    public Result<List<Counselor>> counselors() {
        return Result.ok(counselorMapper.selectList(
                Wrappers.<Counselor>lambdaQuery().eq(Counselor::getStatus, "ACTIVE").orderByAsc(Counselor::getCounselorNo)));
    }

    /** 带班关系列表 */
    @GetMapping("/counselor-assignments")
    public Result<List<CounselorClassVO>> list(@RequestParam(required = false) Long termId,
                                               @RequestParam(required = false) Long counselorId) {
        return Result.ok(assignService.list(termId, counselorId));
    }

    /** 批量分配 */
    @PostMapping("/counselor-assignments")
    public Result<Integer> assign(@Valid @RequestBody CounselorAssignRequest request) {
        return Result.ok(assignService.assign(request));
    }

    /** 解除某条带班关系 */
    @DeleteMapping("/counselor-assignments/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        assignService.remove(id);
        return Result.ok();
    }
}
`);

// ---------------------------------------------------------------- 鉴权拦截器（加角色校验）
w('security/AuthInterceptor.java', `
package ${P}.security;

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
        response.getWriter().write("{\\"code\\":" + code + ",\\"message\\":\\"" + message + "\\",\\"data\\":null}");
    }
}
`);

console.log('基础数据模块：控制器与鉴权拦截器生成完成');

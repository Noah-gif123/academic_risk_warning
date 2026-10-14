package com.example.academic_risk_warning.controller;

import com.example.academic_risk_warning.service.StudentLoginService;
import com.example.academic_risk_warning.service.StudentProfileService;
import com.example.academic_risk_warning.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 学生三维学情画像读取接口。
 *
 * <ul>
 *   <li>GET /api/profile/my?courseId=          学生查看自己的画像（按 Token 解析出的用户ID）</li>
 *   <li>GET /api/profile/student/{id}?courseId= 教师查看指定学生画像（学生端页面不使用该接口）</li>
 * </ul>
 *
 * 画像内容由智能体流水线（/api/agent/full-assessment/{studentId}）生成，本接口只读。
 *
 * <p>权限说明：当前 Token 只保存数值 ID、不区分角色（教师/学生 ID 存在同号），
 * 无法在本接口内可靠判定"调用者是教师还是学生"，因此权限口径与
 * {@code /api/alert/student/{id}/radar} 保持一致：任何已登录用户均可读取学生画像。
 * 待认证模型（Token 携带角色）重构后再收紧此处。
 */
@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private static final Logger log = LoggerFactory.getLogger(ProfileController.class);

    private final StudentProfileService studentProfileService;
    private final StudentLoginService studentLoginService;
    private final TokenService tokenService;

    public ProfileController(StudentProfileService studentProfileService,
                             StudentLoginService studentLoginService,
                             TokenService tokenService) {
        this.studentProfileService = studentProfileService;
        this.studentLoginService = studentLoginService;
        this.tokenService = tokenService;
    }

    /** 学生查看自己的三维画像 */
    @GetMapping("/my")
    public Map<String, Object> myProfile(@RequestParam(required = false) Long courseId,
                                         HttpServletRequest request) {
        Long loginId = resolveLoginId(request);
        if (loginId == null) {
            return error("未登录或 Token 已失效");
        }
        if (studentLoginService.getStudentById(loginId) == null) {
            return error("当前账号不是学生，无法查看个人三维画像");
        }
        try {
            return success(studentProfileService.getProfile(loginId, courseId));
        } catch (Exception e) {
            log.error("[ProfileAPI] 查询本人画像失败，studentId={}", loginId, e);
            return error("查询三维画像失败: " + e.getMessage());
        }
    }

    /** 教师查看指定学生的三维画像 */
    @GetMapping("/student/{studentId}")
    public Map<String, Object> studentProfile(@PathVariable Long studentId,
                                              @RequestParam(required = false) Long courseId,
                                              HttpServletRequest request) {
        if (resolveLoginId(request) == null) {
            return error("未登录或 Token 已失效");
        }
        try {
            return success(studentProfileService.getProfile(studentId, courseId));
        } catch (Exception e) {
            log.error("[ProfileAPI] 查询学生画像失败，studentId={}", studentId, e);
            return error("查询三维画像失败: " + e.getMessage());
        }
    }

    private Long resolveLoginId(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth == null || auth.isBlank()) {
            return null;
        }
        if (auth.startsWith("Bearer ")) {
            auth = auth.substring(7).trim();
        }
        return tokenService.validateAndGetTeacherId(auth);
    }

    private Map<String, Object> success(Object data) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("message", "查询成功");
        result.put("data", data);
        return result;
    }

    private Map<String, Object> error(String message) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", false);
        result.put("message", message);
        return result;
    }
}

package com.example.academic_risk_warning.controller.admin;

import com.example.academic_risk_warning.common.Result;
import com.example.academic_risk_warning.dto.CounselorAssignRequest;
import com.example.academic_risk_warning.entity.Counselor;
import com.example.academic_risk_warning.mapper.CounselorMapper;
import com.example.academic_risk_warning.security.RequireRole;
import com.example.academic_risk_warning.service.CounselorAssignService;
import com.example.academic_risk_warning.vo.CounselorClassVO;
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

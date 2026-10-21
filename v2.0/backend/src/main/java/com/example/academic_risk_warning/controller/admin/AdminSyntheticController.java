package com.example.academic_risk_warning.controller.admin;

import com.example.academic_risk_warning.common.Result;
import com.example.academic_risk_warning.dto.SyntheticGenerateRequest;
import com.example.academic_risk_warning.security.RequireRole;
import com.example.academic_risk_warning.service.SyntheticDataService;
import com.example.academic_risk_warning.vo.SyntheticResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** 教务端 · 合成数据生成器（联调与算法验证用；只操作 SYN- 开头的独立学期） */
@RestController
@RequestMapping("/api/admin/synthetic")
@RequiredArgsConstructor
@RequireRole("ADMIN")
public class AdminSyntheticController {

    private final SyntheticDataService syntheticDataService;

    @PostMapping("/generate")
    public Result<SyntheticResultVO> generate(@RequestBody(required = false) SyntheticGenerateRequest request) {
        return Result.ok(syntheticDataService.generate(request));
    }

    @PostMapping("/clear")
    public Result<Void> clear(@RequestParam Long termId) {
        syntheticDataService.clearTerm(termId);
        return Result.ok();
    }
}

package com.example.academic_risk_warning.controller.admin;

import com.example.academic_risk_warning.common.Result;
import com.example.academic_risk_warning.dto.MajorSaveRequest;
import com.example.academic_risk_warning.entity.Major;
import com.example.academic_risk_warning.security.RequireRole;
import com.example.academic_risk_warning.service.MajorService;
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

package com.example.academic_risk_warning.controller.admin;

import com.example.academic_risk_warning.common.Result;
import com.example.academic_risk_warning.dto.AdminClassSaveRequest;
import com.example.academic_risk_warning.entity.AdministrativeClass;
import com.example.academic_risk_warning.security.RequireRole;
import com.example.academic_risk_warning.service.AdminClassService;
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

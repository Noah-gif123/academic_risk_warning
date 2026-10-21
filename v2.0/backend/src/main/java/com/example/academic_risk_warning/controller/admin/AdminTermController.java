package com.example.academic_risk_warning.controller.admin;

import com.example.academic_risk_warning.common.Result;
import com.example.academic_risk_warning.dto.CalendarWeekRequest;
import com.example.academic_risk_warning.dto.TermSaveRequest;
import com.example.academic_risk_warning.entity.TermCalendar;
import com.example.academic_risk_warning.security.RequireRole;
import com.example.academic_risk_warning.service.TermCalendarService;
import com.example.academic_risk_warning.service.TermService;
import com.example.academic_risk_warning.vo.TermVO;
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

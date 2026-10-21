package com.example.academic_risk_warning.controller.counselor;

import com.example.academic_risk_warning.common.BizException;
import com.example.academic_risk_warning.common.Result;
import com.example.academic_risk_warning.dto.InterventionSaveRequest;
import com.example.academic_risk_warning.dto.QuestionnaireSaveRequest;
import com.example.academic_risk_warning.dto.ReferRequest;
import com.example.academic_risk_warning.entity.Term;
import com.example.academic_risk_warning.security.LoginUser;
import com.example.academic_risk_warning.security.RequireRole;
import com.example.academic_risk_warning.security.UserContext;
import com.example.academic_risk_warning.service.CounselorPortalService;
import com.example.academic_risk_warning.vo.CounselorNoticeVO;
import com.example.academic_risk_warning.vo.CounselorOverviewVO;
import com.example.academic_risk_warning.vo.CounselorProfileVO;
import com.example.academic_risk_warning.vo.CounselorStudentDetailVO;
import com.example.academic_risk_warning.vo.CounselorStudentVO;
import com.example.academic_risk_warning.vo.QuestionnaireVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 导员端 · 看板 / 名单与单科详情 / 干预记录 / 问卷 / 通知 */
@RestController
@RequestMapping("/api/counselor")
@RequiredArgsConstructor
@RequireRole("COUNSELOR")
public class CounselorPortalController {

    private final CounselorPortalService counselorService;

    private Long me() {
        LoginUser user = UserContext.require();
        if (user.getRefId() == null) {
            throw new BizException("当前账号没有导员档案");
        }
        return user.getRefId();
    }

    @GetMapping("/profile")
    public Result<CounselorProfileVO> profile() {
        return Result.ok(counselorService.profile(me()));
    }

    @GetMapping("/terms")
    public Result<List<Term>> terms() {
        return Result.ok(counselorService.terms());
    }

    @GetMapping("/overview")
    public Result<CounselorOverviewVO> overview(@RequestParam(required = false) Long termId) {
        return Result.ok(counselorService.overview(me(), termId));
    }

    @GetMapping("/students")
    public Result<List<CounselorStudentVO>> students(@RequestParam(required = false) Long adminClassId,
                                                     @RequestParam(required = false) Long termId) {
        return Result.ok(counselorService.students(me(), adminClassId, termId));
    }

    @GetMapping("/students/{studentId}")
    public Result<CounselorStudentDetailVO> detail(@PathVariable Long studentId) {
        return Result.ok(counselorService.studentDetail(me(), studentId));
    }

    @GetMapping("/intervention-records")
    public Result<java.util.List<java.util.Map<String, Object>>> interventionRecords(
            @RequestParam(required = false) Long adminClassId) {
        return Result.ok(counselorService.allInterventions(me(), adminClassId));
    }

    @PostMapping("/interventions")
    public Result<Long> addIntervention(@Valid @RequestBody InterventionSaveRequest request) {
        return Result.ok(counselorService.addIntervention(me(), request));
    }

    @PostMapping("/interventions/{id}/refer")
    public Result<Void> refer(@PathVariable Long id, @RequestBody(required = false) ReferRequest request) {
        counselorService.referIntervention(me(), id, request == null ? null : request.getNote());
        return Result.ok();
    }

    @PostMapping("/interventions/{id}/terminate")
    public Result<Void> terminate(@PathVariable Long id, @RequestBody(required = false) ReferRequest request) {
        counselorService.terminateIntervention(me(), id, request == null ? null : request.getReason());
        return Result.ok();
    }

    @GetMapping("/offering-options")
    public Result<java.util.List<java.util.Map<String, Object>>> offeringOptions(
            @RequestParam(required = false) Long termId) {
        return Result.ok(counselorService.offeringOptions(me(), termId));
    }

    @GetMapping("/questionnaires")
    public Result<List<QuestionnaireVO>> questionnaires(@RequestParam(required = false) Long termId) {
        return Result.ok(counselorService.questionnaires(me(), termId));
    }

    @PostMapping("/questionnaires")
    public Result<QuestionnaireVO> publish(@Valid @RequestBody QuestionnaireSaveRequest request) {
        return Result.ok(counselorService.publishQuestionnaire(me(), request));
    }

    @GetMapping("/notices")
    public Result<List<CounselorNoticeVO>> notices() {
        return Result.ok(counselorService.notices(me()));
    }

    @PostMapping("/notices/{id}/read")
    public Result<Void> read(@PathVariable Long id) {
        counselorService.markRead(me(), id);
        return Result.ok();
    }
}

/* gen_counselor_controller.js — 1-14 导员端控制器 */
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

w('controller/counselor/CounselorPortalController.java', `
package ${P}.controller.counselor;

import ${P}.common.BizException;
import ${P}.common.Result;
import ${P}.dto.InterventionSaveRequest;
import ${P}.dto.QuestionnaireSaveRequest;
import ${P}.dto.ReferRequest;
import ${P}.entity.Term;
import ${P}.security.LoginUser;
import ${P}.security.RequireRole;
import ${P}.security.UserContext;
import ${P}.service.CounselorPortalService;
import ${P}.vo.CounselorNoticeVO;
import ${P}.vo.CounselorOverviewVO;
import ${P}.vo.CounselorProfileVO;
import ${P}.vo.CounselorStudentDetailVO;
import ${P}.vo.CounselorStudentVO;
import ${P}.vo.QuestionnaireVO;
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
`);

console.log('导员端模块：控制器生成完成');

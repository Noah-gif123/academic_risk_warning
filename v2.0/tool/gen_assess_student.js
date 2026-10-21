/* gen_assess_student.js — 1-7 学生端：我的测验与作业 / 在线作答 / 交卷自动判分 / 结果与答案可见性 */
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

w('service/StudentPaperService.java', `
package ${P}.service;

import ${P}.common.BizException;
import ${P}.dto.AnswerSubmitDTO;
import ${P}.dto.PaperSubmitRequest;
import ${P}.dto.QuestionDTO;
import ${P}.entity.Course;
import ${P}.entity.CourseOffering;
import ${P}.entity.Enrollment;
import ${P}.entity.Paper;
import ${P}.entity.Question;
import ${P}.entity.Student;
import ${P}.entity.Submission;
import ${P}.entity.SubmissionAnswer;
import ${P}.entity.Teacher;
import ${P}.entity.TeachingClass;
import ${P}.mapper.CourseMapper;
import ${P}.mapper.CourseOfferingMapper;
import ${P}.mapper.EnrollmentMapper;
import ${P}.mapper.PaperMapper;
import ${P}.mapper.QuestionMapper;
import ${P}.mapper.StudentMapper;
import ${P}.mapper.SubmissionAnswerMapper;
import ${P}.mapper.SubmissionMapper;
import ${P}.mapper.TeacherMapper;
import ${P}.mapper.TeachingClassMapper;
import ${P}.util.GradeEngine;
import ${P}.vo.StudentPaperVO;
import ${P}.vo.SubmissionAnswerVO;
import ${P}.vo.SubmissionVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 学生端测验与作业：
 * ① 只显示已发布（含已截止）的卷子；② 到时自动截止，不允许迟交、不允许教师代交；
 * ③ 交卷即对客观题判分，简答题等教师批阅；④ 分数、试卷、答案三个开关按试卷配置裁剪。
 */
@Service
@RequiredArgsConstructor
public class StudentPaperService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final PaperMapper paperMapper;
    private final QuestionMapper questionMapper;
    private final SubmissionMapper submissionMapper;
    private final SubmissionAnswerMapper answerMapper;
    private final EnrollmentMapper enrollmentMapper;
    private final StudentMapper studentMapper;
    private final TeachingClassMapper classMapper;
    private final CourseOfferingMapper offeringMapper;
    private final CourseMapper courseMapper;
    private final TeacherMapper teacherMapper;

    // ============================================================ 我的测验与作业
    public List<StudentPaperVO> list(Long studentId) {
        List<Enrollment> ens = enrollmentMapper.selectList(Wrappers.<Enrollment>lambdaQuery()
                .eq(Enrollment::getStudentId, studentId));
        if (ens.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> classIds = ens.stream().map(Enrollment::getTeachingClassId).distinct()
                .collect(Collectors.toList());
        List<Paper> papers = paperMapper.selectList(Wrappers.<Paper>lambdaQuery()
                .in(Paper::getTeachingClassId, classIds)
                .in(Paper::getStatus, List.of("PUBLISHED", "CLOSED"))
                .orderByDesc(Paper::getId));
        Map<Long, TeachingClass> classes = new LinkedHashMap<>();
        for (TeachingClass c : classMapper.selectBatchIds(classIds)) {
            classes.put(c.getId(), c);
        }
        Map<Long, Paper> paperById = papers.stream()
                .collect(Collectors.toMap(Paper::getId, p -> p, (a, b) -> a));
        Map<Long, Submission> mySubs = new LinkedHashMap<>();
        if (!paperById.isEmpty()) {
            for (Submission s : submissionMapper.selectList(Wrappers.<Submission>lambdaQuery()
                    .eq(Submission::getStudentId, studentId)
                    .in(Submission::getPaperId, paperById.keySet()))) {
                mySubs.put(s.getPaperId(), s);
            }
        }
        List<StudentPaperVO> out = new ArrayList<>();
        for (Paper p : papers) {
            StudentPaperVO v = baseVO(p, classes.get(p.getTeachingClassId()), null);
            Submission s = mySubs.get(p.getId());
            applyMine(v, p, s);
            LocalDateTime nowList = LocalDateTime.now();
            v.setCanAnswer(canAnswer(p, s, nowList));
            v.setReason(reason(p, s, nowList));
            out.add(v);
        }
        return out;
    }

    // ============================================================ 取卷作答
    public StudentPaperVO get(Long paperId, Long studentId) {
        Paper p = paperMapper.selectById(paperId);
        if (p == null) {
            throw new BizException("试卷不存在");
        }
        requireEnrollment(p, studentId);
        if (!"PUBLISHED".equals(p.getStatus()) && !"CLOSED".equals(p.getStatus())) {
            throw new BizException("该卷尚未发布");
        }
        TeachingClass tc = classMapper.selectById(p.getTeachingClassId());
        List<Question> qs = questionsOf(paperId);
        StudentPaperVO v = baseVO(p, tc, qs);
        Submission s = submissionMapper.selectOne(Wrappers.<Submission>lambdaQuery()
                .eq(Submission::getPaperId, paperId)
                .eq(Submission::getStudentId, studentId).last("limit 1"));
        applyMine(v, p, s);
        LocalDateTime now = LocalDateTime.now();
        v.setCanAnswer(canAnswer(p, s, now));
        v.setReason(reason(p, s, now));
        return v;
    }

    private void applyMine(StudentPaperVO v, Paper p, Submission s) {
        if (s == null) {
            v.setMyStatus("NOT_STARTED");
            return;
        }
        v.setMyStatus(s.getSubmitStatus());
        boolean show = p.getShowScore() == null || p.getShowScore() == 1;
        if (show) {
            v.setMyScore(s.getTotalScore());
        }
    }

    private boolean canAnswer(Paper p, Submission s, LocalDateTime now) {
        if (s != null) {
            return false;
        }
        if (!"PUBLISHED".equals(p.getStatus())) {
            return false;
        }
        if (p.getStartTime() != null && now.isBefore(p.getStartTime())) {
            return false;
        }
        return p.getEndTime() == null || !now.isAfter(p.getEndTime());
    }

    private String reason(Paper p, Submission s, LocalDateTime now) {
        if (s != null) {
            return "ABSENT".equals(s.getSubmitStatus()) ? "缺考（记 0 分）" : "已交卷";
        }
        if (!"PUBLISHED".equals(p.getStatus()) && !"CLOSED".equals(p.getStatus())) {
            return "未发布";
        }
        if (p.getStartTime() != null && now.isBefore(p.getStartTime())) {
            return "未到开始时间";
        }
        if (p.getEndTime() != null && now.isAfter(p.getEndTime())) {
            return "已截止（不允许补交）";
        }
        return null;
    }

    // ============================================================ 交卷
    @Transactional
    public SubmissionVO submit(Long paperId, PaperSubmitRequest req, Long studentId) {
        Paper p = paperMapper.selectById(paperId);
        if (p == null) {
            throw new BizException("试卷不存在");
        }
        Enrollment e = requireEnrollment(p, studentId);
        LocalDateTime now = LocalDateTime.now();
        if (!"PUBLISHED".equals(p.getStatus())) {
            throw new BizException("CLOSED".equals(p.getStatus())
                    ? "该卷已截止，不允许补交" : "该卷尚未发布，不能作答");
        }
        if (p.getStartTime() != null && now.isBefore(p.getStartTime())) {
            throw new BizException("还没到开始时间");
        }
        if (p.getEndTime() != null && now.isAfter(p.getEndTime())) {
            throw new BizException("已过截止时间，不允许补交");
        }
        long exist = submissionMapper.selectCount(Wrappers.<Submission>lambdaQuery()
                .eq(Submission::getPaperId, paperId)
                .eq(Submission::getStudentId, studentId));
        if (exist > 0) {
            throw new BizException("你已经交过这份卷子，不能重复提交");
        }
        List<Question> qs = questionsOf(paperId);
        if (qs.isEmpty()) {
            throw new BizException("这份卷子还没有题目");
        }

        LocalDateTime startedAt = req.getStartedAt() == null ? now : req.getStartedAt();
        if (startedAt.isAfter(now)) {
            startedAt = now;
        }
        int seconds = (int) Math.max(0, Duration.between(startedAt, now).getSeconds());
        String remark = null;
        if (p.getDurationMinutes() != null && p.getDurationMinutes() > 0) {
            int limit = p.getDurationMinutes() * 60;
            if (seconds > limit) {
                seconds = limit;
                remark = "超出限时，按自动交卷处理";
            }
        }

        Submission s = new Submission();
        s.setPaperId(paperId);
        s.setTeachingClassId(p.getTeachingClassId());
        s.setTermId(p.getTermId());
        s.setStudentId(studentId);
        s.setEnrollmentId(e.getId());
        s.setSubmitStatus("SUBMITTED");
        s.setMarkedStatus("NOT_MARKED");
        s.setObjectiveScore(ZERO);
        s.setSubjectiveScore(ZERO);
        s.setStartTime(startedAt);
        s.setSubmitTime(now);
        s.setDurationSeconds(seconds);
        s.setIsAbsent(0);
        s.setRemark(remark);
        submissionMapper.insert(s);

        Map<Long, String> given = new LinkedHashMap<>();
        if (req.getAnswers() != null) {
            for (AnswerSubmitDTO a : req.getAnswers()) {
                if (a.getQuestionId() != null) {
                    given.put(a.getQuestionId(), a.getAnswerJson());
                }
            }
        }
        BigDecimal obj = ZERO;
        BigDecimal subj = ZERO;
        boolean pending = false;
        for (Question q : qs) {
            String ans = given.get(q.getId());
            SubmissionAnswer a = new SubmissionAnswer();
            a.setSubmissionId(s.getId());
            a.setQuestionId(q.getId());
            a.setAnswerJson(ans == null || ans.trim().isEmpty() ? null : ans);
            GradeEngine.Result r = GradeEngine.grade(q, a.getAnswerJson());
            a.setScore(r.getScore());
            a.setIsCorrect(r.getCorrect());
            if (!r.isPending()) {
                a.setMarkedByType("SYSTEM");
                a.setMarkedAt(now);
            }
            answerMapper.insert(a);
            if (r.isPending()) {
                pending = true;
            } else if ("SHORT".equalsIgnoreCase(q.getQType())) {
                subj = subj.add(r.getScore());
            } else {
                obj = obj.add(r.getScore());
            }
        }
        s.setObjectiveScore(obj);
        s.setSubjectiveScore(subj);
        if (pending) {
            s.setMarkedStatus("SYSTEM_MARKED");
            s.setTotalScore(null);
        } else {
            s.setMarkedStatus("ALL_MARKED");
            s.setSubmitStatus("MARKED");
            s.setTotalScore(obj.add(subj));
        }
        submissionMapper.updateById(s);
        return viewSubmission(p, s, studentId);
    }

    // ============================================================ 结果
    public SubmissionVO result(Long paperId, Long studentId) {
        Paper p = paperMapper.selectById(paperId);
        if (p == null) {
            throw new BizException("试卷不存在");
        }
        Submission s = submissionMapper.selectOne(Wrappers.<Submission>lambdaQuery()
                .eq(Submission::getPaperId, paperId)
                .eq(Submission::getStudentId, studentId).last("limit 1"));
        if (s == null) {
            throw new BizException("你还没有作答这份卷子");
        }
        return viewSubmission(p, s, studentId);
    }

    /** 按三个可见性开关裁剪 */
    private SubmissionVO viewSubmission(Paper p, Submission s, Long studentId) {
        SubmissionVO v = new SubmissionVO();
        v.setId(s.getId());
        v.setPaperId(p.getId());
        v.setPaperName(p.getPaperName());
        v.setPaperType(p.getPaperType());
        v.setStudentId(studentId);
        v.setSubmitStatus(s.getSubmitStatus());
        v.setMarkedStatus(s.getMarkedStatus());
        v.setStartTime(s.getStartTime());
        v.setSubmitTime(s.getSubmitTime());
        v.setDurationSeconds(s.getDurationSeconds());
        v.setFullScore(p.getFullScore());
        v.setIsAbsent(s.getIsAbsent());
        v.setRemark(s.getRemark());
        boolean showScore = p.getShowScore() == null || p.getShowScore() == 1;
        if (showScore) {
            v.setObjectiveScore(s.getObjectiveScore());
            v.setSubjectiveScore(s.getSubjectiveScore());
            v.setTotalScore(s.getTotalScore());
        }
        boolean viewPaper = p.getAllowViewPaper() != null && p.getAllowViewPaper() == 1;
        if (!viewPaper) {
            return v;
        }
        Map<Long, Question> qs = questionsOf(p.getId()).stream()
                .collect(Collectors.toMap(Question::getId, q -> q, (a, b) -> a));
        Map<Long, String> markedNames = new LinkedHashMap<>();
        List<SubmissionAnswer> answers = answerMapper.selectList(Wrappers.<SubmissionAnswer>lambdaQuery()
                .eq(SubmissionAnswer::getSubmissionId, s.getId()));
        List<Long> ids = answers.stream().map(SubmissionAnswer::getMarkedBy)
                .filter(java.util.Objects::nonNull).distinct().collect(Collectors.toList());
        if (!ids.isEmpty()) {
            for (Teacher t : teacherMapper.selectBatchIds(ids)) {
                markedNames.put(t.getId(), t.getTeacherName());
            }
        }
        boolean answerVisible = "AFTER_MARK".equals(p.getShowAnswer())
                && "ALL_MARKED".equals(s.getMarkedStatus());
        List<SubmissionAnswerVO> list = new ArrayList<>();
        for (SubmissionAnswer a : answers) {
            Question q = qs.get(a.getQuestionId());
            if (q == null) {
                continue;
            }
            SubmissionAnswerVO av = new SubmissionAnswerVO();
            av.setId(a.getId());
            av.setQuestionId(a.getQuestionId());
            av.setQNo(q.getQNo());
            av.setQType(q.getQType());
            av.setStem(q.getStem());
            av.setOptionsJson(q.getOptionsJson());
            av.setFullScore(q.getFullScore());
            av.setAnswerJson(a.getAnswerJson());
            av.setComment(a.getComment());
            av.setMarkedByType(a.getMarkedByType());
            av.setMarkedByName(a.getMarkedBy() == null ? null : markedNames.get(a.getMarkedBy()));
            if (showScore) {
                av.setScore(a.getScore());
                av.setIsCorrect(a.getIsCorrect());
            }
            if (answerVisible) {
                av.setStdAnswerJson(q.getAnswerJson());
                av.setAnalysis(q.getAnalysis());
            }
            list.add(av);
        }
        list.sort((x, y) -> Integer.compare(x.getQNo() == null ? 0 : x.getQNo(),
                y.getQNo() == null ? 0 : y.getQNo()));
        v.setAnswers(list);
        return v;
    }

    // ============================================================ 内部
    private Enrollment requireEnrollment(Paper p, Long studentId) {
        Enrollment e = enrollmentMapper.selectOne(Wrappers.<Enrollment>lambdaQuery()
                .eq(Enrollment::getTeachingClassId, p.getTeachingClassId())
                .eq(Enrollment::getStudentId, studentId).last("limit 1"));
        if (e == null) {
            throw new BizException("你没有选修这门课，看不到这份卷子");
        }
        if (e.getStatus() != null && ("WITHDRAWN".equals(e.getStatus()) || "SUSPENDED".equals(e.getStatus()))) {
            throw new BizException("你已不在该教学班（" + e.getStatus() + "），不能作答");
        }
        return e;
    }

    private List<Question> questionsOf(Long paperId) {
        return questionMapper.selectList(Wrappers.<Question>lambdaQuery()
                .eq(Question::getPaperId, paperId)
                .orderByAsc(Question::getSortNo).orderByAsc(Question::getQNo));
    }

    private StudentPaperVO baseVO(Paper p, TeachingClass tc, List<Question> qs) {
        StudentPaperVO v = new StudentPaperVO();
        v.setId(p.getId());
        v.setPaperName(p.getPaperName());
        v.setPaperType(p.getPaperType());
        v.setStartTime(p.getStartTime());
        v.setEndTime(p.getEndTime());
        v.setDurationMinutes(p.getDurationMinutes());
        v.setFullScore(p.getFullScore());
        if (tc != null) {
            CourseOffering o = offeringMapper.selectById(tc.getOfferingId());
            if (o != null) {
                Course c = courseMapper.selectById(o.getCourseId());
                v.setCourseName(c == null ? null : c.getCourseName());
            }
            Teacher t = teacherMapper.selectById(tc.getTeacherId());
            v.setTeacherName(t == null ? null : t.getTeacherName());
        }
        if (qs != null) {
            List<QuestionDTO> list = new ArrayList<>();
            for (Question q : qs) {
                QuestionDTO d = new QuestionDTO();
                d.setId(q.getId());
                d.setQNo(q.getQNo());
                d.setQType(q.getQType());
                d.setStem(q.getStem());
                d.setOptionsJson(q.getOptionsJson());
                d.setFullScore(q.getFullScore());
                d.setGroupName(q.getGroupName());
                d.setSortNo(q.getSortNo());
                list.add(d);
            }
            v.setQuestions(list);
        }
        LocalDateTime now = LocalDateTime.now();
        if (p.getEndTime() != null) {
            long left = Duration.between(now, p.getEndTime()).getSeconds();
            v.setRemainingSeconds((int) Math.max(0, left));
        }
        return v;
    }
}
`);

w('controller/student/StudentPaperController.java', `
package ${P}.controller.student;

import ${P}.common.BizException;
import ${P}.common.Result;
import ${P}.dto.PaperSubmitRequest;
import ${P}.security.LoginUser;
import ${P}.security.RequireRole;
import ${P}.security.UserContext;
import ${P}.service.StudentPaperService;
import ${P}.vo.StudentPaperVO;
import ${P}.vo.SubmissionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 学生端 · 测验与作业（我的考试 / 在线作答 / 结果） */
@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
@RequireRole("STUDENT")
public class StudentPaperController {

    private final StudentPaperService studentPaperService;

    private Long me() {
        LoginUser user = UserContext.require();
        if (user.getRefId() == null) {
            throw new BizException("当前账号没有学生档案");
        }
        return user.getRefId();
    }

    @GetMapping("/papers")
    public Result<List<StudentPaperVO>> list() {
        return Result.ok(studentPaperService.list(me()));
    }

    @GetMapping("/papers/{id}")
    public Result<StudentPaperVO> get(@PathVariable Long id) {
        return Result.ok(studentPaperService.get(id, me()));
    }

    @PostMapping("/papers/{id}/submit")
    public Result<SubmissionVO> submit(@PathVariable Long id, @RequestBody PaperSubmitRequest request) {
        return Result.ok(studentPaperService.submit(id, request, me()));
    }

    @GetMapping("/papers/{id}/result")
    public Result<SubmissionVO> result(@PathVariable Long id) {
        return Result.ok(studentPaperService.result(id, me()));
    }
}
`);

console.log('学生端测验与作业：生成完成');

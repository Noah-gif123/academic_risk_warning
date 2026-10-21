package com.example.academic_risk_warning.service;

import com.example.academic_risk_warning.common.BizException;
import com.example.academic_risk_warning.dto.AnswerSubmitDTO;
import com.example.academic_risk_warning.dto.PaperSubmitRequest;
import com.example.academic_risk_warning.dto.QuestionDTO;
import com.example.academic_risk_warning.entity.Course;
import com.example.academic_risk_warning.entity.CourseOffering;
import com.example.academic_risk_warning.entity.Enrollment;
import com.example.academic_risk_warning.entity.Paper;
import com.example.academic_risk_warning.entity.Question;
import com.example.academic_risk_warning.entity.Student;
import com.example.academic_risk_warning.entity.Submission;
import com.example.academic_risk_warning.entity.SubmissionAnswer;
import com.example.academic_risk_warning.entity.Teacher;
import com.example.academic_risk_warning.entity.TeachingClass;
import com.example.academic_risk_warning.mapper.CourseMapper;
import com.example.academic_risk_warning.mapper.CourseOfferingMapper;
import com.example.academic_risk_warning.mapper.EnrollmentMapper;
import com.example.academic_risk_warning.mapper.PaperMapper;
import com.example.academic_risk_warning.mapper.QuestionMapper;
import com.example.academic_risk_warning.mapper.StudentMapper;
import com.example.academic_risk_warning.mapper.SubmissionAnswerMapper;
import com.example.academic_risk_warning.mapper.SubmissionMapper;
import com.example.academic_risk_warning.mapper.TeacherMapper;
import com.example.academic_risk_warning.mapper.TeachingClassMapper;
import com.example.academic_risk_warning.util.GradeEngine;
import com.example.academic_risk_warning.util.MiniJson;
import com.example.academic_risk_warning.vo.StudentPaperVO;
import com.example.academic_risk_warning.vo.SubmissionAnswerVO;
import com.example.academic_risk_warning.vo.SubmissionVO;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

    /** 填空题的“一个空”：连续两个及以上的半角/全角下划线（＿＿＿＿ 算一个空，不是四个） */
    private static final Pattern BLANK_GROUP = Pattern.compile("[_＿]{2,}");

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
        // 客户端时区/时钟异常（例如把本地时间当成 UTC 发上来）会让用时离谱，这里做兜底：
        // 早于开考时间按开考时间算；早于 24 小时视为无效客户端时间，按本次提交重新计时
        if (p.getStartTime() != null && startedAt.isBefore(p.getStartTime())) {
            startedAt = p.getStartTime();
        }
        if (startedAt.isBefore(now.minusHours(24))) {
            startedAt = now;
        }
        // 限时卷再兜一层：客户端把本地时间当 UTC 发上来会多出 8 小时，
        // 超过“限时 + 6 小时”一律视为客户端时间不可信，按本次提交重新计时
        if (p.getDurationMinutes() != null && p.getDurationMinutes() > 0
                && Duration.between(startedAt, now).toMinutes() > p.getDurationMinutes() + 360) {
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
            if (r.isPending()) {
                // 主观题等老师批阅：分数留空，避免前端显示成 0 分
                a.setScore(null);
                a.setIsCorrect(null);
            } else {
                a.setScore(r.getScore());
                a.setIsCorrect(r.getCorrect());
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

    /**
     * 填空题空数：以老师给的标准答案为唯一口径（一条答案 = 一个空），
     * 没有答案时退回“题干里连续下划线的组数”，再兜底 1 个空。
     * 注意要把连续的“＿/__”当成一个空，否则一个 4 连下划线会被数成 4 个空。
     */
    private int blankCountOf(Question q) {
        List<String> std = MiniJson.arrayStrings(q.getAnswerJson(), "answers");
        if (std != null && !std.isEmpty()) {
            return Math.min(std.size(), 20);
        }
        int n = 0;
        if (q.getStem() != null) {
            Matcher m = BLANK_GROUP.matcher(q.getStem());
            while (m.find()) {
                n++;
            }
        }
        return Math.min(Math.max(n, 1), 20);
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
                if ("BLANK".equalsIgnoreCase(q.getQType())) {
                    d.setBlankCount(blankCountOf(q));
                }
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

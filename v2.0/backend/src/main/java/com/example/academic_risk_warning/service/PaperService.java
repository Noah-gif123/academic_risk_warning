package com.example.academic_risk_warning.service;

import com.example.academic_risk_warning.common.BizException;
import com.example.academic_risk_warning.dto.GradeItemDTO;
import com.example.academic_risk_warning.dto.GradeRequest;
import com.example.academic_risk_warning.dto.PaperSaveRequest;
import com.example.academic_risk_warning.dto.QuestionDTO;
import com.example.academic_risk_warning.entity.Course;
import com.example.academic_risk_warning.entity.CourseLesson;
import com.example.academic_risk_warning.entity.CourseOffering;
import com.example.academic_risk_warning.entity.Enrollment;
import com.example.academic_risk_warning.entity.Paper;
import com.example.academic_risk_warning.entity.Question;
import com.example.academic_risk_warning.entity.ScoreSchemeItem;
import com.example.academic_risk_warning.entity.Student;
import com.example.academic_risk_warning.entity.Submission;
import com.example.academic_risk_warning.entity.SubmissionAnswer;
import com.example.academic_risk_warning.entity.Teacher;
import com.example.academic_risk_warning.entity.TeachingClass;
import com.example.academic_risk_warning.mapper.CourseLessonMapper;
import com.example.academic_risk_warning.mapper.CourseMapper;
import com.example.academic_risk_warning.mapper.CourseOfferingMapper;
import com.example.academic_risk_warning.mapper.EnrollmentMapper;
import com.example.academic_risk_warning.mapper.PaperMapper;
import com.example.academic_risk_warning.mapper.QuestionMapper;
import com.example.academic_risk_warning.mapper.ScoreSchemeItemMapper;
import com.example.academic_risk_warning.mapper.StudentMapper;
import com.example.academic_risk_warning.mapper.SubmissionAnswerMapper;
import com.example.academic_risk_warning.mapper.SubmissionMapper;
import com.example.academic_risk_warning.mapper.TeacherMapper;
import com.example.academic_risk_warning.mapper.TeachingClassMapper;
import com.example.academic_risk_warning.util.GradeEngine;
import com.example.academic_risk_warning.vo.PaperVO;
import com.example.academic_risk_warning.vo.SubmissionAnswerVO;
import com.example.academic_risk_warning.vo.SubmissionVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 试卷（测验 / 作业 / 考试）：教师端管理、发布、批改、缺考记 0 */
@Service
@RequiredArgsConstructor
public class PaperService {

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
    private final CourseLessonMapper lessonMapper;
    private final ScoreSchemeItemMapper schemeItemMapper;
    private final CourseContentService contentService;
    private final AuditService auditService;

    // ============================================================ 查询
    public List<PaperVO> listPapers(Long teachingClassId, Long teacherId) {
        contentService.requireMyClass(teachingClassId, teacherId);
        List<Paper> papers = paperMapper.selectList(Wrappers.<Paper>lambdaQuery()
                .eq(Paper::getTeachingClassId, teachingClassId)
                .orderByDesc(Paper::getId));
        List<PaperVO> out = new ArrayList<>();
        for (Paper p : papers) {
            out.add(toVO(p, false));
        }
        return out;
    }

    public PaperVO getPaper(Long paperId, Long teacherId) {
        Paper p = requireMyPaper(paperId, teacherId);
        return toVO(p, true);
    }

    public Paper requireMyPaper(Long paperId, Long teacherId) {
        Paper p = paperMapper.selectById(paperId);
        if (p == null) {
            throw new BizException("试卷不存在");
        }
        contentService.requireMyClass(p.getTeachingClassId(), teacherId);
        return p;
    }

    private List<Question> questionsOf(Long paperId) {
        return questionMapper.selectList(Wrappers.<Question>lambdaQuery()
                .eq(Question::getPaperId, paperId)
                .orderByAsc(Question::getSortNo).orderByAsc(Question::getQNo));
    }

    // ============================================================ 保存
    @Transactional
    public PaperVO save(PaperSaveRequest req, Long teacherId) {
        TeachingClass tc = contentService.requireMyClass(req.getTeachingClassId(), teacherId);
        Paper paper = req.getId() == null ? null : paperMapper.selectById(req.getId());
        if (paper == null) {
            paper = new Paper();
            paper.setTeachingClassId(tc.getId());
            paper.setOfferingId(tc.getOfferingId());
            paper.setTermId(contentService.termIdOf(tc));
            paper.setPublisherRole("TEACHER");
            paper.setPublisherId(teacherId);
            paper.setStatus("DRAFT");
            paper.setFullScore(ZERO);
        } else {
            if (!paper.getTeachingClassId().equals(tc.getId())) {
                throw new BizException("试卷不属于该教学班");
            }
            if ("PUBLISHED".equals(paper.getStatus())) {
                throw new BizException("已发布的试卷不能改题，请先撤回再编辑");
            }
        }
        paper.setPaperType(req.getPaperType());
        paper.setPaperName(req.getPaperName());
        paper.setLessonId(req.getLessonId());
        paper.setStartTime(req.getStartTime());
        paper.setEndTime(req.getEndTime());
        paper.setDurationMinutes(req.getDurationMinutes());
        paper.setSchemeItemId(req.getSchemeItemId());
        paper.setAllowViewPaper(req.getAllowViewPaper() == null ? 0 : req.getAllowViewPaper());
        paper.setShowScore(req.getShowScore() == null ? 1 : req.getShowScore());
        paper.setShowAnswer(req.getShowAnswer() == null ? "NEVER" : req.getShowAnswer());
        paper.setRemark(req.getRemark());
        if (paper.getId() == null) {
            paperMapper.insert(paper);
        } else {
            paperMapper.updateById(paper);
        }

        if (req.getQuestions() != null) {
            questionMapper.delete(Wrappers.<Question>lambdaQuery().eq(Question::getPaperId, paper.getId()));
            BigDecimal sum = ZERO;
            int i = 0;
            for (QuestionDTO d : req.getQuestions()) {
                i++;
                Question q = new Question();
                q.setPaperId(paper.getId());
                q.setQNo(d.getQNo() == null ? i : d.getQNo());
                q.setQType(d.getQType() == null ? "SINGLE" : d.getQType());
                q.setStem(d.getStem());
                q.setOptionsJson(d.getOptionsJson());
                q.setAnswerJson(d.getAnswerJson());
                q.setAnalysis(d.getAnalysis());
                q.setFullScore(d.getFullScore() == null ? new BigDecimal("2") : d.getFullScore());
                q.setGroupName(d.getGroupName());
                q.setSortNo(d.getSortNo() == null ? i : d.getSortNo());
                questionMapper.insert(q);
                sum = sum.add(q.getFullScore());
            }
            paper.setFullScore(sum);
            paperMapper.updateById(paper);
        }
        auditService.record("PAPER", paper.getId(), "SAVE", null, null,
                "保存试卷：" + paper.getPaperName() + "（" + paper.getStatus() + "）");
        return toVO(paper, true);
    }

    // ============================================================ 发布 / 撤回
    @Transactional
    public PaperVO setPublish(Long paperId, boolean publish, Long teacherId) {
        Paper p = requireMyPaper(paperId, teacherId);
        if (publish) {
            List<Question> qs = questionsOf(paperId);
            if (qs.isEmpty()) {
                throw new BizException("试卷还没有题目，不能发布");
            }
            for (Question q : qs) {
                if (q.getAnswerJson() == null || q.getAnswerJson().trim().isEmpty()) {
                    throw new BizException("第 " + q.getQNo() + " 题没有标准答案，不能发布");
                }
            }
            if (p.getEndTime() == null) {
                throw new BizException("作业与测验不允许迟交，发布前必须设置截止时间");
            }
            if (p.getStartTime() != null && p.getStartTime().isAfter(p.getEndTime())) {
                throw new BizException("开始时间不能晚于截止时间");
            }
            p.setStatus("PUBLISHED");
        } else {
            if ("CLOSED".equals(p.getStatus())) {
                throw new BizException("已截止的试卷不能撤回");
            }
            long subs = submissionMapper.selectCount(Wrappers.<Submission>lambdaQuery()
                    .eq(Submission::getPaperId, paperId));
            if (subs > 0) {
                throw new BizException("已有学生交卷，不能撤回");
            }
            p.setStatus("DRAFT");
        }
        paperMapper.updateById(p);
        auditService.record("PAPER", paperId, publish ? "PUBLISH" : "UNPUBLISH", null, null,
                (publish ? "发布" : "撤回") + "试卷：" + p.getPaperName());
        return toVO(p, true);
    }

    /** 截止：把未交卷的学生记缺考（0 分） */
    @Transactional
    public PaperVO closePaper(Long paperId, Long teacherId) {
        Paper p = requireMyPaper(paperId, teacherId);
        if (p.getEndTime() == null || LocalDateTime.now().isBefore(p.getEndTime())) {
            throw new BizException("还没到截止时间，不能登记缺考");
        }
        int absent = markAbsentForPaper(p, teacherId);
        p.setStatus("CLOSED");
        paperMapper.updateById(p);
        auditService.record("PAPER", paperId, "CLOSE", null, null,
                "截止试卷并登记缺考 " + absent + " 人：" + p.getPaperName());
        return toVO(p, true);
    }

    /** 删除草稿：只有草稿、且没有学生交卷时才允许删除（已发布的请先撤回） */
    @Transactional
    public void deletePaper(Long paperId, Long teacherId) {
        Paper p = requireMyPaper(paperId, teacherId);
        if ("PUBLISHED".equals(p.getStatus())) {
            throw new BizException("已发布的试卷不能删除，请先撤回为草稿");
        }
        if ("CLOSED".equals(p.getStatus())) {
            throw new BizException("已截止的试卷不能删除");
        }
        long subs = submissionMapper.selectCount(Wrappers.<Submission>lambdaQuery()
                .eq(Submission::getPaperId, paperId));
        if (subs > 0) {
            throw new BizException("已有学生交卷，不能删除这份试卷");
        }
        questionMapper.delete(Wrappers.<Question>lambdaQuery().eq(Question::getPaperId, paperId));
        paperMapper.deleteById(paperId);
        auditService.record("PAPER", paperId, "DELETE", null, null, "删除试卷草稿：" + p.getPaperName());
    }

    private int markAbsentForPaper(Paper p, Long teacherId) {
        List<Enrollment> ens = enrollmentMapper.selectList(Wrappers.<Enrollment>lambdaQuery()
                .eq(Enrollment::getTeachingClassId, p.getTeachingClassId()));
        int absent = 0;
        LocalDateTime now = LocalDateTime.now();
        for (Enrollment e : ens) {
            if (e.getStatus() != null && ("WITHDRAWN".equals(e.getStatus()) || "SUSPENDED".equals(e.getStatus()))) {
                continue;
            }
            long exist = submissionMapper.selectCount(Wrappers.<Submission>lambdaQuery()
                    .eq(Submission::getPaperId, p.getId())
                    .eq(Submission::getStudentId, e.getStudentId()));
            if (exist > 0) {
                continue;
            }
            Submission s = new Submission();
            s.setPaperId(p.getId());
            s.setTeachingClassId(p.getTeachingClassId());
            s.setTermId(p.getTermId());
            s.setStudentId(e.getStudentId());
            s.setEnrollmentId(e.getId());
            s.setSubmitStatus("ABSENT");
            s.setMarkedStatus("ALL_MARKED");
            s.setObjectiveScore(ZERO);
            s.setSubjectiveScore(ZERO);
            s.setTotalScore(ZERO);
            s.setIsAbsent(1);
            s.setRemark("缺考记 0 分");
            s.setMarkedBy(teacherId);
            s.setMarkedAt(now);
            submissionMapper.insert(s);
            absent++;
        }
        return absent;
    }

    // ============================================================ 提交与批改
    public List<SubmissionVO> listSubmissions(Long paperId, Long teacherId) {
        Paper p = requireMyPaper(paperId, teacherId);
        List<Submission> subs = submissionMapper.selectList(Wrappers.<Submission>lambdaQuery()
                .eq(Submission::getPaperId, paperId)
                .orderByAsc(Submission::getId));
        Map<Long, Student> students = new LinkedHashMap<>();
        if (!subs.isEmpty()) {
            for (Student st : studentMapper.selectBatchIds(
                    subs.stream().map(Submission::getStudentId).distinct().collect(Collectors.toList()))) {
                students.put(st.getId(), st);
            }
        }
        Teacher teacher = teacherMapper.selectById(p.getPublisherId());
        List<SubmissionVO> out = new ArrayList<>();
        for (Submission s : subs) {
            SubmissionVO v = baseSubmissionVO(s, p, students.get(s.getStudentId()), teacher, false);
            out.add(v);
        }
        return out;
    }

    public SubmissionVO getSubmission(Long submissionId, Long teacherId) {
        Submission s = submissionMapper.selectById(submissionId);
        if (s == null) {
            throw new BizException("提交记录不存在");
        }
        Paper p = requireMyPaper(s.getPaperId(), teacherId);
        Student st = studentMapper.selectById(s.getStudentId());
        Teacher teacher = teacherMapper.selectById(p.getPublisherId());
        return baseSubmissionVO(s, p, st, teacher, true);
    }

    /** 保存主观题批改 */
    @Transactional
    public SubmissionVO grade(Long submissionId, GradeRequest req, Long teacherId) {
        Submission s = submissionMapper.selectById(submissionId);
        if (s == null) {
            throw new BizException("提交记录不存在");
        }
        Paper p = requireMyPaper(s.getPaperId(), teacherId);
        if (s.getIsAbsent() != null && s.getIsAbsent() == 1) {
            throw new BizException("缺考记录无需批改（已记 0 分）");
        }
        Map<Long, Question> qs = questionsOf(p.getId()).stream()
                .collect(Collectors.toMap(Question::getId, q -> q, (a, b) -> a));
        LocalDateTime now = LocalDateTime.now();
        if (req.getItems() != null) {
            for (GradeItemDTO it : req.getItems()) {
                SubmissionAnswer a = answerMapper.selectById(it.getAnswerId());
                if (a == null || !submissionId.equals(a.getSubmissionId())) {
                    continue;
                }
                Question q = qs.get(a.getQuestionId());
                if (q == null || !GradeEngine.isManual(q.getQType())) {
                    continue;
                }
                BigDecimal score = it.getScore() == null ? ZERO : it.getScore();
                if (score.signum() < 0) {
                    score = ZERO;
                }
                BigDecimal max = q.getFullScore() == null ? ZERO : q.getFullScore();
                if (score.compareTo(max) > 0) {
                    score = max;
                }
                a.setScore(score);
                a.setComment(it.getComment());
                a.setMarkedByType("TEACHER");
                a.setMarkedBy(teacherId);
                a.setMarkedAt(now);
                answerMapper.updateById(a);
            }
        }
        recompute(s, qs, teacherId, now, req.getRemark());
        auditService.record("SUBMISSION", submissionId, "GRADE", null, null,
                "批改主观题，总分 " + s.getTotalScore());
        Student st = studentMapper.selectById(s.getStudentId());
        return baseSubmissionVO(s, p, st, teacherMapper.selectById(p.getPublisherId()), true);
    }

    /** 单个学生记缺考 */
    @Transactional
    public SubmissionVO markAbsent(Long paperId, Long studentId, Long teacherId) {
        Paper p = requireMyPaper(paperId, teacherId);
        Enrollment e = enrollmentMapper.selectOne(Wrappers.<Enrollment>lambdaQuery()
                .eq(Enrollment::getTeachingClassId, p.getTeachingClassId())
                .eq(Enrollment::getStudentId, studentId).last("limit 1"));
        if (e == null) {
            throw new BizException("该学生没有选修这门课");
        }
        long exist = submissionMapper.selectCount(Wrappers.<Submission>lambdaQuery()
                .eq(Submission::getPaperId, paperId).eq(Submission::getStudentId, studentId));
        if (exist > 0) {
            throw new BizException("该学生已有记录（已交卷或缺考）");
        }
        LocalDateTime now = LocalDateTime.now();
        Submission s = new Submission();
        s.setPaperId(p.getId());
        s.setTeachingClassId(p.getTeachingClassId());
        s.setTermId(p.getTermId());
        s.setStudentId(studentId);
        s.setEnrollmentId(e.getId());
        s.setSubmitStatus("ABSENT");
        s.setMarkedStatus("ALL_MARKED");
        s.setObjectiveScore(ZERO);
        s.setSubjectiveScore(ZERO);
        s.setTotalScore(ZERO);
        s.setIsAbsent(1);
        s.setRemark("教师登记缺考，记 0 分");
        s.setMarkedBy(teacherId);
        s.setMarkedAt(now);
        submissionMapper.insert(s);
        auditService.record("SUBMISSION", s.getId(), "ABSENT", null, null, "登记缺考：学生 " + studentId);
        return baseSubmissionVO(s, p, studentMapper.selectById(studentId),
                teacherMapper.selectById(p.getPublisherId()), true);
    }

    /** 重算客观分 / 主观分 / 总分 / 批阅进度 */
    public void recompute(Submission s, Map<Long, Question> qs, Long teacherId, LocalDateTime now, String remark) {
        List<SubmissionAnswer> answers = answerMapper.selectList(Wrappers.<SubmissionAnswer>lambdaQuery()
                .eq(SubmissionAnswer::getSubmissionId, s.getId()));
        BigDecimal obj = ZERO;
        BigDecimal subj = ZERO;
        boolean pending = false;
        for (SubmissionAnswer a : answers) {
            Question q = qs.get(a.getQuestionId());
            if (q == null) {
                continue;
            }
            boolean isShort = GradeEngine.isManual(q.getQType());
            BigDecimal score = a.getScore() == null ? ZERO : a.getScore();
            if (isShort) {
                boolean marked = "TEACHER".equals(a.getMarkedByType()) || "AI".equals(a.getMarkedByType());
                if (marked) {
                    subj = subj.add(score);
                } else {
                    pending = true;
                }
            } else {
                obj = obj.add(score);
            }
        }
        s.setObjectiveScore(obj);
        s.setSubjectiveScore(subj);
        if (pending) {
            s.setMarkedStatus("SYSTEM_MARKED");
            s.setSubmitStatus("SUBMITTED");
            s.setTotalScore(null);
        } else {
            s.setMarkedStatus("ALL_MARKED");
            s.setSubmitStatus("MARKED");
            s.setTotalScore(obj.add(subj));
        }
        s.setMarkedBy(teacherId);
        s.setMarkedAt(now);
        if (remark != null) {
            s.setRemark(remark);
        }
        submissionMapper.updateById(s);
    }

    // ============================================================ 视图组装
    private PaperVO toVO(Paper p, boolean withQuestions) {
        PaperVO v = new PaperVO();
        v.setId(p.getId());
        v.setTeachingClassId(p.getTeachingClassId());
        v.setPaperType(p.getPaperType());
        v.setPaperName(p.getPaperName());
        v.setLessonId(p.getLessonId());
        if (p.getLessonId() != null) {
            CourseLesson l = lessonMapper.selectById(p.getLessonId());
            if (l != null) {
                v.setWeekNo(l.getWeekNo());
                v.setSessionNo(l.getSessionNo());
            }
        }
        v.setStartTime(p.getStartTime());
        v.setEndTime(p.getEndTime());
        v.setDurationMinutes(p.getDurationMinutes());
        v.setFullScore(p.getFullScore());
        v.setSchemeItemId(p.getSchemeItemId());
        if (p.getSchemeItemId() != null) {
            ScoreSchemeItem item = schemeItemMapper.selectById(p.getSchemeItemId());
            v.setSchemeItemName(item == null ? null : item.getItemName());
        }
        v.setAllowViewPaper(p.getAllowViewPaper());
        v.setShowScore(p.getShowScore());
        v.setShowAnswer(p.getShowAnswer());
        v.setStatus(p.getStatus());
        v.setRemark(p.getRemark());
        List<Question> qs = questionsOf(p.getId());
        BigDecimal sum = ZERO;
        int subjective = 0;
        for (Question q : qs) {
            sum = sum.add(q.getFullScore() == null ? ZERO : q.getFullScore());
            if (GradeEngine.isManual(q.getQType())) {
                subjective++;
            }
        }
        v.setQuestionCount(qs.size());
        v.setQuestionScore(sum);
        v.setSubjectiveCount(subjective);
        long subs = submissionMapper.selectCount(Wrappers.<Submission>lambdaQuery()
                .eq(Submission::getPaperId, p.getId()));
        long absent = submissionMapper.selectCount(Wrappers.<Submission>lambdaQuery()
                .eq(Submission::getPaperId, p.getId()).eq(Submission::getIsAbsent, 1));
        long marked = submissionMapper.selectCount(Wrappers.<Submission>lambdaQuery()
                .eq(Submission::getPaperId, p.getId()).eq(Submission::getMarkedStatus, "ALL_MARKED"));
        v.setSubmitCount((int) subs);
        v.setAbsentCount((int) absent);
        v.setMarkedCount((int) marked);
        v.setTotalStudents(enrollmentMapper.selectCount(Wrappers.<Enrollment>lambdaQuery()
                .eq(Enrollment::getTeachingClassId, p.getTeachingClassId())).intValue());
        if (withQuestions) {
            List<QuestionDTO> list = new ArrayList<>();
            for (Question q : qs) {
                QuestionDTO d = new QuestionDTO();
                d.setId(q.getId());
                d.setQNo(q.getQNo());
                d.setQType(q.getQType());
                d.setStem(q.getStem());
                d.setOptionsJson(q.getOptionsJson());
                d.setAnswerJson(q.getAnswerJson());
                d.setAnalysis(q.getAnalysis());
                d.setFullScore(q.getFullScore());
                d.setGroupName(q.getGroupName());
                d.setSortNo(q.getSortNo());
                list.add(d);
            }
            v.setQuestions(list);
        }
        return v;
    }

    private SubmissionVO baseSubmissionVO(Submission s, Paper p, Student st, Teacher teacher, boolean withAnswers) {
        SubmissionVO v = new SubmissionVO();
        v.setId(s.getId());
        v.setPaperId(p.getId());
        v.setPaperName(p.getPaperName());
        v.setPaperType(p.getPaperType());
        v.setStudentId(s.getStudentId());
        v.setStudentNo(st == null ? null : st.getStudentNo());
        v.setStudentName(st == null ? null : st.getStudentName());
        v.setSubmitStatus(s.getSubmitStatus());
        v.setMarkedStatus(s.getMarkedStatus());
        v.setStartTime(s.getStartTime());
        v.setSubmitTime(s.getSubmitTime());
        v.setDurationSeconds(s.getDurationSeconds());
        v.setObjectiveScore(s.getObjectiveScore());
        v.setSubjectiveScore(s.getSubjectiveScore());
        v.setTotalScore(s.getTotalScore());
        v.setFullScore(p.getFullScore());
        v.setIsAbsent(s.getIsAbsent());
        v.setRemark(s.getRemark());
        v.setMarkedByName(teacher == null ? null : teacher.getTeacherName());
        if (withAnswers) {
            Map<Long, Question> qs = questionsOf(p.getId()).stream()
                    .collect(Collectors.toMap(Question::getId, q -> q, (a, b) -> a));
            Map<Long, String> markedNames = new LinkedHashMap<>();
            List<SubmissionAnswer> answers = answerMapper.selectList(Wrappers.<SubmissionAnswer>lambdaQuery()
                    .eq(SubmissionAnswer::getSubmissionId, s.getId()));
            List<Long> markerIds = answers.stream().map(SubmissionAnswer::getMarkedBy)
                    .filter(java.util.Objects::nonNull).distinct().collect(Collectors.toList());
            if (!markerIds.isEmpty()) {
                for (Teacher t : teacherMapper.selectBatchIds(markerIds)) {
                    markedNames.put(t.getId(), t.getTeacherName());
                }
            }
            List<SubmissionAnswerVO> list = new ArrayList<>();
            int pending = 0;
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
                av.setStdAnswerJson(q.getAnswerJson());
                av.setAnalysis(q.getAnalysis());
                av.setScore(a.getScore());
                av.setIsCorrect(a.getIsCorrect());
                av.setComment(a.getComment());
                av.setMarkedByType(a.getMarkedByType());
                av.setMarkedBy(a.getMarkedBy());
                av.setMarkedByName(a.getMarkedBy() == null ? null : markedNames.get(a.getMarkedBy()));
                av.setMarkedAt(a.getMarkedAt());
                if (GradeEngine.isManual(q.getQType())
                        && (a.getMarkedByType() == null || a.getMarkedByType().isEmpty())) {
                    pending++;
                }
                list.add(av);
            }
            list.sort((x, y) -> {
                int a1 = x.getQNo() == null ? 0 : x.getQNo();
                int b1 = y.getQNo() == null ? 0 : y.getQNo();
                return Integer.compare(a1, b1);
            });
            v.setAnswers(list);
            v.setPendingCount(pending);
        }
        return v;
    }
}

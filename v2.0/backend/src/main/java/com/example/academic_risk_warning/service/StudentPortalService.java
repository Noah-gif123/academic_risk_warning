package com.example.academic_risk_warning.service;

import com.example.academic_risk_warning.common.BizException;
import com.example.academic_risk_warning.entity.SysAccount;
import com.example.academic_risk_warning.entity.AdministrativeClass;
import com.example.academic_risk_warning.entity.Course;
import com.example.academic_risk_warning.entity.CourseGuide;
import com.example.academic_risk_warning.entity.CourseJoinRequest;
import com.example.academic_risk_warning.entity.CourseLesson;
import com.example.academic_risk_warning.entity.CourseOffering;
import com.example.academic_risk_warning.entity.CourseSession;
import com.example.academic_risk_warning.entity.Enrollment;
import com.example.academic_risk_warning.entity.LessonResource;
import com.example.academic_risk_warning.entity.Major;
import com.example.academic_risk_warning.entity.Paper;
import com.example.academic_risk_warning.entity.ProjectSubmission;
import com.example.academic_risk_warning.entity.ProjectTask;
import com.example.academic_risk_warning.entity.Question;
import com.example.academic_risk_warning.entity.ResourceFile;
import com.example.academic_risk_warning.entity.ScoreScheme;
import com.example.academic_risk_warning.entity.ScoreSchemeItem;
import com.example.academic_risk_warning.entity.Student;
import com.example.academic_risk_warning.entity.Submission;
import com.example.academic_risk_warning.entity.Teacher;
import com.example.academic_risk_warning.entity.TeachingClass;
import com.example.academic_risk_warning.entity.TeachingObjective;
import com.example.academic_risk_warning.entity.Term;
import com.example.academic_risk_warning.mapper.SysAccountMapper;
import com.example.academic_risk_warning.mapper.AdministrativeClassMapper;
import com.example.academic_risk_warning.mapper.CourseGuideMapper;
import com.example.academic_risk_warning.mapper.CourseJoinRequestMapper;
import com.example.academic_risk_warning.mapper.CourseLessonMapper;
import com.example.academic_risk_warning.mapper.CourseMapper;
import com.example.academic_risk_warning.mapper.CourseOfferingMapper;
import com.example.academic_risk_warning.mapper.CourseSessionMapper;
import com.example.academic_risk_warning.mapper.EnrollmentMapper;
import com.example.academic_risk_warning.mapper.LessonResourceMapper;
import com.example.academic_risk_warning.mapper.MajorMapper;
import com.example.academic_risk_warning.mapper.PaperMapper;
import com.example.academic_risk_warning.mapper.ProjectSubmissionMapper;
import com.example.academic_risk_warning.mapper.ProjectTaskMapper;
import com.example.academic_risk_warning.mapper.QuestionMapper;
import com.example.academic_risk_warning.mapper.ResourceFileMapper;
import com.example.academic_risk_warning.mapper.ScoreSchemeItemMapper;
import com.example.academic_risk_warning.mapper.ScoreSchemeMapper;
import com.example.academic_risk_warning.mapper.StudentMapper;
import com.example.academic_risk_warning.mapper.SubmissionMapper;
import com.example.academic_risk_warning.mapper.TeacherMapper;
import com.example.academic_risk_warning.mapper.TeachingClassMapper;
import com.example.academic_risk_warning.mapper.TeachingObjectiveMapper;
import com.example.academic_risk_warning.mapper.TermMapper;
import com.example.academic_risk_warning.vo.CourseCardPageVO;
import com.example.academic_risk_warning.vo.CourseCardVO;
import com.example.academic_risk_warning.vo.JoinRequestVO;
import com.example.academic_risk_warning.vo.StudentCourseDetailVO;
import com.example.academic_risk_warning.vo.StudentLessonVO;
import com.example.academic_risk_warning.vo.StudentProfileVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 学生端门户：课程卡片 / 课程学习 / 课程详情六页签 / 加入课程（邀请码）/ 个人中心。
 * 口径：① 只看自己的选课，不显示其他同学数据；② 教师结课（教学班 CLOSED）后课程在界面消失；
 * ③ 课程详情只保留「导学 / 教学目标 / 目录 / 测验 / 作业 / 项目」六个页签，
 * 后三个是否出现由成绩构成里的评分项决定；④ 目录只显示已发布的资源与任务。
 */
@Service
@RequiredArgsConstructor
public class StudentPortalService {

    private final EnrollmentMapper enrollmentMapper;
    private final StudentMapper studentMapper;
    private final SysAccountMapper accountMapper;
    private final MajorMapper majorMapper;
    private final AdministrativeClassMapper adminClassMapper;
    private final TeachingClassMapper classMapper;
    private final CourseOfferingMapper offeringMapper;
    private final CourseMapper courseMapper;
    private final TermMapper termMapper;
    private final TeacherMapper teacherMapper;
    private final CourseGuideMapper guideMapper;
    private final TeachingObjectiveMapper objectiveMapper;
    private final CourseLessonMapper lessonMapper;
    private final CourseSessionMapper sessionMapper;
    private final LessonResourceMapper lessonResourceMapper;
    private final ResourceFileMapper resourceFileMapper;
    private final PaperMapper paperMapper;
    private final SubmissionMapper submissionMapper;
    private final ProjectTaskMapper projectTaskMapper;
    private final ProjectSubmissionMapper projectSubmissionMapper;
    private final QuestionMapper questionMapper;
    private final ScoreSchemeMapper schemeMapper;
    private final ScoreSchemeItemMapper schemeItemMapper;
    private final CourseJoinRequestMapper joinRequestMapper;

    // ============================================================ 学期
    public List<Term> terms() {
        return termMapper.selectList(Wrappers.<Term>lambdaQuery().orderByDesc(Term::getStartDate));
    }

    // ============================================================ 课程卡片（首页 / 课程学习）
    public CourseCardPageVO cards(Long studentId, Long termId, String keyword, String status,
                                  String teacherName, int page, int size) {
        List<CourseCardVO> all = allCards(studentId, termId);
        List<CourseCardVO> filtered = new ArrayList<>();
        for (CourseCardVO c : all) {
            if (keyword != null && !keyword.isBlank()) {
                String k = keyword.trim();
                if (!contains(c.getCourseName(), k) && !contains(c.getTeacherName(), k)) {
                    continue;
                }
            }
            if (teacherName != null && !teacherName.isBlank() && !contains(c.getTeacherName(), teacherName.trim())) {
                continue;
            }
            if (status != null && !status.isBlank() && !status.equals(c.getStatus())) {
                continue;
            }
            filtered.add(c);
        }
        filtered.sort(Comparator.comparing(CourseCardVO::getCourseName,
                Comparator.nullsLast(String::compareTo)));
        int total = filtered.size();
        int pageSize = size <= 0 ? 12 : size;
        int pages = (int) Math.ceil(total / (double) pageSize);
        int p = page <= 0 ? 1 : page;
        int from = Math.min((p - 1) * pageSize, total);
        int to = Math.min(from + pageSize, total);
        CourseCardPageVO vo = new CourseCardPageVO();
        vo.setItems(filtered.subList(from, to));
        vo.setTotal(total);
        vo.setPage(p);
        vo.setSize(pageSize);
        vo.setPages(Math.max(pages, 1));
        return vo;
    }

    private Long currentTermId() {
        Term current = termMapper.selectOne(Wrappers.<Term>lambdaQuery()
                .eq(Term::getIsCurrent, 1)
                .orderByDesc(Term::getId)
                .last("limit 1"));
        return current == null ? null : current.getId();
    }

    private List<CourseCardVO> allCards(Long studentId, Long termId) {
        // 不指定学期时只看当前学期：历史/合成学期仍保留数据，但不再混进"我的课程"
        if (termId == null) {
            termId = currentTermId();
        }
        List<Enrollment> ens = enrollmentMapper.selectList(Wrappers.<Enrollment>lambdaQuery()
                .eq(Enrollment::getStudentId, studentId));
        List<CourseCardVO> out = new ArrayList<>();
        Map<Long, Term> terms = new LinkedHashMap<>();
        for (Term t : termMapper.selectList(Wrappers.<Term>lambdaQuery())) {
            terms.put(t.getId(), t);
        }
        for (Enrollment e : ens) {
            TeachingClass tc = classMapper.selectById(e.getTeachingClassId());
            if (tc == null) {
                continue;
            }
            // 教师结课后学生端不再显示（数据保留）
            if ("CLOSED".equals(tc.getStatus())) {
                continue;
            }
            CourseOffering o = offeringMapper.selectById(tc.getOfferingId());
            if (o == null) {
                continue;
            }
            if (termId != null && !termId.equals(o.getTermId())) {
                continue;
            }
            Course c = courseMapper.selectById(o.getCourseId());
            Teacher t = teacherMapper.selectById(tc.getTeacherId());
            Term term = terms.get(o.getTermId());
            CourseCardVO v = new CourseCardVO();
            v.setTeachingClassId(tc.getId());
            v.setOfferingId(o.getId());
            v.setCourseName(c == null ? null : c.getCourseName());
            v.setCourseType(c == null ? null : c.getCourseType());
            v.setClassName(tc.getClassName());
            v.setTeacherName(t == null ? null : t.getTeacherName());
            v.setTermId(o.getTermId());
            v.setTermName(term == null ? null : term.getTermName());
            v.setEnrollmentStatus(e.getStatus());
            LocalDate today = LocalDate.now();
            String status = "进行中";
            if (term != null && term.getStartDate() != null && today.isBefore(term.getStartDate())) {
                status = "未开课";
            } else if (term != null && term.getEndDate() != null && today.isAfter(term.getEndDate())) {
                status = "已结束";
            }
            v.setStatus(status);
            v.setStatusText(status);
            v.setCanEnter(!"已结束".equals(status) && !"WITHDRAWN".equals(e.getStatus()));
            out.add(v);
        }
        return out;
    }

    // ============================================================ 课程详情
    public StudentCourseDetailVO detail(Long teachingClassId, Long studentId) {
        Enrollment e = enrollmentMapper.selectOne(Wrappers.<Enrollment>lambdaQuery()
                .eq(Enrollment::getTeachingClassId, teachingClassId)
                .eq(Enrollment::getStudentId, studentId).last("limit 1"));
        if (e == null) {
            throw new BizException("你没有选修这门课");
        }
        TeachingClass tc = classMapper.selectById(teachingClassId);
        if (tc == null) {
            throw new BizException("教学班不存在");
        }
        CourseOffering o = offeringMapper.selectById(tc.getOfferingId());
        Course course = o == null ? null : courseMapper.selectById(o.getCourseId());
        Teacher teacher = teacherMapper.selectById(tc.getTeacherId());
        Term term = o == null ? null : termMapper.selectById(o.getTermId());

        StudentCourseDetailVO v = new StudentCourseDetailVO();
        v.setTeachingClassId(tc.getId());
        v.setOfferingId(tc.getOfferingId());
        v.setCourseName(course == null ? null : course.getCourseName());
        v.setCourseType(course == null ? null : course.getCourseType());
        v.setClassName(tc.getClassName());
        v.setTeacherName(teacher == null ? null : teacher.getTeacherName());
        v.setTermName(term == null ? null : term.getTermName());
        v.setStatusText("CLOSED".equals(tc.getStatus()) ? "已结课" : "进行中");

        // 导学（只给已发布的）
        CourseGuide guide = guideMapper.selectOne(Wrappers.<CourseGuide>lambdaQuery()
                .eq(CourseGuide::getTeachingClassId, teachingClassId).last("limit 1"));
        boolean published = guide != null && guide.getIsPublished() != null && guide.getIsPublished() == 1;
        v.setShowGuide(true);
        if (published) {
            v.setGuideContent(guide.getContent());
            v.setGuideFormat(guide.getContentFormat());
            v.setGuidePublishTime(guide.getPublishTime());
        }

        // 教学目标
        List<StudentCourseDetailVO.Objective> objs = new ArrayList<>();
        for (TeachingObjective t : objectiveMapper.selectList(Wrappers.<TeachingObjective>lambdaQuery()
                .eq(TeachingObjective::getTeachingClassId, teachingClassId)
                .eq(TeachingObjective::getStatus, "ACTIVE")
                .orderByAsc(TeachingObjective::getSortNo))) {
            StudentCourseDetailVO.Objective ob = new StudentCourseDetailVO.Objective();
            ob.setObjCode(t.getObjCode());
            ob.setObjContent(t.getObjContent());
            ob.setObjType(t.getObjType());
            objs.add(ob);
        }
        v.setObjectives(objs);
        v.setShowObjectives(true);
        v.setShowCatalog(true);

        // 目录（只含已发布资源）与两大活动列表
        v.setLessons(lessonsOf(tc, studentId));
        List<StudentCourseDetailVO.PaperBrief> papers = papersOf(tc, studentId);
        List<StudentCourseDetailVO.ProjectBrief> projects = projectsOf(tc, studentId);
        v.setPapers(papers);
        v.setProjects(projects);

        // 页签开关：成绩构成里有对应分类的评分项就出现；
        // 另外已经发布了对应活动（测验/作业/项目）时也出现，避免教师发布后学生看不到入口
        List<ScoreSchemeItem> items = schemeItemsOf(o);
        boolean hasQuiz = hasCategory(items, "QUIZ") || papers.stream().anyMatch(x -> "QUIZ".equals(x.getPaperType()));
        boolean hasHomework = hasCategory(items, "HOMEWORK")
                || papers.stream().anyMatch(x -> "HOMEWORK".equals(x.getPaperType()));
        boolean hasProject = hasCategory(items, "PROJECT") || !projects.isEmpty();
        v.setShowQuiz(hasQuiz);
        v.setShowHomework(hasHomework);
        v.setShowProject(hasProject);
        return v;
    }

    private boolean hasCategory(List<ScoreSchemeItem> items, String category) {
        for (ScoreSchemeItem it : items) {
            if (category.equalsIgnoreCase(it.getCategory())) {
                return true;
            }
        }
        return false;
    }

    private List<ScoreSchemeItem> schemeItemsOf(CourseOffering o) {
        if (o == null) {
            return new ArrayList<>();
        }
        // 课程组：同课程同学期
        List<ScoreScheme> schemes = schemeMapper.selectList(Wrappers.<ScoreScheme>lambdaQuery()
                .eq(ScoreScheme::getTermId, o.getTermId())
                .orderByDesc(ScoreScheme::getVersion));
        for (ScoreScheme s : schemes) {
            List<ScoreSchemeItem> items = schemeItemMapper.selectList(Wrappers.<ScoreSchemeItem>lambdaQuery()
                    .eq(ScoreSchemeItem::getSchemeId, s.getId())
                    .eq(ScoreSchemeItem::getStatus, "ACTIVE"));
            if (!items.isEmpty()) {
                return items;
            }
        }
        return new ArrayList<>();
    }

    private List<StudentLessonVO> lessonsOf(TeachingClass tc, Long studentId) {
        List<CourseLesson> lessons = lessonMapper.selectList(Wrappers.<CourseLesson>lambdaQuery()
                .eq(CourseLesson::getTeachingClassId, tc.getId())
                .eq(CourseLesson::getStatus, "ACTIVE")
                .orderByAsc(CourseLesson::getSortNo));
        if (lessons.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, CourseSession> sessions = new LinkedHashMap<>();
        for (CourseSession s : sessionMapper.selectBatchIds(
                lessons.stream().map(CourseLesson::getSessionId).distinct().collect(Collectors.toList()))) {
            sessions.put(s.getId(), s);
        }
        List<LessonResource> resources = lessonResourceMapper.selectList(Wrappers.<LessonResource>lambdaQuery()
                .eq(LessonResource::getTeachingClassId, tc.getId())
                .eq(LessonResource::getPublishStatus, "PUBLISHED")
                .orderByAsc(LessonResource::getSortNo));
        Map<Long, ResourceFile> files = new LinkedHashMap<>();
        List<Long> fileIds = resources.stream()
                .filter(r -> "FILE".equals(r.getResType()) && r.getRefId() != null)
                .map(LessonResource::getRefId).distinct().collect(Collectors.toList());
        if (!fileIds.isEmpty()) {
            for (ResourceFile f : resourceFileMapper.selectBatchIds(fileIds)) {
                files.put(f.getId(), f);
            }
        }
        Map<Long, Paper> papers = new LinkedHashMap<>();
        List<Long> paperIds = resources.stream()
                .filter(r -> "PAPER".equals(r.getResType()) && r.getRefId() != null)
                .map(LessonResource::getRefId).distinct().collect(Collectors.toList());
        if (!paperIds.isEmpty()) {
            for (Paper p : paperMapper.selectBatchIds(paperIds)) {
                papers.put(p.getId(), p);
            }
        }
        Map<Long, Submission> mySubs = new LinkedHashMap<>();
        if (!paperIds.isEmpty()) {
            for (Submission s : submissionMapper.selectList(Wrappers.<Submission>lambdaQuery()
                    .eq(Submission::getStudentId, studentId)
                    .in(Submission::getPaperId, paperIds))) {
                mySubs.put(s.getPaperId(), s);
            }
        }
        List<StudentLessonVO> out = new ArrayList<>();
        for (CourseLesson l : lessons) {
            StudentLessonVO v = new StudentLessonVO();
            v.setLessonId(l.getId());
            v.setWeekNo(l.getWeekNo());
            v.setSessionNo(l.getSessionNo());
            CourseSession s = sessions.get(l.getSessionId());
            v.setSessionDate(s == null ? null : s.getSessionDate());
            v.setTitle(l.getTitle());
            List<StudentLessonVO.Res> before = new ArrayList<>();
            List<StudentLessonVO.Res> during = new ArrayList<>();
            List<StudentLessonVO.Res> after = new ArrayList<>();
            for (LessonResource r : resources) {
                if (!l.getId().equals(r.getLessonId())) {
                    continue;
                }
                StudentLessonVO.Res res = new StudentLessonVO.Res();
                res.setId(r.getId());
                res.setResType(r.getResType());
                res.setTitle(r.getTitle());
                res.setRefId(r.getRefId());
                res.setExtUrl(r.getExtUrl());
                if ("FILE".equals(r.getResType()) && r.getRefId() != null) {
                    ResourceFile f = files.get(r.getRefId());
                    if (f != null) {
                        res.setFileId(f.getId());
                        res.setFileExt(f.getFileExt());
                    }
                }
                if ("PAPER".equals(r.getResType()) && r.getRefId() != null) {
                    Paper p = papers.get(r.getRefId());
                    if (p != null) {
                        res.setPaperType(p.getPaperType());
                        Submission sub = mySubs.get(p.getId());
                        if (sub == null) {
                            res.setTaskStatus("NOT_STARTED");
                        } else {
                            res.setTaskStatus(sub.getSubmitStatus());
                        }
                    }
                }
                if ("BEFORE".equals(r.getPhase())) {
                    before.add(res);
                } else if ("DURING".equals(r.getPhase())) {
                    during.add(res);
                } else if ("AFTER".equals(r.getPhase())) {
                    after.add(res);
                }
            }
            v.setBefore(before);
            v.setDuring(during);
            v.setAfter(after);
            out.add(v);
        }
        return out;
    }

    private List<StudentCourseDetailVO.PaperBrief> papersOf(TeachingClass tc, Long studentId) {
        List<Paper> papers = paperMapper.selectList(Wrappers.<Paper>lambdaQuery()
                .eq(Paper::getTeachingClassId, tc.getId())
                .in(Paper::getStatus, List.of("PUBLISHED", "CLOSED"))
                .orderByDesc(Paper::getId));
        List<StudentCourseDetailVO.PaperBrief> out = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (Paper p : papers) {
            StudentCourseDetailVO.PaperBrief b = new StudentCourseDetailVO.PaperBrief();
            b.setId(p.getId());
            b.setPaperName(p.getPaperName());
            b.setPaperType(p.getPaperType());
            b.setStartTime(p.getStartTime());
            b.setEndTime(p.getEndTime());
            b.setDurationMinutes(p.getDurationMinutes());
            b.setFullScore(p.getFullScore());
            Submission s = submissionMapper.selectOne(Wrappers.<Submission>lambdaQuery()
                    .eq(Submission::getPaperId, p.getId())
                    .eq(Submission::getStudentId, studentId).last("limit 1"));
            if (s == null) {
                b.setMyStatus("NOT_STARTED");
            } else {
                b.setMyStatus(s.getSubmitStatus());
                b.setMyScore(s.getTotalScore());
            }
            boolean can = s == null && "PUBLISHED".equals(p.getStatus())
                    && (p.getStartTime() == null || !now.isBefore(p.getStartTime()))
                    && (p.getEndTime() == null || !now.isAfter(p.getEndTime()));
            b.setCanAnswer(can);
            if (!can) {
                b.setReason(s != null ? ("ABSENT".equals(s.getSubmitStatus()) ? "缺考" : "已交卷")
                        : (!"PUBLISHED".equals(p.getStatus()) ? "已截止"
                        : (p.getEndTime() != null && now.isAfter(p.getEndTime()) ? "已截止" : "未开始")));
            }
            long qc = questionMapper.selectCount(Wrappers.<Question>lambdaQuery()
                    .eq(Question::getPaperId, p.getId()));
            b.setQuestionCount((int) qc);
            out.add(b);
        }
        return out;
    }

    private List<StudentCourseDetailVO.ProjectBrief> projectsOf(TeachingClass tc, Long studentId) {
        List<ProjectTask> tasks = projectTaskMapper.selectList(Wrappers.<ProjectTask>lambdaQuery()
                .eq(ProjectTask::getTeachingClassId, tc.getId())
                .in(ProjectTask::getStatus, List.of("PUBLISHED", "CLOSED"))
                .orderByDesc(ProjectTask::getId));
        List<StudentCourseDetailVO.ProjectBrief> out = new ArrayList<>();
        for (ProjectTask t : tasks) {
            StudentCourseDetailVO.ProjectBrief b = new StudentCourseDetailVO.ProjectBrief();
            b.setId(t.getId());
            b.setProjectName(t.getProjectName());
            b.setDescription(t.getDescription());
            b.setIsTeam(t.getIsTeam());
            b.setStartTime(t.getStartTime());
            b.setEndTime(t.getEndTime());
            b.setFullScore(t.getFullScore());
            ProjectSubmission s = projectSubmissionMapper.selectOne(Wrappers.<ProjectSubmission>lambdaQuery()
                    .eq(ProjectSubmission::getProjectId, t.getId())
                    .eq(ProjectSubmission::getStudentId, studentId).last("limit 1"));
            b.setMyStatus(s == null ? "NOT_SUBMITTED" : s.getSubmitStatus());
            b.setMyScore(s == null ? null : s.getProjectScore());
            if (s == null && "CLOSED".equals(t.getStatus())) {
                b.setReason("已截止");
            }
            out.add(b);
        }
        return out;
    }

    // ============================================================ 个人中心
    public StudentProfileVO profile(Long studentId) {
        Student st = studentMapper.selectById(studentId);
        if (st == null) {
            throw new BizException("学生档案不存在");
        }
        StudentProfileVO v = new StudentProfileVO();
        v.setStudentId(st.getId());
        v.setStudentNo(st.getStudentNo());
        v.setStudentName(st.getStudentName());
        v.setGender(st.getGender());
        v.setEnrollYear(st.getEnrollYear());
        v.setPhone(st.getPhone());
        v.setEmail(st.getEmail());
        if (st.getMajorId() != null) {
            Major m = majorMapper.selectById(st.getMajorId());
            v.setMajorName(m == null ? null : m.getMajorName());
        }
        if (st.getAdminClassId() != null) {
            AdministrativeClass c = adminClassMapper.selectById(st.getAdminClassId());
            v.setClassName(c == null ? null : c.getClassName());
        }
        if (st.getAccountId() != null) {
            SysAccount a = accountMapper.selectById(st.getAccountId());
            v.setAccountNo(a == null ? null : a.getLoginNo());
        }
        return v;
    }

    // ============================================================ 加入课程（邀请码）
    @Transactional
    public JoinRequestVO apply(String inviteCode, Long studentId) {
        if (inviteCode == null || inviteCode.trim().length() != 6) {
            throw new BizException("请输入 6 位邀请码");
        }
        TeachingClass tc = classMapper.selectOne(Wrappers.<TeachingClass>lambdaQuery()
                .eq(TeachingClass::getInviteCode, inviteCode.trim().toUpperCase()).last("limit 1"));
        if (tc == null) {
            throw new BizException("邀请码不存在或已失效");
        }
        if ("CLOSED".equals(tc.getStatus())) {
            throw new BizException("该教学班已结课，不能再加入");
        }
        long enrolled = enrollmentMapper.selectCount(Wrappers.<Enrollment>lambdaQuery()
                .eq(Enrollment::getTeachingClassId, tc.getId())
                .eq(Enrollment::getStudentId, studentId));
        if (enrolled > 0) {
            throw new BizException("你已在这门课里，不需要重复申请");
        }
        CourseJoinRequest exist = joinRequestMapper.selectOne(Wrappers.<CourseJoinRequest>lambdaQuery()
                .eq(CourseJoinRequest::getTeachingClassId, tc.getId())
                .eq(CourseJoinRequest::getStudentId, studentId)
                .eq(CourseJoinRequest::getStatus, "APPLIED").last("limit 1"));
        if (exist != null) {
            throw new BizException("你已经提交过申请，等待教师同意");
        }
        CourseJoinRequest req = new CourseJoinRequest();
        req.setTeachingClassId(tc.getId());
        req.setOfferingId(tc.getOfferingId());
        CourseOffering o = offeringMapper.selectById(tc.getOfferingId());
        req.setTermId(o == null ? null : o.getTermId());
        req.setStudentId(studentId);
        req.setInviteCode(tc.getInviteCode());
        req.setApplySource("INVITE_CODE");
        req.setApplyTime(LocalDateTime.now());
        boolean review = tc.getJoinReviewEnabled() != null && tc.getJoinReviewEnabled() == 1;
        req.setStatus(review ? "APPLIED" : "AUTO");
        req.setNotifyAdmin(0);
        joinRequestMapper.insert(req);
        if (!review) {
            enroll(tc, studentId);
        }
        return joinVO(req);
    }

    public List<JoinRequestVO> myRequests(Long studentId) {
        List<JoinRequestVO> out = new ArrayList<>();
        for (CourseJoinRequest r : joinRequestMapper.selectList(Wrappers.<CourseJoinRequest>lambdaQuery()
                .eq(CourseJoinRequest::getStudentId, studentId)
                .orderByDesc(CourseJoinRequest::getId))) {
            out.add(joinVO(r));
        }
        return out;
    }

    // ============================================================ 教师端：邀请码与审批
    @Transactional
    public String setInvite(Long teachingClassId, String code, Integer reviewEnabled, Long teacherId) {
        TeachingClass tc = classMapper.selectById(teachingClassId);
        if (tc == null || !teacherId.equals(tc.getTeacherId())) {
            throw new BizException("只能设置自己教学班的邀请码");
        }
        String finalCode = code;
        if (finalCode == null || finalCode.isBlank()) {
            finalCode = randomCode();
        }
        if (finalCode.length() != 6) {
            throw new BizException("邀请码必须是 6 位");
        }
        finalCode = finalCode.toUpperCase();
        TeachingClass dup = classMapper.selectOne(Wrappers.<TeachingClass>lambdaQuery()
                .eq(TeachingClass::getInviteCode, finalCode).last("limit 1"));
        if (dup != null && !dup.getId().equals(teachingClassId)) {
            throw new BizException("邀请码已被其他教学班占用，请换一个");
        }
        tc.setInviteCode(finalCode);
        tc.setJoinReviewEnabled(reviewEnabled == null ? 0 : reviewEnabled);
        classMapper.updateById(tc);
        return finalCode;
    }

    public List<JoinRequestVO> classRequests(Long teachingClassId, Long teacherId, String status) {
        TeachingClass tc = classMapper.selectById(teachingClassId);
        if (tc == null || !teacherId.equals(tc.getTeacherId())) {
            throw new BizException("只能查看自己教学班的申请");
        }
        List<JoinRequestVO> out = new ArrayList<>();
        for (CourseJoinRequest r : joinRequestMapper.selectList(Wrappers.<CourseJoinRequest>lambdaQuery()
                .eq(CourseJoinRequest::getTeachingClassId, teachingClassId)
                .eq(status != null && !status.isEmpty(), CourseJoinRequest::getStatus, status)
                .orderByDesc(CourseJoinRequest::getId))) {
            out.add(joinVO(r));
        }
        return out;
    }

    @Transactional
    public JoinRequestVO review(Long requestId, boolean approve, String note, Long teacherId) {
        CourseJoinRequest r = joinRequestMapper.selectById(requestId);
        if (r == null) {
            throw new BizException("申请不存在");
        }
        TeachingClass tc = classMapper.selectById(r.getTeachingClassId());
        if (tc == null || !teacherId.equals(tc.getTeacherId())) {
            throw new BizException("只能审批自己教学班的申请");
        }
        if (!"APPLIED".equals(r.getStatus())) {
            throw new BizException("该申请已处理（当前：" + r.getStatus() + "）");
        }
        r.setStatus(approve ? "APPROVED" : "REJECTED");
        r.setReviewBy(teacherId);
        r.setReviewTime(LocalDateTime.now());
        r.setReviewNote(note);
        if (approve) {
            enroll(tc, r.getStudentId());
            // 同意后给教务一条知会（教务只收知会，不审批）
            r.setNotifyAdmin(1);
            r.setNotifyTime(LocalDateTime.now());
        }
        joinRequestMapper.updateById(r);
        return joinVO(r);
    }

    private void enroll(TeachingClass tc, Long studentId) {
        long exist = enrollmentMapper.selectCount(Wrappers.<Enrollment>lambdaQuery()
                .eq(Enrollment::getTeachingClassId, tc.getId())
                .eq(Enrollment::getStudentId, studentId));
        if (exist > 0) {
            return;
        }
        CourseOffering o = offeringMapper.selectById(tc.getOfferingId());
        Enrollment e = new Enrollment();
        e.setStudentId(studentId);
        e.setTeachingClassId(tc.getId());
        e.setOfferingId(tc.getOfferingId());
        e.setTermId(o == null ? null : o.getTermId());
        e.setStatus("ENROLLED");
        e.setRetakeTimes(0);
        e.setEnrollTime(LocalDateTime.now());
        enrollmentMapper.insert(e);
    }

    private JoinRequestVO joinVO(CourseJoinRequest r) {
        JoinRequestVO v = new JoinRequestVO();
        v.setId(r.getId());
        v.setTeachingClassId(r.getTeachingClassId());
        TeachingClass tc = classMapper.selectById(r.getTeachingClassId());
        if (tc != null) {
            v.setClassName(tc.getClassName());
            Teacher t = teacherMapper.selectById(tc.getTeacherId());
            v.setTeacherName(t == null ? null : t.getTeacherName());
            CourseOffering o = offeringMapper.selectById(tc.getOfferingId());
            Course c = o == null ? null : courseMapper.selectById(o.getCourseId());
            v.setCourseName(c == null ? null : c.getCourseName());
        }
        Student st = studentMapper.selectById(r.getStudentId());
        if (st != null) {
            v.setStudentId(st.getId());
            v.setStudentNo(st.getStudentNo());
            v.setStudentName(st.getStudentName());
        }
        v.setInviteCode(r.getInviteCode());
        v.setStatus(r.getStatus());
        v.setStatusText(switch (r.getStatus() == null ? "" : r.getStatus()) {
            case "APPLIED" -> "待教师同意";
            case "APPROVED" -> "已同意入班";
            case "REJECTED" -> "已拒绝";
            case "AUTO" -> "免审已入班";
            default -> r.getStatus();
        });
        v.setApplyTime(r.getApplyTime());
        v.setReviewTime(r.getReviewTime());
        v.setReviewNote(r.getReviewNote());
        v.setNotifyAdmin(r.getNotifyAdmin() != null && r.getNotifyAdmin() == 1);
        return v;
    }

    private String randomCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder();
        java.util.Random rnd = new java.util.Random();
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private boolean contains(String text, String kw) {
        return text != null && text.contains(kw);
    }
}

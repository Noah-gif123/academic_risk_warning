package com.example.academic_risk_warning.service;

import com.example.academic_risk_warning.common.BizException;
import com.example.academic_risk_warning.dto.ContributionItemDTO;
import com.example.academic_risk_warning.dto.ContributionSubmitRequest;
import com.example.academic_risk_warning.dto.ProjectSaveRequest;
import com.example.academic_risk_warning.dto.ProjectScoreRequest;
import com.example.academic_risk_warning.dto.ProjectSubmitRequest;
import com.example.academic_risk_warning.dto.TeamSaveRequest;
import com.example.academic_risk_warning.entity.Course;
import com.example.academic_risk_warning.entity.CourseLesson;
import com.example.academic_risk_warning.entity.CourseOffering;
import com.example.academic_risk_warning.entity.Enrollment;
import com.example.academic_risk_warning.entity.ProjectContribution;
import com.example.academic_risk_warning.entity.ProjectSubmission;
import com.example.academic_risk_warning.entity.ProjectTask;
import com.example.academic_risk_warning.entity.ProjectTeam;
import com.example.academic_risk_warning.entity.ResourceFile;
import com.example.academic_risk_warning.entity.ScoreSchemeItem;
import com.example.academic_risk_warning.entity.Student;
import com.example.academic_risk_warning.entity.Teacher;
import com.example.academic_risk_warning.entity.TeachingClass;
import com.example.academic_risk_warning.mapper.CourseLessonMapper;
import com.example.academic_risk_warning.mapper.CourseMapper;
import com.example.academic_risk_warning.mapper.CourseOfferingMapper;
import com.example.academic_risk_warning.mapper.EnrollmentMapper;
import com.example.academic_risk_warning.mapper.ProjectContributionMapper;
import com.example.academic_risk_warning.mapper.ProjectSubmissionMapper;
import com.example.academic_risk_warning.mapper.ProjectTaskMapper;
import com.example.academic_risk_warning.mapper.ProjectTeamMapper;
import com.example.academic_risk_warning.mapper.ResourceFileMapper;
import com.example.academic_risk_warning.mapper.ScoreSchemeItemMapper;
import com.example.academic_risk_warning.mapper.StudentMapper;
import com.example.academic_risk_warning.mapper.TeacherMapper;
import com.example.academic_risk_warning.mapper.TeachingClassMapper;
import com.example.academic_risk_warning.util.JsonObjects;
import com.example.academic_risk_warning.util.MiniJson;
import com.example.academic_risk_warning.vo.ProjectMemberVO;
import com.example.academic_risk_warning.vo.ProjectSubmissionVO;
import com.example.academic_risk_warning.vo.ProjectTeamVO;
import com.example.academic_risk_warning.vo.ProjectVO;
import com.example.academic_risk_warning.vo.StudentProjectVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 项目与贡献度。口径：
 * ① 项目只收文件（Word / PDF / 代码 / PPT / 压缩包 / 仓库链接），逾期不允许补交；
 * ② 团队项目由组长提交一份贡献表，提交即锁定，**教师也没有修改权限**；
 * ③ 个人得分 = 教师给的项目总分 × 个人贡献占比；个人项目占比视为 100%。
 */
@Service
@RequiredArgsConstructor
public class ProjectService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final BigDecimal ONE = BigDecimal.ONE;

    private final ProjectTaskMapper taskMapper;
    private final ProjectTeamMapper teamMapper;
    private final ProjectSubmissionMapper submissionMapper;
    private final ProjectContributionMapper contributionMapper;
    private final EnrollmentMapper enrollmentMapper;
    private final StudentMapper studentMapper;
    private final TeacherMapper teacherMapper;
    private final TeachingClassMapper classMapper;
    private final CourseLessonMapper lessonMapper;
    private final CourseOfferingMapper offeringMapper;
    private final CourseMapper courseMapper;
    private final ResourceFileMapper resourceMapper;
    private final ScoreSchemeItemMapper schemeItemMapper;
    private final CourseContentService contentService;
    private final AuditService auditService;

    // ============================================================ 教师端
    public List<ProjectVO> listProjects(Long teachingClassId, Long teacherId) {
        contentService.requireMyClass(teachingClassId, teacherId);
        List<ProjectTask> list = taskMapper.selectList(Wrappers.<ProjectTask>lambdaQuery()
                .eq(ProjectTask::getTeachingClassId, teachingClassId)
                .orderByDesc(ProjectTask::getId));
        List<ProjectVO> out = new ArrayList<>();
        for (ProjectTask t : list) {
            out.add(toVO(t, false));
        }
        return out;
    }

    public ProjectVO getProject(Long projectId, Long teacherId) {
        ProjectTask t = requireMyProject(projectId, teacherId);
        return toVO(t, true);
    }

    public ProjectTask requireMyProject(Long projectId, Long teacherId) {
        ProjectTask t = taskMapper.selectById(projectId);
        if (t == null) {
            throw new BizException("项目不存在");
        }
        contentService.requireMyClass(t.getTeachingClassId(), teacherId);
        return t;
    }

    @Transactional
    public ProjectVO save(ProjectSaveRequest req, Long teacherId) {
        TeachingClass tc = contentService.requireMyClass(req.getTeachingClassId(), teacherId);
        ProjectTask t = req.getId() == null ? null : taskMapper.selectById(req.getId());
        if (t == null) {
            t = new ProjectTask();
            t.setTeachingClassId(tc.getId());
            t.setOfferingId(tc.getOfferingId());
            t.setTermId(contentService.termIdOf(tc));
            t.setPublisherId(teacherId);
            t.setStatus("DRAFT");
        } else {
            if (!t.getTeachingClassId().equals(tc.getId())) {
                throw new BizException("项目不属于该教学班");
            }
            if ("PUBLISHED".equals(t.getStatus())) {
                throw new BizException("已发布的项目不能修改，请先撤回");
            }
        }
        t.setProjectName(req.getProjectName());
        t.setDescription(req.getDescription());
        t.setLessonId(req.getLessonId());
        t.setStartTime(req.getStartTime());
        t.setEndTime(req.getEndTime());
        t.setIsTeam(req.getIsTeam() == null ? 0 : req.getIsTeam());
        t.setFullScore(req.getFullScore() == null ? new BigDecimal("100") : req.getFullScore());
        t.setAllowFormats(req.getAllowFormats() == null || req.getAllowFormats().isBlank()
                ? "WORD,PDF,CODE,PPT,ZIP,LINK" : req.getAllowFormats().toUpperCase());
        t.setSchemeItemId(req.getSchemeItemId());
        if (t.getId() == null) {
            taskMapper.insert(t);
        } else {
            taskMapper.updateById(t);
        }
        auditService.record("PROJECT_TASK", t.getId(), "SAVE", null, null,
                "保存项目：" + t.getProjectName() + (t.getIsTeam() == 1 ? "（团队）" : "（个人）"));
        return toVO(t, true);
    }

    @Transactional
    public ProjectVO setPublish(Long projectId, boolean publish, Long teacherId) {
        ProjectTask t = requireMyProject(projectId, teacherId);
        if (publish) {
            if (t.getEndTime() == null) {
                throw new BizException("项目逾期不允许补交，发布前必须设置截止时间");
            }
            if (t.getStartTime() != null && t.getStartTime().isAfter(t.getEndTime())) {
                throw new BizException("开始时间不能晚于截止时间");
            }
            if (t.getFullScore() == null || t.getFullScore().signum() <= 0) {
                throw new BizException("项目满分必须大于 0");
            }
            t.setStatus("PUBLISHED");
        } else {
            if ("CLOSED".equals(t.getStatus())) {
                throw new BizException("已截止的项目不能撤回");
            }
            long subs = submissionMapper.selectCount(Wrappers.<ProjectSubmission>lambdaQuery()
                    .eq(ProjectSubmission::getProjectId, projectId)
                    .eq(ProjectSubmission::getSubmitStatus, "SUBMITTED"));
            if (subs > 0) {
                throw new BizException("已有小组/学生提交，不能撤回");
            }
            t.setStatus("DRAFT");
        }
        taskMapper.updateById(t);
        auditService.record("PROJECT_TASK", projectId, publish ? "PUBLISH" : "UNPUBLISH", null, null,
                (publish ? "发布" : "撤回") + "项目：" + t.getProjectName());
        return toVO(t, true);
    }

    @Transactional
    public ProjectVO close(Long projectId, Long teacherId) {
        ProjectTask t = requireMyProject(projectId, teacherId);
        if (t.getEndTime() == null || LocalDateTime.now().isBefore(t.getEndTime())) {
            throw new BizException("还没到截止时间，不能截止");
        }
        t.setStatus("CLOSED");
        taskMapper.updateById(t);
        auditService.record("PROJECT_TASK", projectId, "CLOSE", null, null, "截止项目：" + t.getProjectName());
        return toVO(t, true);
    }

    /** 教师给项目总分（团队项目=整组分） */
    @Transactional
    public ProjectSubmissionVO grade(Long submissionId, ProjectScoreRequest req, Long teacherId) {
        ProjectSubmission s = submissionMapper.selectById(submissionId);
        if (s == null) {
            throw new BizException("提交记录不存在");
        }
        ProjectTask t = requireMyProject(s.getProjectId(), teacherId);
        BigDecimal score = req.getScore() == null ? ZERO : req.getScore();
        if (score.signum() < 0) {
            score = ZERO;
        }
        if (t.getFullScore() != null && score.compareTo(t.getFullScore()) > 0) {
            score = t.getFullScore();
        }
        s.setProjectScore(score);
        s.setComment(req.getComment());
        s.setSubmitStatus("MARKED");
        s.setMarkedBy(teacherId);
        s.setMarkedAt(LocalDateTime.now());
        submissionMapper.updateById(s);
        auditService.record("PROJECT_SUBMISSION", submissionId, "GRADE", null, null,
                "项目评分 " + score + "（" + (s.getTeamId() == null ? "个人" : "团队整组") + "）");
        return toSubmissionVO(s, t, true);
    }

    // ============================================================ 学生端
    public List<StudentProjectVO> listForStudent(Long studentId) {
        List<Enrollment> ens = enrollmentMapper.selectList(Wrappers.<Enrollment>lambdaQuery()
                .eq(Enrollment::getStudentId, studentId));
        if (ens.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> classIds = ens.stream().map(Enrollment::getTeachingClassId).distinct()
                .collect(Collectors.toList());
        List<ProjectTask> list = taskMapper.selectList(Wrappers.<ProjectTask>lambdaQuery()
                .in(ProjectTask::getTeachingClassId, classIds)
                .in(ProjectTask::getStatus, List.of("PUBLISHED", "CLOSED"))
                .orderByDesc(ProjectTask::getId));
        List<StudentProjectVO> out = new ArrayList<>();
        for (ProjectTask t : list) {
            out.add(toStudentVO(t, studentId));
        }
        return out;
    }

    public StudentProjectVO getForStudent(Long projectId, Long studentId) {
        ProjectTask t = taskMapper.selectById(projectId);
        if (t == null) {
            throw new BizException("项目不存在");
        }
        requireEnrollment(t, studentId);
        return toStudentVO(t, studentId);
    }

    /** 组队：本人当组长，成员必须是本教学班在册学生 */
    @Transactional
    public ProjectTeamVO createTeam(Long projectId, TeamSaveRequest req, Long studentId) {
        ProjectTask t = requireOpenProject(projectId, studentId);
        if (t.getIsTeam() != 1) {
            throw new BizException("这是个人项目，不需要组队");
        }
        requireEnrollment(t, studentId);
        ProjectTeam exists = findMyTeam(projectId, studentId);
        if (exists != null) {
            throw new BizException("你已经在组里，不能重复组队");
        }
        List<Long> ids = new ArrayList<>();
        ids.add(studentId);
        if (req.getMemberIds() != null) {
            for (Long id : req.getMemberIds()) {
                if (id != null && !ids.contains(id)) {
                    ids.add(id);
                }
            }
        }
        List<Student> students = studentMapper.selectBatchIds(ids);
        if (students.size() != ids.size()) {
            throw new BizException("成员名单里有学生不存在");
        }
        Map<Long, Enrollment> enrolled = enrollmentMapper.selectList(Wrappers.<Enrollment>lambdaQuery()
                        .eq(Enrollment::getTeachingClassId, t.getTeachingClassId())
                        .in(Enrollment::getStudentId, ids))
                .stream().collect(Collectors.toMap(Enrollment::getStudentId, e -> e, (a, b) -> a));
        for (Long id : ids) {
            if (!enrolled.containsKey(id)) {
                throw new BizException("成员 " + id + " 不是本教学班学生");
            }
        }
        // 成员不能已经在别组
        List<ProjectTeam> teams = teamMapper.selectList(Wrappers.<ProjectTeam>lambdaQuery()
                .eq(ProjectTeam::getProjectId, projectId));
        for (ProjectTeam other : teams) {
            for (Map<String, String> m : JsonObjects.arrayObjects(other.getMemberJson())) {
                String sid = m.get("studentId");
                if (sid != null && ids.contains(Long.parseLong(sid))) {
                    throw new BizException("有成员已在其他组，请调整名单");
                }
            }
        }
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (Long id : ids) {
            Student st = students.stream().filter(s -> s.getId().equals(id)).findFirst().orElse(null);
            if (st == null) {
                continue;
            }
            if (!first) {
                sb.append(',');
            }
            first = false;
            sb.append("{\"studentId\":").append(st.getId())
                    .append(",\"studentNo\":").append(MiniJson.quote(st.getStudentNo()))
                    .append(",\"studentName\":").append(MiniJson.quote(st.getStudentName())).append('}');
        }
        sb.append(']');
        ProjectTeam team = new ProjectTeam();
        team.setProjectId(projectId);
        team.setTeamName(req.getTeamName() == null || req.getTeamName().isBlank()
                ? "第 " + (teams.size() + 1) + " 组" : req.getTeamName());
        team.setLeaderStudentId(studentId);
        team.setMemberJson(sb.toString());
        team.setMemberCount(ids.size());
        teamMapper.insert(team);
        auditService.record("PROJECT_TEAM", team.getId(), "CREATE", null, null,
                "组队：" + team.getTeamName() + "（" + ids.size() + " 人）");
        return teamVO(team, null);
    }

    /** 提交项目：个人项目本人提交，团队项目仅组长提交；逾期不允许补交 */
    @Transactional
    public ProjectSubmissionVO submit(Long projectId, ProjectSubmitRequest req, Long studentId) {
        ProjectTask t = requireOpenProject(projectId, studentId);
        Enrollment e = requireEnrollment(t, studentId);
        ProjectTeam team = null;
        if (t.getIsTeam() == 1) {
            team = findMyTeam(projectId, studentId);
            if (team == null) {
                throw new BizException("团队项目要先组队，才能提交");
            }
            if (!studentId.equals(team.getLeaderStudentId())) {
                throw new BizException("团队项目只能由组长提交");
            }
        }
        LocalDateTime now = LocalDateTime.now();
        if (t.getEndTime() != null && now.isAfter(t.getEndTime())) {
            throw new BizException("已过截止时间，项目不允许补交");
        }
        if (t.getStartTime() != null && now.isBefore(t.getStartTime())) {
            throw new BizException("还没到开始时间");
        }
        List<Long> fileIds = req.getFileIds() == null ? new ArrayList<>() : req.getFileIds();
        boolean hasRepo = req.getRepoUrl() != null && !req.getRepoUrl().isBlank();
        if (fileIds.isEmpty() && !hasRepo) {
            throw new BizException("至少上传一个文件或填写代码仓库链接");
        }
        String allowed = t.getAllowFormats() == null ? "" : t.getAllowFormats().toUpperCase();
        List<String> allowedList = Arrays.asList(allowed.split(","));
        StringBuilder filesJson = new StringBuilder("[");
        boolean first = true;
        for (Long fid : fileIds) {
            ResourceFile f = resourceMapper.selectById(fid);
            if (f == null || (f.getIsVoid() != null && f.getIsVoid() == 1)) {
                throw new BizException("文件不存在或已作废：" + fid);
            }
            String format = formatOf(f.getFileExt());
            if (!allowedList.contains(format)) {
                throw new BizException("不允许提交 " + f.getFileExt() + " 格式（本项目允许：" + allowed + "）");
            }
            if (!first) {
                filesJson.append(',');
            }
            first = false;
            filesJson.append("{\"fileId\":").append(f.getId())
                    .append(",\"name\":").append(MiniJson.quote(f.getFileName()))
                    .append(",\"size\":").append(f.getFileSize() == null ? 0 : f.getFileSize()).append('}');
        }
        filesJson.append(']');
        if (hasRepo && !allowedList.contains("LINK")) {
            throw new BizException("本项目不允许提交仓库链接（允许：" + allowed + "）");
        }

        ProjectSubmission s = team == null
                ? submissionMapper.selectOne(Wrappers.<ProjectSubmission>lambdaQuery()
                        .eq(ProjectSubmission::getProjectId, projectId)
                        .eq(ProjectSubmission::getStudentId, studentId).last("limit 1"))
                : submissionMapper.selectOne(Wrappers.<ProjectSubmission>lambdaQuery()
                        .eq(ProjectSubmission::getProjectId, projectId)
                        .eq(ProjectSubmission::getTeamId, team.getId()).last("limit 1"));
        if (s != null && "MARKED".equals(s.getSubmitStatus())) {
            throw new BizException("教师已批阅，不能再修改提交");
        }
        if (s == null) {
            s = new ProjectSubmission();
            s.setProjectId(projectId);
            s.setTeamId(team == null ? null : team.getId());
            s.setStudentId(studentId);
        }
        s.setSubmitTime(now);
        s.setFilesJson(filesJson.toString());
        s.setRepoUrl(hasRepo ? req.getRepoUrl().trim() : null);
        s.setRemark(req.getRemark());
        s.setSubmitStatus("SUBMITTED");
        if (s.getId() == null) {
            submissionMapper.insert(s);
        } else {
            submissionMapper.updateById(s);
        }
        auditService.record("PROJECT_SUBMISSION", s.getId(), "SUBMIT", null, null,
                "提交项目：" + t.getProjectName() + "（" + (team == null ? "个人" : team.getTeamName()) + "，"
                        + fileIds.size() + " 个文件）");
        return toSubmissionVO(s, t, true);
    }

    /** 组长提交贡献表：合计必须 100%，提交即锁定，此后任何人（含教师）都不能改 */
    @Transactional
    public ProjectTeamVO submitContribution(Long projectId, ContributionSubmitRequest req, Long studentId) {
        ProjectTask t = requireOpenProject(projectId, studentId);
        if (t.getIsTeam() != 1) {
            throw new BizException("个人项目不需要贡献表（按 100% 计）");
        }
        ProjectTeam team = findMyTeam(projectId, studentId);
        if (team == null) {
            throw new BizException("你不在任何组里");
        }
        if (!studentId.equals(team.getLeaderStudentId())) {
            throw new BizException("贡献表只能由组长提交");
        }
        ProjectContribution exist = contributionMapper.selectOne(Wrappers.<ProjectContribution>lambdaQuery()
                .eq(ProjectContribution::getProjectId, projectId)
                .eq(ProjectContribution::getTeamId, team.getId()).last("limit 1"));
        if (exist != null) {
            throw new BizException("贡献表已提交并锁定，任何角色都不能修改（如需更正请联系教务特批）");
        }
        List<Map<String, String>> members = JsonObjects.arrayObjects(team.getMemberJson());
        List<ContributionItemDTO> items = req.getItems() == null ? new ArrayList<>() : req.getItems();
        if (items.isEmpty()) {
            throw new BizException("贡献表不能为空");
        }
        BigDecimal total = BigDecimal.ZERO;
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        List<Long> seen = new ArrayList<>();
        for (ContributionItemDTO it : items) {
            if (it.getStudentId() == null) {
                continue;
            }
            if (seen.contains(it.getStudentId())) {
                throw new BizException("同一个成员出现了两次");
            }
            seen.add(it.getStudentId());
            Map<String, String> m = members.stream()
                    .filter(x -> String.valueOf(it.getStudentId()).equals(x.get("studentId")))
                    .findFirst().orElse(null);
            if (m == null) {
                throw new BizException("贡献表里出现了非本组成员：" + it.getStudentId());
            }
            BigDecimal ratio = it.getRatio() == null ? BigDecimal.ZERO : it.getRatio();
            if (ratio.signum() < 0) {
                throw new BizException("占比不能为负数");
            }
            total = total.add(ratio);
            if (!first) {
                sb.append(',');
            }
            first = false;
            sb.append("{\"studentId\":").append(it.getStudentId())
                    .append(",\"studentNo\":").append(MiniJson.quote(m.get("studentNo")))
                    .append(",\"studentName\":").append(MiniJson.quote(m.get("studentName")))
                    .append(",\"ratio\":").append(ratio.setScale(4, RoundingMode.HALF_UP).toPlainString())
                    .append(",\"note\":").append(MiniJson.quote(it.getNote())).append('}');
        }
        if (seen.size() != members.size()) {
            throw new BizException("贡献表必须覆盖全部 " + members.size() + " 名成员");
        }
        BigDecimal scaled = total.setScale(4, RoundingMode.HALF_UP);
        if (scaled.compareTo(ONE) != 0) {
            throw new BizException("贡献占比合计必须为 100%，当前为 "
                    + scaled.multiply(new BigDecimal("100")).stripTrailingZeros().toPlainString() + "%");
        }
        sb.append(']');
        ProjectContribution c = new ProjectContribution();
        c.setProjectId(projectId);
        c.setTeamId(team.getId());
        c.setLeaderStudentId(studentId);
        c.setContributionJson(sb.toString());
        c.setTotalRatio(scaled);
        c.setSubmitTime(LocalDateTime.now());
        c.setIsLocked(1);
        c.setRemark(req.getRemark());
        contributionMapper.insert(c);
        auditService.record("PROJECT_CONTRIBUTION", c.getId(), "SUBMIT", null, null,
                "组长提交贡献表（" + members.size() + " 人，合计 100%），提交后锁定");
        return teamVO(team, c);
    }

    /** 同班同学（组队时选成员用） */
    public List<Student> listClassmates(Long teachingClassId, Long studentId) {
        requireEnrolledClass(teachingClassId, studentId);
        List<Long> ids = enrollmentMapper.selectList(Wrappers.<Enrollment>lambdaQuery()
                        .eq(Enrollment::getTeachingClassId, teachingClassId))
                .stream().map(Enrollment::getStudentId).distinct().collect(Collectors.toList());
        if (ids.isEmpty()) {
            return new ArrayList<>();
        }
        List<Student> list = studentMapper.selectBatchIds(ids);
        list.sort((a, b) -> a.getStudentNo().compareTo(b.getStudentNo()));
        return list;
    }

    /** 学生上传项目文件前的教学班校验（返回教学班，供上传服务用） */
    public TeachingClass requireEnrolledClass(Long teachingClassId, Long studentId) {
        TeachingClass tc = classMapper.selectById(teachingClassId);
        if (tc == null) {
            throw new BizException("教学班不存在");
        }
        Enrollment e = enrollmentMapper.selectOne(Wrappers.<Enrollment>lambdaQuery()
                .eq(Enrollment::getTeachingClassId, teachingClassId)
                .eq(Enrollment::getStudentId, studentId).last("limit 1"));
        if (e == null) {
            throw new BizException("你没有选修这门课");
        }
        if (e.getStatus() != null && ("WITHDRAWN".equals(e.getStatus()) || "SUSPENDED".equals(e.getStatus()))) {
            throw new BizException("你已不在该教学班（" + e.getStatus() + "）");
        }
        return tc;
    }

    // ============================================================ 内部
    private ProjectTask requireOpenProject(Long projectId, Long studentId) {
        ProjectTask t = taskMapper.selectById(projectId);
        if (t == null) {
            throw new BizException("项目不存在");
        }
        if (!"PUBLISHED".equals(t.getStatus())) {
            throw new BizException("CLOSED".equals(t.getStatus()) ? "项目已截止" : "项目尚未发布");
        }
        return t;
    }

    private Enrollment requireEnrollment(ProjectTask t, Long studentId) {
        Enrollment e = enrollmentMapper.selectOne(Wrappers.<Enrollment>lambdaQuery()
                .eq(Enrollment::getTeachingClassId, t.getTeachingClassId())
                .eq(Enrollment::getStudentId, studentId).last("limit 1"));
        if (e == null) {
            throw new BizException("你没有选修这门课");
        }
        if (e.getStatus() != null && ("WITHDRAWN".equals(e.getStatus()) || "SUSPENDED".equals(e.getStatus()))) {
            throw new BizException("你已不在该教学班（" + e.getStatus() + "）");
        }
        return e;
    }

    private ProjectTeam findMyTeam(Long projectId, Long studentId) {
        List<ProjectTeam> teams = teamMapper.selectList(Wrappers.<ProjectTeam>lambdaQuery()
                .eq(ProjectTeam::getProjectId, projectId));
        for (ProjectTeam team : teams) {
            for (Map<String, String> m : JsonObjects.arrayObjects(team.getMemberJson())) {
                if (String.valueOf(studentId).equals(m.get("studentId"))) {
                    return team;
                }
            }
        }
        return null;
    }

    private String formatOf(String ext) {
        if (ext == null) {
            return "OTHER";
        }
        String e = ext.toLowerCase();
        switch (e) {
            case "doc":
            case "docx":
                return "WORD";
            case "pdf":
                return "PDF";
            case "ppt":
            case "pptx":
                return "PPT";
            case "java":
            case "py":
            case "c":
            case "cpp":
            case "h":
            case "js":
            case "ts":
            case "sql":
            case "html":
            case "css":
                return "CODE";
            case "zip":
            case "rar":
            case "7z":
            case "tar":
            case "gz":
                return "ZIP";
            default:
                return "OTHER";
        }
    }

    private ProjectVO toVO(ProjectTask t, boolean withDetail) {
        ProjectVO v = new ProjectVO();
        v.setId(t.getId());
        v.setTeachingClassId(t.getTeachingClassId());
        v.setProjectName(t.getProjectName());
        v.setDescription(t.getDescription());
        v.setLessonId(t.getLessonId());
        if (t.getLessonId() != null) {
            CourseLesson l = lessonMapper.selectById(t.getLessonId());
            if (l != null) {
                v.setWeekNo(l.getWeekNo());
                v.setSessionNo(l.getSessionNo());
            }
        }
        v.setStartTime(t.getStartTime());
        v.setEndTime(t.getEndTime());
        v.setIsTeam(t.getIsTeam());
        v.setFullScore(t.getFullScore());
        v.setAllowFormats(t.getAllowFormats());
        v.setSchemeItemId(t.getSchemeItemId());
        if (t.getSchemeItemId() != null) {
            ScoreSchemeItem item = schemeItemMapper.selectById(t.getSchemeItemId());
            v.setSchemeItemName(item == null ? null : item.getItemName());
        }
        v.setStatus(t.getStatus());
        List<ProjectTeam> teams = teamMapper.selectList(Wrappers.<ProjectTeam>lambdaQuery()
                .eq(ProjectTeam::getProjectId, t.getId()));
        v.setTeamCount(teams.size());
        List<ProjectSubmission> subs = submissionMapper.selectList(Wrappers.<ProjectSubmission>lambdaQuery()
                .eq(ProjectSubmission::getProjectId, t.getId()));
        v.setSubmitCount((int) subs.stream()
                .filter(s -> !"NOT_SUBMITTED".equals(s.getSubmitStatus())).count());
        v.setMarkedCount((int) subs.stream()
                .filter(s -> "MARKED".equals(s.getSubmitStatus())).count());
        v.setTotalStudents(enrollmentMapper.selectCount(Wrappers.<Enrollment>lambdaQuery()
                .eq(Enrollment::getTeachingClassId, t.getTeachingClassId())).intValue());
        if (t.getIsTeam() != null && t.getIsTeam() == 1) {
            int missing = 0;
            for (ProjectTeam team : teams) {
                long c = contributionMapper.selectCount(Wrappers.<ProjectContribution>lambdaQuery()
                        .eq(ProjectContribution::getProjectId, t.getId())
                        .eq(ProjectContribution::getTeamId, team.getId()));
                if (c == 0) {
                    missing++;
                }
            }
            v.setMissingContributionCount(missing);
        } else {
            v.setMissingContributionCount(0);
        }
        if (withDetail) {
            List<ProjectTeamVO> teamVOs = new ArrayList<>();
            for (ProjectTeam team : teams) {
                ProjectContribution c = contributionMapper.selectOne(
                        Wrappers.<ProjectContribution>lambdaQuery()
                                .eq(ProjectContribution::getProjectId, t.getId())
                                .eq(ProjectContribution::getTeamId, team.getId()).last("limit 1"));
                teamVOs.add(teamVO(team, c));
            }
            v.setTeams(teamVOs);
            List<ProjectSubmissionVO> subVOs = new ArrayList<>();
            for (ProjectSubmission s : subs) {
                subVOs.add(toSubmissionVO(s, t, true));
            }
            v.setSubmissions(subVOs);
        }
        return v;
    }

    private StudentProjectVO toStudentVO(ProjectTask t, Long studentId) {
        StudentProjectVO v = new StudentProjectVO();
        v.setId(t.getId());
        v.setTeachingClassId(t.getTeachingClassId());
        v.setProjectName(t.getProjectName());
        v.setDescription(t.getDescription());
        v.setIsTeam(t.getIsTeam());
        v.setStartTime(t.getStartTime());
        v.setEndTime(t.getEndTime());
        v.setFullScore(t.getFullScore());
        v.setAllowFormats(t.getAllowFormats());
        v.setStatus(t.getStatus());
        TeachingClass tc = classMapper.selectById(t.getTeachingClassId());
        if (tc != null) {
            CourseOffering o = offeringMapper.selectById(tc.getOfferingId());
            if (o != null) {
                Course c = courseMapper.selectById(o.getCourseId());
                v.setCourseName(c == null ? null : c.getCourseName());
            }
            Teacher teacher = teacherMapper.selectById(tc.getTeacherId());
            v.setTeacherName(teacher == null ? null : teacher.getTeacherName());
        }
        LocalDateTime now = LocalDateTime.now();
        boolean open = "PUBLISHED".equals(t.getStatus())
                && (t.getStartTime() == null || !now.isBefore(t.getStartTime()))
                && (t.getEndTime() == null || !now.isAfter(t.getEndTime()));
        ProjectTeam team = findMyTeam(t.getId(), studentId);
        if (team != null) {
            ProjectContribution c = contributionMapper.selectOne(Wrappers.<ProjectContribution>lambdaQuery()
                    .eq(ProjectContribution::getProjectId, t.getId())
                    .eq(ProjectContribution::getTeamId, team.getId()).last("limit 1"));
            v.setMyTeam(teamVO(team, c));
            v.setTeamMembers(teamVO(team, c).getMembers());
            v.setIsLeader(studentId.equals(team.getLeaderStudentId()));
            v.setContributionSubmitted(c != null);
        } else {
            v.setIsLeader(false);
            v.setContributionSubmitted(false);
        }
        v.setCanCreateTeam(open && t.getIsTeam() != null && t.getIsTeam() == 1 && team == null);
        ProjectSubmission s = team == null
                ? submissionMapper.selectOne(Wrappers.<ProjectSubmission>lambdaQuery()
                        .eq(ProjectSubmission::getProjectId, t.getId())
                        .eq(ProjectSubmission::getStudentId, studentId).last("limit 1"))
                : submissionMapper.selectOne(Wrappers.<ProjectSubmission>lambdaQuery()
                        .eq(ProjectSubmission::getProjectId, t.getId())
                        .eq(ProjectSubmission::getTeamId, team.getId()).last("limit 1"));
        if (s != null) {
            v.setMySubmission(toSubmissionVO(s, t, true));
        }
        boolean canSubmit = open && (t.getIsTeam() != 1 || (team != null && studentId.equals(team.getLeaderStudentId())))
                && (s == null || !"MARKED".equals(s.getSubmitStatus()));
        v.setCanSubmit(canSubmit);
        v.setReason(reasonOf(t, open, team, s, studentId));
        return v;
    }

    private String reasonOf(ProjectTask t, boolean open, ProjectTeam team, ProjectSubmission s, Long studentId) {
        if ("DRAFT".equals(t.getStatus())) {
            return "项目未发布";
        }
        if (s != null && "MARKED".equals(s.getSubmitStatus())) {
            return "已批阅";
        }
        if (t.getIsTeam() != null && t.getIsTeam() == 1 && team == null) {
            return open ? "需要先组队（你是组长才能提交）" : "已截止";
        }
        if (t.getIsTeam() != null && t.getIsTeam() == 1 && team != null
                && !studentId.equals(team.getLeaderStudentId())) {
            return open ? "团队项目由组长提交" : "已截止";
        }
        if (!open) {
            return "已截止（逾期不允许补交）";
        }
        return null;
    }

    private ProjectTeamVO teamVO(ProjectTeam team, ProjectContribution c) {
        ProjectTeamVO v = new ProjectTeamVO();
        v.setId(team.getId());
        v.setTeamName(team.getTeamName());
        v.setLeaderStudentId(team.getLeaderStudentId());
        v.setMemberCount(team.getMemberCount());
        List<Map<String, String>> members = JsonObjects.arrayObjects(team.getMemberJson());
        Map<Long, BigDecimal> ratios = new LinkedHashMap<>();
        Map<Long, String> notes = new LinkedHashMap<>();
        if (c != null) {
            for (Map<String, String> m : JsonObjects.arrayObjects(c.getContributionJson())) {
                try {
                    ratios.put(Long.parseLong(m.get("studentId")), new BigDecimal(m.get("ratio")));
                    notes.put(Long.parseLong(m.get("studentId")), m.get("note"));
                } catch (RuntimeException ignored) {
                    // 数据不完整时按无占比处理
                }
            }
            v.setContributionLocked(c.getIsLocked() != null && c.getIsLocked() == 1);
            v.setTotalRatio(c.getTotalRatio());
            v.setContributionTime(c.getSubmitTime());
            v.setContributionRemark(c.getRemark());
        } else {
            v.setContributionLocked(false);
        }
        List<ProjectMemberVO> list = new ArrayList<>();
        for (Map<String, String> m : members) {
            ProjectMemberVO mv = new ProjectMemberVO();
            try {
                mv.setStudentId(Long.parseLong(m.get("studentId")));
            } catch (RuntimeException ignored) {
                continue;
            }
            mv.setStudentNo(m.get("studentNo"));
            mv.setStudentName(m.get("studentName"));
            mv.setLeader(mv.getStudentId().equals(team.getLeaderStudentId()));
            mv.setRatio(ratios.get(mv.getStudentId()));
            mv.setNote(notes.get(mv.getStudentId()));
            if (mv.getLeader() != null && mv.getLeader() && mv.getStudentNo() != null) {
                v.setLeaderStudentNo(mv.getStudentNo());
                v.setLeaderStudentName(mv.getStudentName());
            }
            list.add(mv);
        }
        list.sort((a, b) -> Boolean.compare(b.getLeader() == null ? false : b.getLeader(),
                a.getLeader() == null ? false : a.getLeader()));
        v.setMembers(list);
        return v;
    }

    private ProjectSubmissionVO toSubmissionVO(ProjectSubmission s, ProjectTask t, boolean withMembers) {
        ProjectSubmissionVO v = new ProjectSubmissionVO();
        v.setId(s.getId());
        v.setProjectId(s.getProjectId());
        v.setTeamId(s.getTeamId());
        v.setStudentId(s.getStudentId());
        v.setSubmitTime(s.getSubmitTime());
        v.setFiles(JsonObjects.arrayObjects(s.getFilesJson()));
        v.setRepoUrl(s.getRepoUrl());
        v.setRemark(s.getRemark());
        v.setSubmitStatus(s.getSubmitStatus());
        v.setProjectScore(s.getProjectScore());
        v.setComment(s.getComment());
        v.setMarkedAt(s.getMarkedAt());
        if (s.getMarkedBy() != null) {
            Teacher teacher = teacherMapper.selectById(s.getMarkedBy());
            v.setMarkedByName(teacher == null ? null : teacher.getTeacherName());
        }
        Student st = studentMapper.selectById(s.getStudentId());
        if (st != null) {
            v.setStudentNo(st.getStudentNo());
            v.setStudentName(st.getStudentName());
        }
        if (s.getTeamId() != null) {
            ProjectTeam team = teamMapper.selectById(s.getTeamId());
            if (team != null) {
                v.setTeamName(team.getTeamName());
                ProjectContribution c = contributionMapper.selectOne(
                        Wrappers.<ProjectContribution>lambdaQuery()
                                .eq(ProjectContribution::getProjectId, s.getProjectId())
                                .eq(ProjectContribution::getTeamId, team.getId()).last("limit 1"));
                ProjectTeamVO tv = teamVO(team, c);
                if (withMembers) {
                    BigDecimal total = s.getProjectScore();
                    for (ProjectMemberVO m : tv.getMembers()) {
                        if (total != null && m.getRatio() != null) {
                            m.setPersonalScore(total.multiply(m.getRatio())
                                    .setScale(2, RoundingMode.HALF_UP));
                        }
                    }
                    v.setMembers(tv.getMembers());
                }
            }
        } else if (withMembers) {
            ProjectMemberVO self = new ProjectMemberVO();
            self.setStudentId(s.getStudentId());
            self.setStudentNo(v.getStudentNo());
            self.setStudentName(v.getStudentName());
            self.setLeader(true);
            self.setRatio(ONE);
            if (s.getProjectScore() != null) {
                self.setPersonalScore(s.getProjectScore().setScale(2, RoundingMode.HALF_UP));
            }
            List<ProjectMemberVO> one = new ArrayList<>();
            one.add(self);
            v.setMembers(one);
        }
        return v;
    }
}

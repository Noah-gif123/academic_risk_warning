package com.example.academic_risk_warning.service;

import com.example.academic_risk_warning.common.BizException;
import com.example.academic_risk_warning.dto.GuideSaveRequest;
import com.example.academic_risk_warning.dto.LessonResourceSaveRequest;
import com.example.academic_risk_warning.dto.LessonUpdateRequest;
import com.example.academic_risk_warning.dto.ObjectiveItemDTO;
import com.example.academic_risk_warning.dto.ObjectiveSaveRequest;
import com.example.academic_risk_warning.entity.CourseGuide;
import com.example.academic_risk_warning.entity.CourseLesson;
import com.example.academic_risk_warning.entity.CourseOffering;
import com.example.academic_risk_warning.entity.CourseSession;
import com.example.academic_risk_warning.entity.LessonResource;
import com.example.academic_risk_warning.entity.Paper;
import com.example.academic_risk_warning.entity.ResourceFile;
import com.example.academic_risk_warning.entity.TeachingClass;
import com.example.academic_risk_warning.entity.TeachingObjective;
import com.example.academic_risk_warning.mapper.CourseGuideMapper;
import com.example.academic_risk_warning.mapper.CourseLessonMapper;
import com.example.academic_risk_warning.mapper.CourseOfferingMapper;
import com.example.academic_risk_warning.mapper.CourseSessionMapper;
import com.example.academic_risk_warning.mapper.LessonResourceMapper;
import com.example.academic_risk_warning.mapper.PaperMapper;
import com.example.academic_risk_warning.mapper.ResourceFileMapper;
import com.example.academic_risk_warning.mapper.TeachingClassMapper;
import com.example.academic_risk_warning.mapper.TeachingObjectiveMapper;
import com.example.academic_risk_warning.vo.LessonResourceVO;
import com.example.academic_risk_warning.vo.LessonVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 课程内容：导学（富文本）、教学目标、目录（一行 = 一次课）、课时三段资源（课前 / 课中 / 课后）。
 * 口径：① 每位任课教师只维护自己教学班的那份；② 目录绑定课次，课中任务据此自动挂到当次课；
 * ③ 资源未发布则学生端不可见；④ 文件只存元信息，本体落磁盘目录。
 */
@Service
@RequiredArgsConstructor
public class CourseContentService {

    private final CourseGuideMapper guideMapper;
    private final TeachingObjectiveMapper objectiveMapper;
    private final ResourceFileMapper resourceMapper;
    private final CourseLessonMapper lessonMapper;
    private final LessonResourceMapper lessonResourceMapper;
    private final CourseSessionMapper sessionMapper;
    private final CourseOfferingMapper offeringMapper;
    private final TeachingClassMapper classMapper;
    private final PaperMapper paperMapper;
    private final AuditService auditService;

    /** 教学班所属学期（教学班只存 offering_id，学期从开课实例取） */
    public Long termIdOf(TeachingClass tc) {
        CourseOffering o = offeringMapper.selectById(tc.getOfferingId());
        return o == null ? null : o.getTermId();
    }

    // ============================================================ 权限
    /** 只能操作自己的教学班 */
    public TeachingClass requireMyClass(Long teachingClassId, Long teacherId) {
        TeachingClass tc = classMapper.selectById(teachingClassId);
        if (tc == null) {
            throw new BizException("教学班不存在");
        }
        if (teacherId != null && !teacherId.equals(tc.getTeacherId())) {
            throw new BizException("只能维护自己教学班的课程内容");
        }
        return tc;
    }

    private TeachingClass requireMyLessonClass(Long lessonId, Long teacherId) {
        CourseLesson lesson = lessonMapper.selectById(lessonId);
        if (lesson == null) {
            throw new BizException("课时不存在");
        }
        requireMyClass(lesson.getTeachingClassId(), teacherId);
        return classMapper.selectById(lesson.getTeachingClassId());
    }

    // ============================================================ 导学
    public CourseGuide getGuide(Long teachingClassId, Long teacherId) {
        requireMyClass(teachingClassId, teacherId);
        CourseGuide guide = guideMapper.selectOne(Wrappers.<CourseGuide>lambdaQuery()
                .eq(CourseGuide::getTeachingClassId, teachingClassId).last("limit 1"));
        if (guide == null) {
            guide = new CourseGuide();
            guide.setTeachingClassId(teachingClassId);
            guide.setContent("");
            guide.setContentFormat("HTML");
            guide.setVersion(0);
            guide.setIsPublished(0);
        }
        return guide;
    }

    @Transactional
    public CourseGuide saveGuide(GuideSaveRequest req, Long teacherId) {
        TeachingClass tc = requireMyClass(req.getTeachingClassId(), teacherId);
        CourseGuide guide = guideMapper.selectOne(Wrappers.<CourseGuide>lambdaQuery()
                .eq(CourseGuide::getTeachingClassId, req.getTeachingClassId()).last("limit 1"));
        boolean publish = Boolean.TRUE.equals(req.getPublish());
        if (guide == null) {
            guide = new CourseGuide();
            guide.setTeachingClassId(tc.getId());
            guide.setOfferingId(tc.getOfferingId());
            guide.setTermId(termIdOf(tc));
            guide.setContent(req.getContent());
            guide.setContentFormat(req.getContentFormat() == null ? "HTML" : req.getContentFormat());
            guide.setVersion(1);
            guide.setIsPublished(publish ? 1 : 0);
            guide.setPublishTime(publish ? LocalDateTime.now() : null);
            guideMapper.insert(guide);
        } else {
            guide.setContent(req.getContent());
            guide.setContentFormat(req.getContentFormat() == null ? "HTML" : req.getContentFormat());
            guide.setVersion(guide.getVersion() == null ? 1 : guide.getVersion() + 1);
            if (publish) {
                guide.setIsPublished(1);
                guide.setPublishTime(LocalDateTime.now());
            }
            guideMapper.updateById(guide);
        }
        auditService.record("COURSE_GUIDE", guide.getId(), "SAVE", null, null,
                (publish ? "保存并发布" : "保存") + "课程导学 v" + guide.getVersion());
        return guide;
    }

    // ============================================================ 教学目标
    public List<TeachingObjective> listObjectives(Long teachingClassId, Long teacherId) {
        requireMyClass(teachingClassId, teacherId);
        return objectiveMapper.selectList(Wrappers.<TeachingObjective>lambdaQuery()
                .eq(TeachingObjective::getTeachingClassId, teachingClassId)
                .eq(TeachingObjective::getStatus, "ACTIVE")
                .orderByAsc(TeachingObjective::getSortNo)
                .orderByAsc(TeachingObjective::getId));
    }

    /** 整批保存：提交的清单为准，未提交的停用（不物理删除，保留历史） */
    @Transactional
    public List<TeachingObjective> saveObjectives(ObjectiveSaveRequest req, Long teacherId) {
        TeachingClass tc = requireMyClass(req.getTeachingClassId(), teacherId);
        List<ObjectiveItemDTO> items = req.getItems() == null ? new ArrayList<>() : req.getItems();
        List<TeachingObjective> exists =
                objectiveMapper.selectList(Wrappers.<TeachingObjective>lambdaQuery()
                        .eq(TeachingObjective::getTeachingClassId, tc.getId())
                        .eq(TeachingObjective::getStatus, "ACTIVE"));
        Map<Long, TeachingObjective> byId = exists.stream()
                .collect(Collectors.toMap(TeachingObjective::getId, o -> o, (a, b) -> a));
        int sort = 0;
        List<Long> kept = new ArrayList<>();
        for (ObjectiveItemDTO d : items) {
            sort++;
            if (d.getObjContent() == null || d.getObjContent().trim().isEmpty()) {
                continue;
            }
            TeachingObjective o =
                    d.getId() == null ? null : byId.get(d.getId());
            if (o == null) {
                o = new TeachingObjective();
                o.setTeachingClassId(tc.getId());
                o.setOfferingId(tc.getOfferingId());
                o.setTermId(termIdOf(tc));
                o.setObjCode(d.getObjCode() == null || d.getObjCode().trim().isEmpty()
                        ? ("OBJ-" + sort) : d.getObjCode().trim());
                o.setObjContent(d.getObjContent().trim());
                o.setObjType(d.getObjType());
                o.setSortNo(d.getSortNo() == null ? sort : d.getSortNo());
                o.setStatus("ACTIVE");
                objectiveMapper.insert(o);
            } else {
                o.setObjCode(d.getObjCode() == null || d.getObjCode().trim().isEmpty()
                        ? ("OBJ-" + sort) : d.getObjCode().trim());
                o.setObjContent(d.getObjContent().trim());
                o.setObjType(d.getObjType());
                o.setSortNo(d.getSortNo() == null ? sort : d.getSortNo());
                objectiveMapper.updateById(o);
            }
            kept.add(o.getId());
        }
        for (TeachingObjective o : exists) {
            if (!kept.contains(o.getId())) {
                o.setStatus("DISABLED");
                objectiveMapper.updateById(o);
            }
        }
        auditService.record("TEACHING_OBJECTIVE", tc.getId(), "SAVE", null, null,
                "保存教学目标 " + kept.size() + " 条");
        return listObjectives(tc.getId(), teacherId);
    }

    // ============================================================ 目录（课时）
    /** 从课次生成目录行（幂等：已存在的只补空缺） */
    @Transactional
    public List<LessonVO> syncLessons(Long teachingClassId, Long teacherId) {
        TeachingClass tc = requireMyClass(teachingClassId, teacherId);
        List<CourseSession> sessions = sessionMapper.selectList(Wrappers.<CourseSession>lambdaQuery()
                .eq(CourseSession::getTeachingClassId, teachingClassId)
                .eq(CourseSession::getIsVoid, 0)
                .orderByAsc(CourseSession::getSessionNo));
        List<CourseLesson> lessons = lessonMapper.selectList(Wrappers.<CourseLesson>lambdaQuery()
                .eq(CourseLesson::getTeachingClassId, teachingClassId));
        Map<Long, CourseLesson> bySession = lessons.stream()
                .collect(Collectors.toMap(CourseLesson::getSessionId, l -> l, (a, b) -> a));
        int added = 0;
        for (CourseSession s : sessions) {
            if (bySession.containsKey(s.getId())) {
                continue;
            }
            CourseLesson l = new CourseLesson();
            l.setTeachingClassId(tc.getId());
            l.setOfferingId(tc.getOfferingId());
            l.setTermId(termIdOf(tc));
            l.setSessionId(s.getId());
            l.setWeekNo(s.getWeekNo());
            l.setSessionNo(s.getSessionNo());
            l.setSortNo(s.getSessionNo());
            l.setStatus("ACTIVE");
            lessonMapper.insert(l);
            added++;
        }
        if (added > 0) {
            auditService.record("COURSE_LESSON", tc.getId(), "SYNC", null, null,
                    "按课次生成目录 " + added + " 行");
        }
        return listLessons(teachingClassId, teacherId);
    }

    public List<LessonVO> listLessons(Long teachingClassId, Long teacherId) {
        requireMyClass(teachingClassId, teacherId);
        List<CourseLesson> lessons = lessonMapper.selectList(Wrappers.<CourseLesson>lambdaQuery()
                .eq(CourseLesson::getTeachingClassId, teachingClassId)
                .orderByAsc(CourseLesson::getSortNo).orderByAsc(CourseLesson::getSessionNo));
        Map<Long, CourseSession> sessions = new LinkedHashMap<>();
        if (!lessons.isEmpty()) {
            for (CourseSession s : sessionMapper.selectBatchIds(
                    lessons.stream().map(CourseLesson::getSessionId).distinct().collect(Collectors.toList()))) {
                sessions.put(s.getId(), s);
            }
        }
        Map<Long, List<LessonResource>> resources = lessonResourceMapper.selectList(
                        Wrappers.<LessonResource>lambdaQuery()
                                .eq(LessonResource::getTeachingClassId, teachingClassId)
                                .orderByAsc(LessonResource::getSortNo))
                .stream().collect(Collectors.groupingBy(LessonResource::getLessonId));
        List<LessonVO> out = new ArrayList<>();
        for (CourseLesson l : lessons) {
            LessonVO v = new LessonVO();
            v.setId(l.getId());
            v.setSessionId(l.getSessionId());
            v.setWeekNo(l.getWeekNo());
            v.setSessionNo(l.getSessionNo());
            CourseSession s = sessions.get(l.getSessionId());
            v.setSessionDate(s == null ? null : s.getSessionDate());
            v.setTitle(l.getTitle());
            v.setSortNo(l.getSortNo());
            v.setStatus(l.getStatus());
            List<LessonResourceVO> all = toResourceVOs(resources.getOrDefault(l.getId(), new ArrayList<>()));
            v.setBefore(all.stream().filter(r -> "BEFORE".equals(r.getPhase())).collect(Collectors.toList()));
            v.setDuring(all.stream().filter(r -> "DURING".equals(r.getPhase())).collect(Collectors.toList()));
            v.setAfter(all.stream().filter(r -> "AFTER".equals(r.getPhase())).collect(Collectors.toList()));
            out.add(v);
        }
        return out;
    }

    @Transactional
    public LessonVO updateLesson(Long lessonId, LessonUpdateRequest req, Long teacherId) {
        CourseLesson l = lessonMapper.selectById(lessonId);
        if (l == null) {
            throw new BizException("课时不存在");
        }
        requireMyClass(l.getTeachingClassId(), teacherId);
        if (req.getTitle() != null) {
            l.setTitle(req.getTitle());
        }
        if (req.getSortNo() != null) {
            l.setSortNo(req.getSortNo());
        }
        if (req.getStatus() != null) {
            l.setStatus(req.getStatus());
        }
        lessonMapper.updateById(l);
        List<LessonVO> all = listLessons(l.getTeachingClassId(), teacherId);
        return all.stream().filter(v -> lessonId.equals(v.getId())).findFirst().orElse(null);
    }

    // ============================================================ 课时资源
    @Transactional
    public LessonResourceVO addResource(LessonResourceSaveRequest req, Long teacherId) {
        TeachingClass tc = requireMyLessonClass(req.getLessonId(), teacherId);
        String type = req.getResType().toUpperCase();
        if (!List.of("FILE", "LINK", "TEXT", "PAPER", "PROJECT").contains(type)) {
            throw new BizException("资源类型不合法");
        }
        if ("LINK".equals(type) && (req.getExtUrl() == null || req.getExtUrl().trim().isEmpty())) {
            throw new BizException("外链资源必须填写地址");
        }
        if (List.of("FILE", "PAPER", "PROJECT").contains(type) && req.getRefId() == null) {
            throw new BizException("该类型必须选择关联对象");
        }
        String title = req.getTitle();
        if (title == null || title.trim().isEmpty()) {
            title = defaultTitle(type, req.getRefId(), req.getExtUrl());
        }
        boolean publish = Boolean.TRUE.equals(req.getPublish());
        long sort = lessonResourceMapper.selectCount(Wrappers.<LessonResource>lambdaQuery()
                .eq(LessonResource::getLessonId, req.getLessonId())
                .eq(LessonResource::getPhase, req.getPhase()));
        LessonResource r = new LessonResource();
        r.setLessonId(req.getLessonId());
        r.setTeachingClassId(tc.getId());
        r.setTermId(termIdOf(tc));
        r.setPhase(req.getPhase().toUpperCase());
        r.setResType(type);
        r.setRefId(req.getRefId());
        r.setExtUrl(req.getExtUrl());
        r.setTitle(title);
        r.setSortNo((int) sort + 1);
        r.setPublishStatus(publish ? "PUBLISHED" : "DRAFT");
        r.setPublishTime(publish ? LocalDateTime.now() : null);
        lessonResourceMapper.insert(r);
        auditService.record("LESSON_RESOURCE", r.getId(), "ADD", null, null,
                "课时 " + req.getLessonId() + " 的 " + r.getPhase() + " 增加资源：" + title);
        return toResourceVOs(List.of(r)).get(0);
    }

    private String defaultTitle(String type, Long refId, String extUrl) {
        if ("LINK".equals(type)) {
            return extUrl;
        }
        if ("FILE".equals(type) && refId != null) {
            ResourceFile f = resourceMapper.selectById(refId);
            return f == null ? "文件资源" : f.getFileName();
        }
        if ("PAPER".equals(type) && refId != null) {
            Paper p = paperMapper.selectById(refId);
            return p == null ? "测验/作业" : p.getPaperName();
        }
        return type;
    }

    @Transactional
    public LessonResourceVO publishResource(Long id, boolean publish, Long teacherId) {
        LessonResource r = lessonResourceMapper.selectById(id);
        if (r == null) {
            throw new BizException("资源不存在");
        }
        requireMyClass(r.getTeachingClassId(), teacherId);
        r.setPublishStatus(publish ? "PUBLISHED" : "DRAFT");
        r.setPublishTime(publish ? LocalDateTime.now() : null);
        lessonResourceMapper.updateById(r);
        return toResourceVOs(List.of(r)).get(0);
    }

    @Transactional
    public void deleteResource(Long id, Long teacherId) {
        LessonResource r = lessonResourceMapper.selectById(id);
        if (r == null) {
            return;
        }
        requireMyClass(r.getTeachingClassId(), teacherId);
        lessonResourceMapper.deleteById(id);
        auditService.record("LESSON_RESOURCE", id, "DELETE", null, null, "删除课时资源：" + r.getTitle());
    }

    private List<LessonResourceVO> toResourceVOs(List<LessonResource> list) {
        Map<Long, ResourceFile> files = new LinkedHashMap<>();
        List<Long> fileIds = list.stream()
                .filter(r -> "FILE".equals(r.getResType()) && r.getRefId() != null)
                .map(LessonResource::getRefId).distinct().collect(Collectors.toList());
        if (!fileIds.isEmpty()) {
            for (ResourceFile f : resourceMapper.selectBatchIds(fileIds)) {
                files.put(f.getId(), f);
            }
        }
        List<LessonResourceVO> out = new ArrayList<>();
        for (LessonResource r : list) {
            LessonResourceVO v = new LessonResourceVO();
            v.setId(r.getId());
            v.setLessonId(r.getLessonId());
            v.setPhase(r.getPhase());
            v.setResType(r.getResType());
            v.setRefId(r.getRefId());
            v.setExtUrl(r.getExtUrl());
            v.setTitle(r.getTitle());
            v.setSortNo(r.getSortNo());
            v.setPublishStatus(r.getPublishStatus());
            v.setPublishTime(r.getPublishTime());
            ResourceFile f = r.getRefId() == null ? null : files.get(r.getRefId());
            if (f != null) {
                v.setDisplayName(f.getFileName());
                v.setFileSize(f.getFileSize());
                v.setFileExt(f.getFileExt());
            } else if ("LINK".equals(r.getResType())) {
                v.setDisplayName(r.getExtUrl());
            }
            out.add(v);
        }
        return out;
    }

    // ============================================================ 资料库
    public List<ResourceFile> listResources(Long teachingClassId, Long teacherId) {
        requireMyClass(teachingClassId, teacherId);
        return resourceMapper.selectList(Wrappers.<ResourceFile>lambdaQuery()
                .eq(ResourceFile::getUploadBy, teacherId)
                .eq(ResourceFile::getIsVoid, 0)
                .and(q -> q.eq(ResourceFile::getTeachingClassId, teachingClassId)
                        .or().isNull(ResourceFile::getTeachingClassId))
                .orderByDesc(ResourceFile::getId));
    }

    @Transactional
    public ResourceFile voidResource(Long id, String reason, Long teacherId) {
        ResourceFile f = resourceMapper.selectById(id);
        if (f == null) {
            throw new BizException("文件不存在");
        }
        if (!f.getUploadBy().equals(teacherId)) {
            throw new BizException("只能作废自己上传的文件");
        }
        f.setIsVoid(1);
        f.setVoidReason(reason);
        resourceMapper.updateById(f);
        auditService.record("RESOURCE_FILE", id, "VOID", null, null, "作废文件：" + f.getFileName());
        return f;
    }
}

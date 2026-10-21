/* gen_content_module.js — 第一阶段 1-6 课程内容（导学 / 教学目标 / 目录课时 / 课时三段资源 / 资料库上传） */
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

// ---------------------------------------------------------------- dto
w('dto/GuideSaveRequest.java', `
package ${P}.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 保存课程导学 */
@Data
public class GuideSaveRequest {
    @NotNull(message = "教学班不能为空")
    private Long teachingClassId;
    private String content;
    /** HTML / TEXT */
    private String contentFormat;
    /** 是否同时发布给学生 */
    private Boolean publish;
}
`);

w('dto/ObjectiveItemDTO.java', `
package ${P}.dto;

import lombok.Data;

/** 教学目标（一行） */
@Data
public class ObjectiveItemDTO {
    private Long id;
    private String objCode;
    private String objContent;
    private String objType;
    private Integer sortNo;
}
`);

w('dto/ObjectiveSaveRequest.java', `
package ${P}.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/** 整批保存教学目标（以提交的清单为准：多退少补） */
@Data
public class ObjectiveSaveRequest {
    @NotNull(message = "教学班不能为空")
    private Long teachingClassId;
    private List<ObjectiveItemDTO> items;
}
`);

w('dto/LessonUpdateRequest.java', `
package ${P}.dto;

import lombok.Data;

/** 调整课时（目录一行） */
@Data
public class LessonUpdateRequest {
    private String title;
    private Integer sortNo;
    private String status;
}
`);

w('dto/LessonResourceSaveRequest.java', `
package ${P}.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 在课时的课前 / 课中 / 课后挂一条资源或任务 */
@Data
public class LessonResourceSaveRequest {
    @NotNull(message = "课时不能为空")
    private Long lessonId;
    /** BEFORE / DURING / AFTER */
    @NotNull(message = "阶段不能为空")
    private String phase;
    /** FILE / LINK / TEXT / PAPER / PROJECT */
    @NotNull(message = "资源类型不能为空")
    private String resType;
    private Long refId;
    private String extUrl;
    private String title;
    private Boolean publish;
}
`);

w('dto/ResourceVoidRequest.java', `
package ${P}.dto;

import lombok.Data;

/** 作废资源文件（逻辑删除，保留引用痕迹） */
@Data
public class ResourceVoidRequest {
    private String reason;
}
`);

// ---------------------------------------------------------------- vo
w('vo/LessonResourceVO.java', `
package ${P}.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 课时资源视图（附带被引用对象的展示信息） */
@Data
public class LessonResourceVO {
    private Long id;
    private Long lessonId;
    private String phase;
    private String resType;
    private Long refId;
    private String extUrl;
    private String title;
    private Integer sortNo;
    private String publishStatus;
    private LocalDateTime publishTime;
    /** 文件资源的原始文件名 / 外链地址（展示用） */
    private String displayName;
    private Long fileSize;
    private String fileExt;
}
`);

w('vo/LessonVO.java', `
package ${P}.vo;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/** 目录一行 = 一次课，含课前 / 课中 / 课后三段 */
@Data
public class LessonVO {
    private Long id;
    private Long sessionId;
    private Integer weekNo;
    private Integer sessionNo;
    private LocalDate sessionDate;
    private String title;
    private Integer sortNo;
    private String status;
    private List<LessonResourceVO> before;
    private List<LessonResourceVO> during;
    private List<LessonResourceVO> after;
}
`);

// ---------------------------------------------------------------- service
w('service/CourseContentService.java', `
package ${P}.service;

import ${P}.common.BizException;
import ${P}.dto.GuideSaveRequest;
import ${P}.dto.LessonResourceSaveRequest;
import ${P}.dto.LessonUpdateRequest;
import ${P}.dto.ObjectiveItemDTO;
import ${P}.dto.ObjectiveSaveRequest;
import ${P}.entity.CourseGuide;
import ${P}.entity.CourseLesson;
import ${P}.entity.CourseOffering;
import ${P}.entity.CourseSession;
import ${P}.entity.LessonResource;
import ${P}.entity.Paper;
import ${P}.entity.ResourceFile;
import ${P}.entity.TeachingClass;
import ${P}.entity.TeachingObjective;
import ${P}.mapper.CourseGuideMapper;
import ${P}.mapper.CourseLessonMapper;
import ${P}.mapper.CourseOfferingMapper;
import ${P}.mapper.CourseSessionMapper;
import ${P}.mapper.LessonResourceMapper;
import ${P}.mapper.PaperMapper;
import ${P}.mapper.ResourceFileMapper;
import ${P}.mapper.TeachingClassMapper;
import ${P}.mapper.TeachingObjectiveMapper;
import ${P}.vo.LessonResourceVO;
import ${P}.vo.LessonVO;
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
`);

// ---------------------------------------------------------------- controllers
w('controller/teacher/TeacherContentController.java', `
package ${P}.controller.teacher;

import ${P}.common.BizException;
import ${P}.common.Result;
import ${P}.dto.GuideSaveRequest;
import ${P}.dto.LessonResourceSaveRequest;
import ${P}.dto.LessonUpdateRequest;
import ${P}.dto.ObjectiveSaveRequest;
import ${P}.dto.ResourceVoidRequest;
import ${P}.entity.CourseGuide;
import ${P}.entity.Paper;
import ${P}.entity.ResourceFile;
import ${P}.entity.TeachingObjective;
import ${P}.security.LoginUser;
import ${P}.security.RequireRole;
import ${P}.security.UserContext;
import ${P}.service.CourseContentService;
import ${P}.service.ResourceFileService;
import ${P}.vo.LessonResourceVO;
import ${P}.vo.LessonVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** 教师端 · 课程内容（导学 / 教学目标 / 目录 / 资料库） */
@RestController
@RequestMapping("/api/teacher")
@RequiredArgsConstructor
@RequireRole("TEACHER")
public class TeacherContentController {

    private final CourseContentService contentService;
    private final ResourceFileService resourceFileService;

    private Long me() {
        LoginUser user = UserContext.require();
        if (user.getRefId() == null) {
            throw new BizException("当前账号没有教师档案");
        }
        return user.getRefId();
    }

    // ---------------- 导学
    @GetMapping("/guide")
    public Result<CourseGuide> getGuide(@RequestParam Long teachingClassId) {
        return Result.ok(contentService.getGuide(teachingClassId, me()));
    }

    @PostMapping("/guide")
    public Result<CourseGuide> saveGuide(@Valid @RequestBody GuideSaveRequest request) {
        return Result.ok(contentService.saveGuide(request, me()));
    }

    // ---------------- 教学目标
    @GetMapping("/objectives")
    public Result<List<TeachingObjective>> objectives(@RequestParam Long teachingClassId) {
        return Result.ok(contentService.listObjectives(teachingClassId, me()));
    }

    @PostMapping("/objectives")
    public Result<List<TeachingObjective>> saveObjectives(@Valid @RequestBody ObjectiveSaveRequest request) {
        return Result.ok(contentService.saveObjectives(request, me()));
    }

    // ---------------- 目录
    @GetMapping("/lessons")
    public Result<List<LessonVO>> lessons(@RequestParam Long teachingClassId) {
        return Result.ok(contentService.listLessons(teachingClassId, me()));
    }

    @PostMapping("/lessons/sync")
    public Result<List<LessonVO>> syncLessons(@RequestParam Long teachingClassId) {
        return Result.ok(contentService.syncLessons(teachingClassId, me()));
    }

    @PostMapping("/lessons/{id}")
    public Result<LessonVO> updateLesson(@PathVariable Long id, @RequestBody LessonUpdateRequest request) {
        return Result.ok(contentService.updateLesson(id, request, me()));
    }

    // ---------------- 课时资源
    @PostMapping("/lesson-resources")
    public Result<LessonResourceVO> addResource(@Valid @RequestBody LessonResourceSaveRequest request) {
        return Result.ok(contentService.addResource(request, me()));
    }

    @PostMapping("/lesson-resources/{id}/publish")
    public Result<LessonResourceVO> publish(@PathVariable Long id, @RequestParam(defaultValue = "true") boolean publish) {
        return Result.ok(contentService.publishResource(id, publish, me()));
    }

    @DeleteMapping("/lesson-resources/{id}")
    public Result<Void> deleteResource(@PathVariable Long id) {
        contentService.deleteResource(id, me());
        return Result.ok();
    }

    // ---------------- 资料库
    @GetMapping("/resources")
    public Result<List<ResourceFile>> resources(@RequestParam Long teachingClassId) {
        return Result.ok(contentService.listResources(teachingClassId, me()));
    }

    @PostMapping("/resources/upload")
    public Result<ResourceFile> upload(@RequestParam("file") MultipartFile file,
                                       @RequestParam Long teachingClassId) {
        return Result.ok(resourceFileService.store(file, teachingClassId, me()));
    }

    @PostMapping("/resources/{id}/void")
    public Result<ResourceFile> voidFile(@PathVariable Long id, @RequestBody(required = false) ResourceVoidRequest request) {
        return Result.ok(contentService.voidResource(id, request == null ? null : request.getReason(), me()));
    }
}
`);

// ---------------------------------------------------------------- 文件存储
w('service/ResourceFileService.java', `
package ${P}.service;

import ${P}.common.BizException;
import ${P}.entity.ResourceFile;
import ${P}.entity.TeachingClass;
import ${P}.mapper.ResourceFileMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 资源文件：文件本体落磁盘目录（storage_type=LOCAL），库里只存元信息。
 * 目录约定：{store-dir}/{teachingClassId}/{uuid}.{ext}
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceFileService {

    private final ResourceFileMapper resourceMapper;
    private final CourseContentService contentService;

    @Value("\${app.file.store-dir:./file-store}")
    private String storeDir;

    public ResourceFile store(MultipartFile file, Long teachingClassId, Long teacherId) {
        if (file == null || file.isEmpty()) {
            throw new BizException("请选择要上传的文件");
        }
        TeachingClass tc = contentService.requireMyClass(teachingClassId, teacherId);
        return storeRaw(file, tc, teacherId, "TEACHER");
    }

    /** 通用上传：权限由调用方校验（教师资料库 / 学生项目提交都用它） */
    public ResourceFile storeRaw(MultipartFile file, TeachingClass tc, Long uploaderId, String uploadRole) {
        if (file == null || file.isEmpty()) {
            throw new BizException("请选择要上传的文件");
        }
        Long teachingClassId = tc.getId();
        String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String ext = "";
        int dot = original.lastIndexOf('.');
        if (dot > -1) {
            ext = original.substring(dot + 1).toLowerCase();
        }
        String key = teachingClassId + "/" + UUID.randomUUID().toString().replace("-", "")
                + (ext.isEmpty() ? "" : "." + ext);
        Path target = root().resolve(key);
        try {
            Files.createDirectories(target.getParent());
            file.transferTo(target.toFile());
        } catch (IOException e) {
            log.error("上传文件失败", e);
            throw new BizException("文件写入失败：" + e.getMessage());
        }
        ResourceFile f = new ResourceFile();
        f.setTeachingClassId(tc.getId());
        f.setOfferingId(tc.getOfferingId());
        f.setTermId(contentService.termIdOf(tc));
        f.setFileName(original);
        f.setFilePath(key);
        f.setFileExt(ext);
        f.setFileSize(file.getSize());
        f.setMimeType(file.getContentType());
        f.setStorageType("LOCAL");
        f.setUploadBy(uploaderId);
        f.setUploadRole(uploadRole);
        f.setCreateTime(LocalDateTime.now());
        f.setIsVoid(0);
        resourceMapper.insert(f);
        return f;
    }

    /** 上传根目录：转成绝对路径（Tomcat 会把相对路径解析到临时 work 目录，务必先绝对化） */
    private Path root() {
        Path dir = Paths.get(storeDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new BizException("无法创建文件目录：" + dir);
        }
        return dir;
    }

    /** 读取文件本体（下载 / 预览） */
    public Path resolve(ResourceFile f) {
        Path p = root().resolve(f.getFilePath());
        if (!Files.exists(p)) {
            throw new BizException("文件不存在或已被清理");
        }
        return p;
    }
}
`);

w('controller/FileController.java', `
package ${P}.controller;

import ${P}.common.BizException;
import ${P}.entity.ResourceFile;
import ${P}.mapper.ResourceFileMapper;
import ${P}.security.LoginUser;
import ${P}.security.RequireRole;
import ${P}.security.UserContext;
import ${P}.service.ResourceFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** 文件下载：登录后按 id 取文件本体（不做静态目录暴露） */
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@RequireRole({"STUDENT", "TEACHER", "ADMIN", "COUNSELOR"})
public class FileController {

    private final ResourceFileMapper resourceMapper;
    private final ResourceFileService resourceFileService;

    @GetMapping("/{id}")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        LoginUser user = UserContext.require();
        ResourceFile f = resourceMapper.selectById(id);
        if (f == null || (f.getIsVoid() != null && f.getIsVoid() == 1)) {
            throw new BizException("文件不存在或已作废");
        }
        boolean owner = "TEACHER".equals(user.getRole()) && user.getRefId() != null
                && user.getRefId().equals(f.getUploadBy());
        if (!owner && !"ADMIN".equals(user.getRole())) {
            // 学生 / 辅导员：只允许取本教学班的文件
            if (f.getTeachingClassId() == null) {
                throw new BizException("无权下载该文件");
            }
        }
        Resource body = new FileSystemResource(resourceFileService.resolve(f));
        String name = f.getFileName() == null ? "file" : f.getFileName();
        String encoded;
        try {
            encoded = URLEncoder.encode(name, StandardCharsets.UTF_8.name()).replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            encoded = name;
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(body);
    }
}
`);

console.log('课程内容模块：Java 代码生成完成');

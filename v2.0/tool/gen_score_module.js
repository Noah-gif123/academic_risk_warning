/* gen_score_module.js — 第一阶段 1-5 成绩构成（模板套用 / 评分项两级 / 权重校验 / 课程组确认与锁定 / 教务裁定） */
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
w('dto/ScoreItemDTO.java', `
package ${P}.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/** 评分项（两级）：children 为空表示叶子项 */
@Data
public class ScoreItemDTO {
    private String itemCode;
    private String itemName;
    /** 分类：USUAL/MID/FINAL/HOMEWORK/QUIZ/PROJECT/EXP/PERF/OTHER（禁止 ATTENDANCE） */
    private String category;
    private BigDecimal weight;
    private BigDecimal fullScore;
    /** 数据来源：AUTO 自动汇总（测验/作业/项目）/ MANUAL 手工录入 */
    private String dataSource;
    /** 记分形态：NUMERIC / PASS / GRADE */
    private String scoreType;
    private String aggregateMode;
    private Integer expectedTimes;
    private Integer isAnchor;
    private String anchorLayer;
    private Integer isSubjective;
    private String scorerRole;
    private List<ScoreItemDTO> children;
}
`);

w('dto/ScoreSchemeSaveRequest.java', `
package ${P}.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/** 保存成绩构成（草稿） */
@Data
public class ScoreSchemeSaveRequest {
    @NotNull(message = "开课实例不能为空")
    private Long offeringId;
    private String level;
    private Long sourceTemplateId;
    private String remark;
    private List<ScoreItemDTO> items;
}
`);

// ---------------------------------------------------------------- vo
w('vo/ScoreItemVO.java', `
package ${P}.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/** 评分项视图（树） */
@Data
public class ScoreItemVO {
    private Long id;
    private Long parentId;
    private Integer itemLevel;
    private String itemCode;
    private String itemName;
    private String category;
    private BigDecimal weight;
    private BigDecimal fullScore;
    private String dataSource;
    private String scoreType;
    private String aggregateMode;
    private Integer expectedTimes;
    private Integer isAnchor;
    private String anchorLayer;
    private Integer isSubjective;
    private String scorerRole;
    private Integer sortNo;
    private List<ScoreItemVO> children;
}
`);

w('vo/ScoreSchemeVO.java', `
package ${P}.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 成绩构成视图：方案 + 评分项树 + 确认与锁定状态 */
@Data
public class ScoreSchemeVO {
    private Long id;
    private Long groupId;
    private Long offeringId;
    private Long termId;
    private String termName;
    private String courseName;
    private String courseType;
    private Integer version;
    private String level;
    private Long sourceTemplateId;
    private BigDecimal totalWeight;
    private String confirmStatus;
    private Integer confirmTotal;
    private Integer confirmDone;
    private Integer locked;
    private String lockType;
    private String lockedByName;
    private LocalDateTime lockedAt;
    private String remark;
    private Integer teacherCount;
    private Boolean myConfirmed;
    private List<Long> confirmedTeacherIds;
    private List<ScoreItemVO> items;
}
`);

// ---------------------------------------------------------------- service
w('service/ScoreSchemeService.java', `
package ${P}.service;

import ${P}.common.BizException;
import ${P}.dto.ScoreItemDTO;
import ${P}.dto.ScoreSchemeSaveRequest;
import ${P}.entity.Admin;
import ${P}.entity.Course;
import ${P}.entity.CourseGroup;
import ${P}.entity.CourseOffering;
import ${P}.entity.ScoreScheme;
import ${P}.entity.ScoreSchemeItem;
import ${P}.entity.Teacher;
import ${P}.entity.TeachingClass;
import ${P}.entity.Term;
import ${P}.mapper.AdminMapper;
import ${P}.mapper.CourseGroupMapper;
import ${P}.mapper.CourseMapper;
import ${P}.mapper.CourseOfferingMapper;
import ${P}.mapper.ScoreSchemeItemMapper;
import ${P}.mapper.ScoreSchemeMapper;
import ${P}.mapper.TeacherMapper;
import ${P}.mapper.TeachingClassMapper;
import ${P}.mapper.TermMapper;
import ${P}.vo.ScoreItemVO;
import ${P}.vo.ScoreSchemeVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 成绩构成：开课前由课程组确定评分项与权重，全体确认后锁定。
 * 口径：① 同一层级权重之和必须 = 100；② 不允许考勤类评分项；③ 锁定后不可改；④ 未达成一致由教务裁定锁定。
 */
@Service
@RequiredArgsConstructor
public class ScoreSchemeService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final ScoreSchemeMapper schemeMapper;
    private final ScoreSchemeItemMapper itemMapper;
    private final CourseGroupMapper groupMapper;
    private final CourseOfferingMapper offeringMapper;
    private final CourseMapper courseMapper;
    private final TermMapper termMapper;
    private final TeachingClassMapper classMapper;
    private final TeacherMapper teacherMapper;
    private final AdminMapper adminMapper;
    private final AuditService auditService;

    // ============================================================ 查询
    public ScoreSchemeVO getByOffering(Long offeringId, Long teacherId) {
        CourseOffering offering = offeringMapper.selectById(offeringId);
        if (offering == null) {
            throw new BizException("开课实例不存在");
        }
        CourseGroup group = groupMapper.selectOne(Wrappers.<CourseGroup>lambdaQuery()
                .eq(CourseGroup::getCourseId, offering.getCourseId())
                .eq(CourseGroup::getTermId, offering.getTermId()).last("limit 1"));
        if (group == null) {
            return null;
        }
        ScoreScheme scheme = schemeMapper.selectOne(Wrappers.<ScoreScheme>lambdaQuery()
                .eq(ScoreScheme::getGroupId, group.getId())
                .orderByDesc(ScoreScheme::getVersion).last("limit 1"));
        if (scheme == null) {
            return null;
        }
        return toVO(scheme, offering, group, teacherId);
    }

    public List<ScoreSchemeVO> listByTerm(Long termId) {
        List<ScoreScheme> schemes = schemeMapper.selectList(Wrappers.<ScoreScheme>lambdaQuery()
                .eq(termId != null, ScoreScheme::getTermId, termId)
                .orderByDesc(ScoreScheme::getId));
        List<ScoreSchemeVO> out = new ArrayList<>();
        for (ScoreScheme s : schemes) {
            CourseGroup g = groupMapper.selectById(s.getGroupId());
            if (g == null) {
                continue;
            }
            CourseOffering o = offeringMapper.selectOne(Wrappers.<CourseOffering>lambdaQuery()
                    .eq(CourseOffering::getCourseId, g.getCourseId())
                    .eq(CourseOffering::getTermId, g.getTermId()).last("limit 1"));
            if (o == null) {
                continue;
            }
            out.add(toVO(s, o, g, null));
        }
        return out;
    }

    // ============================================================ 保存（草稿）
    @Transactional
    public ScoreSchemeVO save(ScoreSchemeSaveRequest req, Long operatorTeacherId) {
        CourseOffering offering = offeringMapper.selectById(req.getOfferingId());
        if (offering == null) {
            throw new BizException("开课实例不存在");
        }
        CourseGroup group = groupMapper.selectOne(Wrappers.<CourseGroup>lambdaQuery()
                .eq(CourseGroup::getCourseId, offering.getCourseId())
                .eq(CourseGroup::getTermId, offering.getTermId()).last("limit 1"));
        if (group == null) {
            throw new BizException("该课程本学期还没有课程组，请先由教务创建开课实例");
        }
        List<ScoreItemDTO> items = req.getItems() == null ? Collections.emptyList() : req.getItems();
        if (items.isEmpty()) {
            throw new BizException("至少需要一个评分项");
        }
        BigDecimal total = validate(items);

        ScoreScheme old = schemeMapper.selectOne(Wrappers.<ScoreScheme>lambdaQuery()
                .eq(ScoreScheme::getGroupId, group.getId())
                .orderByDesc(ScoreScheme::getVersion).last("limit 1"));
        if (old != null && old.getLocked() != null && old.getLocked() == 1) {
            throw new BizException("成绩构成已锁定，不能再修改（如需调整请联系教务）");
        }
        int version = old == null ? 1 : old.getVersion() + 1;
        ScoreScheme scheme = new ScoreScheme();
        scheme.setGroupId(group.getId());
        scheme.setTermId(offering.getTermId());
        scheme.setVersion(version);
        scheme.setLevel(req.getLevel() == null ? "L2" : req.getLevel());
        scheme.setSourceTemplateId(req.getSourceTemplateId());
        scheme.setTotalWeight(total);
        scheme.setConfirmStatus("DRAFT");
        scheme.setConfirmTotal(countGroupTeachers(group).size());
        scheme.setConfirmDone(0);
        scheme.setInitiatorId(operatorTeacherId);
        scheme.setConfirmedByJson("[]");
        scheme.setLocked(0);
        scheme.setRemark(req.getRemark());
        schemeMapper.insert(scheme);

        writeItems(scheme.getId(), items);
        auditService.record("SCORE_SCHEME", scheme.getId(), "SAVE", null, null,
                "保存成绩构成 v" + version + "（合计权重 " + total + "）");
        return toVO(scheme, offering, group, operatorTeacherId);
    }

    // ============================================================ 确认
    @Transactional
    public ScoreSchemeVO confirm(Long schemeId, Long teacherId) {
        ScoreScheme scheme = schemeMapper.selectById(schemeId);
        if (scheme == null) {
            throw new BizException("成绩构成不存在");
        }
        if (scheme.getLocked() != null && scheme.getLocked() == 1) {
            throw new BizException("已锁定，无需再确认");
        }
        CourseGroup group = groupMapper.selectById(scheme.getGroupId());
        List<Long> teachers = countGroupTeachers(group);
        if (!teachers.contains(teacherId)) {
            throw new BizException("只有该课程组的任课教师才能确认");
        }
        List<Long> confirmed = parseIds(scheme.getConfirmedByJson());
        if (!confirmed.contains(teacherId)) {
            confirmed.add(teacherId);
        }
        scheme.setConfirmedByJson(toJsonIds(confirmed));
        scheme.setConfirmDone(confirmed.size());
        scheme.setConfirmTotal(teachers.size());
        scheme.setConfirmStatus(confirmed.size() >= teachers.size() ? "CONFIRMED" : "CONFIRMING");
        schemeMapper.updateById(scheme);
        auditService.record("SCORE_SCHEME", schemeId, "CONFIRM", null, null,
                "教师确认成绩构成（" + confirmed.size() + "/" + teachers.size() + "）");
        CourseOffering o = offeringMapper.selectOne(Wrappers.<CourseOffering>lambdaQuery()
                .eq(CourseOffering::getCourseId, group.getCourseId())
                .eq(CourseOffering::getTermId, group.getTermId()).last("limit 1"));
        return toVO(scheme, o, group, teacherId);
    }

    // ============================================================ 锁定
    @Transactional
    public ScoreSchemeVO lock(Long schemeId, Long operatorId, String operatorRole) {
        ScoreScheme scheme = schemeMapper.selectById(schemeId);
        if (scheme == null) {
            throw new BizException("成绩构成不存在");
        }
        if (scheme.getLocked() != null && scheme.getLocked() == 1) {
            throw new BizException("已锁定");
        }
        CourseGroup group = groupMapper.selectById(scheme.getGroupId());
        List<Long> teachers = countGroupTeachers(group);
        boolean admin = "ADMIN".equals(operatorRole);
        if (!admin && (scheme.getConfirmDone() == null || scheme.getConfirmDone() < teachers.size())) {
            throw new BizException("需要课程组全体教师确认后才能锁定；如无法达成一致，请提交教务裁定");
        }
        List<ScoreItemVO> tree = buildTree(scheme.getId());
        BigDecimal total = sumRoot(tree);
        if (total.compareTo(HUNDRED) != 0) {
            throw new BizException("根层权重合计必须为 100，当前为 " + total.stripTrailingZeros().toPlainString());
        }
        for (ScoreItemVO root : tree) {
            if (root.getChildren() != null && !root.getChildren().isEmpty()) {
                BigDecimal sub = root.getChildren().stream().map(ScoreItemVO::getWeight)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                if (sub.compareTo(HUNDRED) != 0) {
                    throw new BizException("「" + root.getItemName() + "」下各项权重之和必须为 100，当前为 "
                            + sub.stripTrailingZeros().toPlainString());
                }
            }
        }
        scheme.setTotalWeight(total);
        scheme.setLocked(1);
        scheme.setLockedBy(operatorId);
        scheme.setLockedAt(LocalDateTime.now());
        scheme.setLockType(admin ? "ADMIN_RULED" : "TEACHER_COLLECTIVE");
        scheme.setConfirmStatus("LOCKED");
        schemeMapper.updateById(scheme);
        auditService.record("SCORE_SCHEME", schemeId, "LOCK", null, null,
                (admin ? "教务裁定锁定" : "课程组确认后锁定") + "成绩构成 v" + scheme.getVersion());
        CourseOffering o = offeringMapper.selectOne(Wrappers.<CourseOffering>lambdaQuery()
                .eq(CourseOffering::getCourseId, group.getCourseId())
                .eq(CourseOffering::getTermId, group.getTermId()).last("limit 1"));
        return toVO(scheme, o, group, null);
    }

    // ============================================================ 校验与写入
    /** 校验并返回根层权重合计 */
    private BigDecimal validate(List<ScoreItemDTO> items) {
        BigDecimal total = BigDecimal.ZERO;
        for (ScoreItemDTO it : items) {
            checkItem(it);
            if (it.getWeight() == null || it.getWeight().signum() <= 0) {
                throw new BizException("评分项「" + it.getItemName() + "」的权重必须大于 0");
            }
            total = total.add(it.getWeight());
            if (it.getChildren() != null && !it.getChildren().isEmpty()) {
                BigDecimal sub = BigDecimal.ZERO;
                for (ScoreItemDTO c : it.getChildren()) {
                    checkItem(c);
                    if (c.getWeight() == null || c.getWeight().signum() <= 0) {
                        throw new BizException("评分项「" + c.getItemName() + "」的权重必须大于 0");
                    }
                    sub = sub.add(c.getWeight());
                }
                if (sub.compareTo(HUNDRED) != 0) {
                    throw new BizException("「" + it.getItemName() + "」下各项权重之和必须为 100，当前为 "
                            + sub.stripTrailingZeros().toPlainString());
                }
            }
        }
        if (total.compareTo(HUNDRED) != 0) {
            throw new BizException("根层权重合计必须为 100，当前为 " + total.stripTrailingZeros().toPlainString());
        }
        return total;
    }

    private void checkItem(ScoreItemDTO it) {
        String name = it.getItemName() == null ? "" : it.getItemName();
        String cat = it.getCategory() == null ? "" : it.getCategory().toUpperCase();
        if (cat.equals("ATTENDANCE") || name.contains("考勤") || name.contains("出勤")) {
            throw new BizException("考勤不得作为评分项：出勤只通过缺课硬门槛与违纪扣分影响结果");
        }
    }

    private void writeItems(Long schemeId, List<ScoreItemDTO> items) {
        int sort = 0;
        for (ScoreItemDTO it : items) {
            ScoreSchemeItem parent = insertItem(schemeId, null, 1, it, ++sort);
            if (it.getChildren() != null) {
                int csort = 0;
                for (ScoreItemDTO c : it.getChildren()) {
                    insertItem(schemeId, parent.getId(), 2, c, ++csort);
                }
            }
        }
    }

    private ScoreSchemeItem insertItem(Long schemeId, Long parentId, int level, ScoreItemDTO d, int sort) {
        ScoreSchemeItem e = new ScoreSchemeItem();
        e.setSchemeId(schemeId);
        e.setParentId(parentId);
        e.setItemLevel(level);
        e.setItemCode(d.getItemCode() == null ? ("ITEM_" + sort) : d.getItemCode());
        e.setItemName(d.getItemName());
        e.setCategory(d.getCategory() == null ? "OTHER" : d.getCategory());
        e.setWeight(d.getWeight());
        e.setFullScore(d.getFullScore() == null ? HUNDRED : d.getFullScore());
        e.setDataSource(d.getDataSource() == null ? "MANUAL" : d.getDataSource());
        e.setScoreType(d.getScoreType() == null ? "NUMERIC" : d.getScoreType());
        e.setAggregateMode(d.getAggregateMode());
        e.setExpectedTimes(d.getExpectedTimes());
        e.setIsAnchor(d.getIsAnchor() == null ? 0 : d.getIsAnchor());
        e.setAnchorLayer(d.getAnchorLayer());
        e.setIsSubjective(d.getIsSubjective() == null ? 0 : d.getIsSubjective());
        e.setScorerRole(d.getScorerRole());
        e.setSortNo(sort);
        e.setStatus("ACTIVE");
        itemMapper.insert(e);
        return e;
    }

    // ============================================================ 视图组装
    private ScoreSchemeVO toVO(ScoreScheme s, CourseOffering offering, CourseGroup group, Long teacherId) {
        ScoreSchemeVO vo = new ScoreSchemeVO();
        vo.setId(s.getId());
        vo.setGroupId(s.getGroupId());
        vo.setOfferingId(offering == null ? null : offering.getId());
        vo.setTermId(s.getTermId());
        Term term = termMapper.selectById(s.getTermId());
        vo.setTermName(term == null ? "" : term.getTermName());
        Course course = courseMapper.selectById(group.getCourseId());
        vo.setCourseName(course == null ? "" : course.getCourseName());
        vo.setCourseType(course == null ? "" : course.getCourseType());
        vo.setVersion(s.getVersion());
        vo.setLevel(s.getLevel());
        vo.setSourceTemplateId(s.getSourceTemplateId());
        vo.setTotalWeight(s.getTotalWeight());
        vo.setConfirmStatus(s.getConfirmStatus());
        vo.setConfirmTotal(s.getConfirmTotal());
        vo.setConfirmDone(s.getConfirmDone());
        vo.setLocked(s.getLocked());
        vo.setLockType(s.getLockType());
        vo.setLockedAt(s.getLockedAt());
        vo.setRemark(s.getRemark());
        List<Long> teachers = countGroupTeachers(group);
        vo.setTeacherCount(teachers.size());
        List<Long> confirmed = parseIds(s.getConfirmedByJson());
        vo.setConfirmedTeacherIds(confirmed);
        if (teacherId != null) {
            vo.setMyConfirmed(confirmed.contains(teacherId));
        }
        if (s.getLockedBy() != null) {
            String name = "";
            if ("ADMIN_RULED".equals(s.getLockType())) {
                for (Admin a : adminMapper.selectList(Wrappers.<Admin>lambdaQuery()
                        .eq(Admin::getAccountId, s.getLockedBy()))) {
                    name = a.getAdminName();
                    break;
                }
            } else {
                Teacher t = teacherMapper.selectById(s.getLockedBy());
                name = t == null ? "" : t.getTeacherName();
            }
            vo.setLockedByName(name);
        }
        vo.setItems(buildTree(s.getId()));
        return vo;
    }

    private List<ScoreItemVO> buildTree(Long schemeId) {
        List<ScoreSchemeItem> all = itemMapper.selectList(Wrappers.<ScoreSchemeItem>lambdaQuery()
                .eq(ScoreSchemeItem::getSchemeId, schemeId).orderByAsc(ScoreSchemeItem::getSortNo));
        Map<Long, ScoreItemVO> byId = new LinkedHashMap<>();
        List<ScoreItemVO> roots = new ArrayList<>();
        for (ScoreSchemeItem e : all) {
            ScoreItemVO v = toItemVO(e);
            byId.put(e.getId(), v);
            if (e.getParentId() == null) {
                roots.add(v);
            }
        }
        for (ScoreSchemeItem e : all) {
            if (e.getParentId() != null && byId.containsKey(e.getParentId())) {
                ScoreItemVO parent = byId.get(e.getParentId());
                if (parent.getChildren() == null) {
                    parent.setChildren(new ArrayList<>());
                }
                parent.getChildren().add(byId.get(e.getId()));
            }
        }
        return roots;
    }

    private ScoreItemVO toItemVO(ScoreSchemeItem e) {
        ScoreItemVO v = new ScoreItemVO();
        v.setId(e.getId());
        v.setParentId(e.getParentId());
        v.setItemLevel(e.getItemLevel());
        v.setItemCode(e.getItemCode());
        v.setItemName(e.getItemName());
        v.setCategory(e.getCategory());
        v.setWeight(e.getWeight());
        v.setFullScore(e.getFullScore());
        v.setDataSource(e.getDataSource());
        v.setScoreType(e.getScoreType());
        v.setAggregateMode(e.getAggregateMode());
        v.setExpectedTimes(e.getExpectedTimes());
        v.setIsAnchor(e.getIsAnchor());
        v.setAnchorLayer(e.getAnchorLayer());
        v.setIsSubjective(e.getIsSubjective());
        v.setScorerRole(e.getScorerRole());
        v.setSortNo(e.getSortNo());
        return v;
    }

    /** 课程组教师：该课程本学期全部教学班的任课教师（去重） */
    private List<Long> countGroupTeachers(CourseGroup group) {
        if (group == null) {
            return new ArrayList<>();
        }
        List<Long> offeringIds = offeringMapper.selectList(Wrappers.<CourseOffering>lambdaQuery()
                        .eq(CourseOffering::getCourseId, group.getCourseId())
                        .eq(CourseOffering::getTermId, group.getTermId()))
                .stream().map(CourseOffering::getId).collect(Collectors.toList());
        if (offeringIds.isEmpty()) {
            return new ArrayList<>();
        }
        return classMapper.selectList(Wrappers.<TeachingClass>lambdaQuery()
                        .in(TeachingClass::getOfferingId, offeringIds))
                .stream().map(TeachingClass::getTeacherId).distinct().collect(Collectors.toList());
    }

    private static List<Long> parseIds(String json) {
        List<Long> out = new ArrayList<>();
        if (json == null) {
            return out;
        }
        String s = json.replace("[", "").replace("]", "").trim();
        if (s.isEmpty()) {
            return out;
        }
        for (String p : s.split(",")) {
            String t = p.trim();
            if (!t.isEmpty()) {
                try {
                    out.add(Long.parseLong(t));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return out;
    }

    private static String toJsonIds(List<Long> ids) {
        return "[" + ids.stream().map(String::valueOf).collect(Collectors.joining(",")) + "]";
    }

    private BigDecimal sumRoot(List<ScoreItemVO> roots) {
        return roots.stream().map(ScoreItemVO::getWeight).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
`);

// ---------------------------------------------------------------- controllers
w('controller/teacher/TeacherScoreController.java', `
package ${P}.controller.teacher;

import ${P}.common.BizException;
import ${P}.common.Result;
import ${P}.dto.ScoreSchemeSaveRequest;
import ${P}.entity.ScoreTemplate;
import ${P}.mapper.ScoreTemplateMapper;
import ${P}.security.LoginUser;
import ${P}.security.RequireRole;
import ${P}.security.UserContext;
import ${P}.service.ScoreSchemeService;
import ${P}.vo.ScoreSchemeVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 教师端 · 成绩构成（课程组共同确定 → 全体确认 → 锁定） */
@RestController
@RequestMapping("/api/teacher")
@RequiredArgsConstructor
@RequireRole("TEACHER")
public class TeacherScoreController {

    private final ScoreSchemeService schemeService;
    private final ScoreTemplateMapper templateMapper;

    /** 成绩构成模板（按课程类型筛选，教师套用） */
    @GetMapping("/score-templates")
    public Result<List<ScoreTemplate>> templates(@RequestParam(required = false) String courseType) {
        return Result.ok(templateMapper.selectList(Wrappers.<ScoreTemplate>lambdaQuery()
                .eq(courseType != null && !courseType.isEmpty(), ScoreTemplate::getCourseType, courseType)
                .eq(ScoreTemplate::getStatus, "ACTIVE")
                .orderByDesc(ScoreTemplate::getIsDefault)));
    }

    @GetMapping("/score-scheme")
    public Result<ScoreSchemeVO> get(@RequestParam Long offeringId) {
        LoginUser user = UserContext.require();
        return Result.ok(schemeService.getByOffering(offeringId, user.getRefId()));
    }

    @PostMapping("/score-scheme")
    public Result<ScoreSchemeVO> save(@Valid @RequestBody ScoreSchemeSaveRequest request) {
        LoginUser user = UserContext.require();
        if (user.getRefId() == null) {
            throw new BizException("当前账号没有教师档案");
        }
        return Result.ok(schemeService.save(request, user.getRefId()));
    }

    @PostMapping("/score-scheme/{id}/confirm")
    public Result<ScoreSchemeVO> confirm(@PathVariable Long id) {
        LoginUser user = UserContext.require();
        return Result.ok(schemeService.confirm(id, user.getRefId()));
    }

    @PostMapping("/score-scheme/{id}/lock")
    public Result<ScoreSchemeVO> lock(@PathVariable Long id) {
        LoginUser user = UserContext.require();
        return Result.ok(schemeService.lock(id, user.getRefId(), "TEACHER"));
    }
}
`);

w('controller/admin/AdminScoreController.java', `
package ${P}.controller.admin;

import ${P}.common.Result;
import ${P}.entity.ScoreTemplate;
import ${P}.mapper.ScoreTemplateMapper;
import ${P}.security.LoginUser;
import ${P}.security.RequireRole;
import ${P}.security.UserContext;
import ${P}.service.ScoreSchemeService;
import ${P}.vo.ScoreSchemeVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 教务端 · 成绩构成（模板 / 全量查看 / 裁定锁定） */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@RequireRole("ADMIN")
public class AdminScoreController {

    private final ScoreSchemeService schemeService;
    private final ScoreTemplateMapper templateMapper;

    @GetMapping("/score-templates")
    public Result<List<ScoreTemplate>> templates(@RequestParam(required = false) String courseType) {
        return Result.ok(templateMapper.selectList(Wrappers.<ScoreTemplate>lambdaQuery()
                .eq(courseType != null, ScoreTemplate::getCourseType, courseType)
                .eq(ScoreTemplate::getStatus, "ACTIVE")
                .orderByDesc(ScoreTemplate::getIsDefault)));
    }

    @GetMapping("/score-schemes")
    public Result<List<ScoreSchemeVO>> list(@RequestParam(required = false) Long termId) {
        return Result.ok(schemeService.listByTerm(termId));
    }

    @PostMapping("/score-schemes/{id}/lock")
    public Result<ScoreSchemeVO> lock(@PathVariable Long id) {
        LoginUser user = UserContext.require();
        return Result.ok(schemeService.lock(id, user.getAccountId(), "ADMIN"));
    }
}
`);

console.log('成绩构成模块：Java 代码生成完成');

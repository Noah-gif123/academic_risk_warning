package com.example.academic_risk_warning.service;

import com.example.academic_risk_warning.common.BizException;
import com.example.academic_risk_warning.dto.ScoreItemDTO;
import com.example.academic_risk_warning.dto.ScoreSchemeSaveRequest;
import com.example.academic_risk_warning.entity.Admin;
import com.example.academic_risk_warning.entity.Course;
import com.example.academic_risk_warning.entity.CourseGroup;
import com.example.academic_risk_warning.entity.CourseOffering;
import com.example.academic_risk_warning.entity.ScoreScheme;
import com.example.academic_risk_warning.entity.ScoreSchemeItem;
import com.example.academic_risk_warning.entity.Teacher;
import com.example.academic_risk_warning.entity.TeachingClass;
import com.example.academic_risk_warning.entity.Term;
import com.example.academic_risk_warning.mapper.AdminMapper;
import com.example.academic_risk_warning.mapper.CourseGroupMapper;
import com.example.academic_risk_warning.mapper.CourseMapper;
import com.example.academic_risk_warning.mapper.CourseOfferingMapper;
import com.example.academic_risk_warning.mapper.ScoreSchemeItemMapper;
import com.example.academic_risk_warning.mapper.ScoreSchemeMapper;
import com.example.academic_risk_warning.mapper.TeacherMapper;
import com.example.academic_risk_warning.mapper.TeachingClassMapper;
import com.example.academic_risk_warning.mapper.TermMapper;
import com.example.academic_risk_warning.vo.ScoreItemVO;
import com.example.academic_risk_warning.vo.ScoreSchemeVO;
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

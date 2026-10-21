package com.example.academic_risk_warning.service;

import com.example.academic_risk_warning.common.BizException;
import com.example.academic_risk_warning.dto.OfferingSaveRequest;
import com.example.academic_risk_warning.entity.Course;
import com.example.academic_risk_warning.entity.CourseGroup;
import com.example.academic_risk_warning.entity.CourseOffering;
import com.example.academic_risk_warning.entity.CourseSession;
import com.example.academic_risk_warning.entity.Enrollment;
import com.example.academic_risk_warning.entity.TeachingClass;
import com.example.academic_risk_warning.entity.Term;
import com.example.academic_risk_warning.entity.TermCalendar;
import com.example.academic_risk_warning.mapper.CourseGroupMapper;
import com.example.academic_risk_warning.mapper.CourseMapper;
import com.example.academic_risk_warning.mapper.CourseOfferingMapper;
import com.example.academic_risk_warning.mapper.CourseSessionMapper;
import com.example.academic_risk_warning.mapper.EnrollmentMapper;
import com.example.academic_risk_warning.mapper.TeachingClassMapper;
import com.example.academic_risk_warning.mapper.TermCalendarMapper;
import com.example.academic_risk_warning.mapper.TermMapper;
import com.example.academic_risk_warning.vo.OfferingVO;
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

/** 开课实例：排课 + 课次生成 + 实际课次确认 */
@Service
@RequiredArgsConstructor
public class OfferingService {

    private final CourseOfferingMapper offeringMapper;
    private final CourseMapper courseMapper;
    private final CourseGroupMapper groupMapper;
    private final TermMapper termMapper;
    private final TermCalendarMapper calendarMapper;
    private final TeachingClassMapper classMapper;
    private final EnrollmentMapper enrollmentMapper;
    private final CourseSessionMapper sessionMapper;
    private final AuditService auditService;

    public List<OfferingVO> list(Long termId) {
        List<CourseOffering> list = offeringMapper.selectList(Wrappers.<CourseOffering>lambdaQuery()
                .eq(termId != null, CourseOffering::getTermId, termId)
                .orderByDesc(CourseOffering::getId));
        if (list.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, Course> courses = courseMapper.selectBatchIds(
                        list.stream().map(CourseOffering::getCourseId).distinct().collect(Collectors.toList()))
                .stream().collect(Collectors.toMap(Course::getId, c -> c, (a, b) -> a));
        Map<Long, Term> terms = termMapper.selectBatchIds(
                        list.stream().map(CourseOffering::getTermId).distinct().collect(Collectors.toList()))
                .stream().collect(Collectors.toMap(Term::getId, c -> c, (a, b) -> a));
        List<Long> offeringIds = list.stream().map(CourseOffering::getId).collect(Collectors.toList());
        Map<Long, Long> classCounts = new LinkedHashMap<>();
        Map<Long, Long> enrollCounts = new LinkedHashMap<>();
        Map<Long, Long> sessionCounts = new LinkedHashMap<>();
        classMapper.selectList(Wrappers.<TeachingClass>lambdaQuery().in(TeachingClass::getOfferingId, offeringIds))
                .forEach(c -> classCounts.merge(c.getOfferingId(), 1L, Long::sum));
        enrollmentMapper.selectList(Wrappers.<Enrollment>lambdaQuery().in(Enrollment::getOfferingId, offeringIds))
                .forEach(e -> enrollCounts.merge(e.getOfferingId(), 1L, Long::sum));
        sessionMapper.selectList(Wrappers.<CourseSession>lambdaQuery().in(CourseSession::getOfferingId, offeringIds))
                .forEach(s -> sessionCounts.merge(s.getOfferingId(), 1L, Long::sum));

        List<OfferingVO> out = new ArrayList<>();
        for (CourseOffering o : list) {
            OfferingVO vo = new OfferingVO();
            vo.setId(o.getId());
            vo.setCourseId(o.getCourseId());
            Course c = courses.get(o.getCourseId());
            if (c != null) {
                vo.setCourseName(c.getCourseName());
                vo.setCourseType(c.getCourseType());
            }
            vo.setTermId(o.getTermId());
            Term t = terms.get(o.getTermId());
            if (t != null) {
                vo.setTermName(t.getTermName());
            }
            vo.setPlannedWeeks(o.getPlannedWeeks());
            vo.setPlannedTimesPerWeek(o.getPlannedTimesPerWeek());
            vo.setPlannedSessions(o.getPlannedSessions());
            vo.setActualSessions(o.getActualSessions());
            vo.setEffectiveSessions(o.getEffectiveSessions());
            vo.setEffectiveSource(o.getEffectiveSource());
            vo.setPassScore(o.getPassScore());
            vo.setStatus(o.getStatus());
            vo.setClassCount(classCounts.getOrDefault(o.getId(), 0L).intValue());
            vo.setEnrollCount(enrollCounts.getOrDefault(o.getId(), 0L).intValue());
            vo.setSessionCount(sessionCounts.getOrDefault(o.getId(), 0L).intValue());
            out.add(vo);
        }
        return out;
    }

    @Transactional
    public CourseOffering save(OfferingSaveRequest req) {
        if (courseMapper.selectById(req.getCourseId()) == null) {
            throw new BizException("课程不存在");
        }
        if (termMapper.selectById(req.getTermId()) == null) {
            throw new BizException("学期不存在");
        }
        CourseOffering o;
        boolean create = req.getId() == null;
        if (create) {
            long dup = offeringMapper.selectCount(Wrappers.<CourseOffering>lambdaQuery()
                    .eq(CourseOffering::getCourseId, req.getCourseId())
                    .eq(CourseOffering::getTermId, req.getTermId()));
            if (dup > 0) {
                throw new BizException("该课程在该学期已存在开课实例");
            }
            o = new CourseOffering();
        } else {
            o = offeringMapper.selectById(req.getId());
            if (o == null) {
                throw new BizException("开课实例不存在");
            }
        }
        o.setCourseId(req.getCourseId());
        o.setTermId(req.getTermId());
        o.setPlannedWeeks(req.getPlannedWeeks());
        o.setPlannedTimesPerWeek(req.getPlannedTimesPerWeek());
        o.setPlannedSessions(req.getPlannedWeeks() * req.getPlannedTimesPerWeek());
        o.setPassScore(req.getPassScore());
        o.setRemark(req.getRemark());
        if (create) {
            CourseGroup g = groupMapper.selectOne(Wrappers.<CourseGroup>lambdaQuery()
                    .eq(CourseGroup::getCourseId, req.getCourseId())
                    .eq(CourseGroup::getTermId, req.getTermId()).last("limit 1"));
            if (g == null) {
                g = new CourseGroup();
                g.setCourseId(req.getCourseId());
                g.setTermId(req.getTermId());
                g.setLevel("L2");
                g.setLevelRequestStatus("NONE");
                g.setStatus("ACTIVE");
                groupMapper.insert(g);
            }
            o.setGroupId(g.getId());
            o.setStatus("ACTIVE");
            offeringMapper.insert(o);
        } else {
            offeringMapper.updateById(o);
        }
        auditService.record("COURSE_OFFERING", o.getId(), create ? "CREATE" : "UPDATE", null, null,
                (create ? "新增开课 " : "修改开课 ") + "计划课次 " + o.getPlannedSessions());
        return o;
    }

    /** 教务确认实际课次：effective_sessions 用于缺课门槛与覆盖率的分母 */
    @Transactional
    public CourseOffering confirmActual(Long offeringId) {
        CourseOffering o = offeringMapper.selectById(offeringId);
        if (o == null) {
            throw new BizException("开课实例不存在");
        }
        // 实际课次优先取“已生成的课次数”（已发生/已排定的课）；没有再退回计划课次
        long generated = sessionMapper.selectCount(Wrappers.<CourseSession>lambdaQuery()
                .eq(CourseSession::getOfferingId, offeringId)
                .eq(CourseSession::getIsVoid, 0));
        int actual;
        String source;
        if (o.getActualSessions() != null) {
            actual = o.getActualSessions();
            source = "ACTUAL";
        } else if (generated > 0) {
            actual = (int) generated;
            source = "SESSIONS";
        } else {
            actual = o.getPlannedSessions() == null ? 0 : o.getPlannedSessions();
            source = "PLANNED";
        }
        o.setActualSessions(actual);
        o.setEffectiveSessions(actual);
        o.setEffectiveSource(source);
        o.setActualSubmitStatus("CONFIRMED");
        o.setActualConfirmBy(1L);
        o.setActualConfirmTime(LocalDateTime.now());
        offeringMapper.updateById(o);
        auditService.record("COURSE_OFFERING", offeringId, "CONFIRM_ACTUAL", null, null,
                "确认有效课次 " + actual + "（口径：有效课次作为 1/3 门槛与覆盖率分母）");
        return o;
    }

    /** 按周历生成课次（跳过假期周与考试周），一次课一行 */
    @Transactional
    public int generateSessions(Long offeringId, Long teachingClassId) {
        CourseOffering o = offeringMapper.selectById(offeringId);
        if (o == null) {
            throw new BizException("开课实例不存在");
        }
        TeachingClass tc = classMapper.selectById(teachingClassId);
        if (tc == null || !offeringId.equals(tc.getOfferingId())) {
            throw new BizException("教学班不存在或不属于该开课");
        }
        long exists = sessionMapper.selectCount(Wrappers.<CourseSession>lambdaQuery()
                .eq(CourseSession::getTeachingClassId, teachingClassId));
        if (exists > 0) {
            throw new BizException("该教学班已生成课次，如需重建请先在数据库清理（本版不提供一键重建）");
        }
        List<TermCalendar> weeks = calendarMapper.selectList(Wrappers.<TermCalendar>lambdaQuery()
                .eq(TermCalendar::getTermId, o.getTermId()).orderByAsc(TermCalendar::getWeekNo));
        if (weeks.isEmpty()) {
            throw new BizException("该学期还没有周历，请先在「基础数据 → 学期与教学周历」生成周历");
        }
        int times = o.getPlannedTimesPerWeek() == null ? 1 : o.getPlannedTimesPerWeek();
        int sessionNo = 0;
        for (TermCalendar wk : weeks) {
            if ("HOLIDAY".equals(wk.getWeekType()) || "EXAM".equals(wk.getWeekType())) {
                continue;
            }
            for (int i = 0; i < times; i++) {
                sessionNo++;
                CourseSession s = new CourseSession();
                s.setOfferingId(offeringId);
                s.setTeachingClassId(teachingClassId);
                s.setTermId(o.getTermId());
                s.setWeekNo(wk.getWeekNo());
                s.setSessionNo(sessionNo);
                s.setSessionDate(wk.getStartDate().plusDays(Math.min(i, 6)));
                s.setSessionType("NORMAL");
                s.setStatus("NORMAL");
                s.setRollCallStatus("NOT_CALLED");
                s.setIsVoid(0);
                sessionMapper.insert(s);
            }
        }
        auditService.record("COURSE_SESSION", offeringId, "GENERATE", null, null,
                "为教学班 " + tc.getClassCode() + " 生成课次 " + sessionNo + " 次");
        return sessionNo;
    }

    public List<CourseSession> listSessions(Long offeringId, Long teachingClassId) {
        return sessionMapper.selectList(Wrappers.<CourseSession>lambdaQuery()
                .eq(offeringId != null, CourseSession::getOfferingId, offeringId)
                .eq(teachingClassId != null, CourseSession::getTeachingClassId, teachingClassId)
                .orderByAsc(CourseSession::getSessionNo));
    }
}

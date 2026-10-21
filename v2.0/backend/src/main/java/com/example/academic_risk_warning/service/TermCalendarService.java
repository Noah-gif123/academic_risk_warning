package com.example.academic_risk_warning.service;

import com.example.academic_risk_warning.common.BizException;
import com.example.academic_risk_warning.dto.CalendarWeekRequest;
import com.example.academic_risk_warning.entity.Term;
import com.example.academic_risk_warning.entity.TermCalendar;
import com.example.academic_risk_warning.mapper.TermCalendarMapper;
import com.example.academic_risk_warning.mapper.TermMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** 教学周历：按学期起止日期与总周数生成逐周记录，供“应完成课次”计算 */
@Service
@RequiredArgsConstructor
public class TermCalendarService {

    private final TermMapper termMapper;
    private final TermCalendarMapper calendarMapper;
    private final AuditService auditService;

    public List<TermCalendar> listByTerm(Long termId) {
        return calendarMapper.selectList(Wrappers.<TermCalendar>lambdaQuery()
                .eq(TermCalendar::getTermId, termId).orderByAsc(TermCalendar::getWeekNo));
    }

    /** 生成 / 重建周历（按 startDate + totalWeeks，一周 7 天） */
    @Transactional
    public int generate(Long termId, boolean force) {
        Term term = termMapper.selectById(termId);
        if (term == null) {
            throw new BizException("学期不存在");
        }
        long exists = calendarMapper.selectCount(Wrappers.<TermCalendar>lambdaQuery().eq(TermCalendar::getTermId, termId));
        if (exists > 0 && !force) {
            throw new BizException("该学期周历已存在，如需重建请选择强制重建");
        }
        if (exists > 0) {
            calendarMapper.delete(Wrappers.<TermCalendar>lambdaQuery().eq(TermCalendar::getTermId, termId));
        }
        int weeks = term.getTotalWeeks() == null ? 0 : term.getTotalWeeks();
        List<TermCalendar> list = new ArrayList<>();
        for (int i = 1; i <= weeks; i++) {
            TermCalendar c = new TermCalendar();
            c.setTermId(termId);
            c.setWeekNo(i);
            LocalDate start = term.getStartDate().plusDays(7L * (i - 1));
            c.setStartDate(start);
            c.setEndDate(start.plusDays(6));
            c.setWeekType("TEACHING");
            c.setHolidayDays(0);
            calendarMapper.insert(c);
            list.add(c);
        }
        auditService.record("TERM_CALENDAR", termId, "GENERATE", null, null,
                "生成周历 " + list.size() + " 周（学期：" + term.getTermName() + "）");
        return list.size();
    }

    @Transactional
    public TermCalendar updateWeek(Long weekId, CalendarWeekRequest req) {
        TermCalendar c = calendarMapper.selectById(weekId);
        if (c == null) {
            throw new BizException("周次不存在");
        }
        if (req.getWeekType() != null) {
            c.setWeekType(req.getWeekType());
        }
        if (req.getHolidayDays() != null) {
            c.setHolidayDays(req.getHolidayDays());
        }
        if (req.getNote() != null) {
            c.setNote(req.getNote());
        }
        calendarMapper.updateById(c);
        auditService.record("TERM_CALENDAR", c.getTermId(), "UPDATE_WEEK", null, null,
                "修改第 " + c.getWeekNo() + " 周：" + c.getWeekType() + "，假日 " + c.getHolidayDays() + " 天");
        return c;
    }
}

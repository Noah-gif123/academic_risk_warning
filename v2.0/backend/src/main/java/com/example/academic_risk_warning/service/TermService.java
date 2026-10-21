package com.example.academic_risk_warning.service;

import com.example.academic_risk_warning.common.BizException;
import com.example.academic_risk_warning.dto.TermSaveRequest;
import com.example.academic_risk_warning.entity.Term;
import com.example.academic_risk_warning.mapper.TermCalendarMapper;
import com.example.academic_risk_warning.mapper.TermMapper;
import com.example.academic_risk_warning.vo.TermVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/** 学期管理：全库时间轴 */
@Service
@RequiredArgsConstructor
public class TermService {

    private final TermMapper termMapper;
    private final TermCalendarMapper calendarMapper;
    private final AuditService auditService;

    public List<TermVO> list() {
        List<Term> terms = termMapper.selectList(new LambdaQueryWrapper<Term>().orderByDesc(Term::getStartDate));
        return terms.stream().map(this::toVO).sorted(Comparator.comparing(TermVO::getStartDate).reversed())
                .collect(Collectors.toList());
    }

    public TermVO current() {
        Term t = termMapper.selectOne(Wrappers.<Term>lambdaQuery().eq(Term::getIsCurrent, 1).last("limit 1"));
        return t == null ? null : toVO(t);
    }

    @Transactional
    public TermVO save(TermSaveRequest req) {
        if (req.getEndDate().isBefore(req.getStartDate())) {
            throw new BizException("结束日期不能早于开始日期");
        }
        Term t;
        boolean create = req.getId() == null;
        if (create) {
            if (termMapper.selectCount(Wrappers.<Term>lambdaQuery().eq(Term::getTermCode, req.getTermCode())) > 0) {
                throw new BizException("学期编码已存在：" + req.getTermCode());
            }
            t = new Term();
        } else {
            t = termMapper.selectById(req.getId());
            if (t == null) {
                throw new BizException("学期不存在");
            }
        }
        t.setTermCode(req.getTermCode());
        t.setSchoolYear(req.getSchoolYear());
        t.setTermNo(req.getTermNo());
        t.setTermName(req.getTermName());
        t.setStartDate(req.getStartDate());
        t.setEndDate(req.getEndDate());
        t.setTotalWeeks(req.getTotalWeeks());
        if (create) {
            t.setIsCurrent(0);
            t.setStatus("ACTIVE");
            termMapper.insert(t);
        } else {
            termMapper.updateById(t);
        }
        auditService.record("TERM", t.getId(), create ? "CREATE" : "UPDATE", null, null,
                (create ? "新增学期 " : "修改学期 ") + t.getTermName());
        return toVO(t);
    }

    @Transactional
    public void setCurrent(Long id) {
        Term t = termMapper.selectById(id);
        if (t == null) {
            throw new BizException("学期不存在");
        }
        termMapper.update(null, Wrappers.<Term>lambdaUpdate().set(Term::getIsCurrent, 0).eq(Term::getIsCurrent, 1));
        termMapper.update(null, Wrappers.<Term>lambdaUpdate().set(Term::getIsCurrent, 1).eq(Term::getId, id));
        auditService.record("TERM", id, "SET_CURRENT", null, null, "设为当前学期：" + t.getTermName());
    }

    private TermVO toVO(Term t) {
        TermVO vo = new TermVO();
        vo.setId(t.getId());
        vo.setTermCode(t.getTermCode());
        vo.setSchoolYear(t.getSchoolYear());
        vo.setTermNo(t.getTermNo());
        vo.setTermName(t.getTermName());
        vo.setStartDate(t.getStartDate());
        vo.setEndDate(t.getEndDate());
        vo.setTotalWeeks(t.getTotalWeeks());
        vo.setCurrent(t.getIsCurrent() != null && t.getIsCurrent() == 1);
        vo.setStatus(t.getStatus());
        vo.setCalendarWeeks(calendarMapper.selectCount(Wrappers.lambdaQuery(com.example.academic_risk_warning.entity.TermCalendar.class)
                .eq(com.example.academic_risk_warning.entity.TermCalendar::getTermId, t.getId())).intValue());
        return vo;
    }
}

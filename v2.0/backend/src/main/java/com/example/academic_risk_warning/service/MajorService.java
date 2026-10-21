package com.example.academic_risk_warning.service;

import com.example.academic_risk_warning.common.BizException;
import com.example.academic_risk_warning.dto.MajorSaveRequest;
import com.example.academic_risk_warning.entity.Major;
import com.example.academic_risk_warning.mapper.MajorMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 专业（含学院）维护 */
@Service
@RequiredArgsConstructor
public class MajorService {

    private final MajorMapper majorMapper;
    private final AuditService auditService;

    public List<Major> list() {
        return majorMapper.selectList(Wrappers.<Major>lambdaQuery().orderByAsc(Major::getMajorCode));
    }

    @Transactional
    public Major save(MajorSaveRequest req) {
        Major m;
        boolean create = req.getId() == null;
        if (create) {
            if (majorMapper.selectCount(Wrappers.<Major>lambdaQuery().eq(Major::getMajorCode, req.getMajorCode())) > 0) {
                throw new BizException("专业编码已存在：" + req.getMajorCode());
            }
            m = new Major();
        } else {
            m = majorMapper.selectById(req.getId());
            if (m == null) {
                throw new BizException("专业不存在");
            }
        }
        m.setMajorCode(req.getMajorCode());
        m.setMajorName(req.getMajorName());
        m.setCollegeCode(req.getCollegeCode());
        m.setCollegeName(req.getCollegeName());
        if (create) {
            m.setStatus("ACTIVE");
            majorMapper.insert(m);
        } else {
            majorMapper.updateById(m);
        }
        auditService.record("MAJOR", m.getId(), create ? "CREATE" : "UPDATE", null, null,
                (create ? "新增专业 " : "修改专业 ") + m.getMajorName());
        return m;
    }
}

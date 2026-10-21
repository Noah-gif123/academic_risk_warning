package com.example.academic_risk_warning.service;

import com.example.academic_risk_warning.entity.AuditLog;
import com.example.academic_risk_warning.mapper.AuditLogMapper;
import com.example.academic_risk_warning.security.LoginUser;
import com.example.academic_risk_warning.security.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** 审计日志：改分、改考勤、归档、上报、基础数据变更等关键操作统一落库 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogMapper auditLogMapper;

    public void record(String bizType, Long bizId, String action, String beforeJson, String afterJson, String detail) {
        LoginUser user = UserContext.get();
        AuditLog log = new AuditLog();
        log.setBizType(bizType);
        log.setBizId(bizId);
        log.setAction(action);
        if (user != null) {
            log.setOperatorId(user.getAccountId());
            log.setOperatorRole(user.getRole());
        }
        log.setBeforeJson(beforeJson);
        log.setAfterJson(afterJson);
        log.setDetail(detail);
        auditLogMapper.insert(log);
    }
}

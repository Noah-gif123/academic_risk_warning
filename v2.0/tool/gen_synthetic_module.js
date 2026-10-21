/* gen_synthetic_module.js — 第一阶段 1-17 合成数据生成器：dto / vo */
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

w('dto/SyntheticGenerateRequest.java', `
package ${P}.dto;

import lombok.Data;

/**
 * 合成数据生成参数（默认就是课题要求的口径：720 学生 / 6 门课 / 每生 6 门 / 32 课次）。
 * 注意：生成到独立学期（默认 SYN-2027-1），不影响演示学期。
 */
@Data
public class SyntheticGenerateRequest {
    private String termCode;
    private String termName;
    private Integer studentCount;
    private Integer courseCount;
    private Integer sessionsPerCourse;
    private Integer classesPerCourse;
    private Integer attendanceSessions;
    private Boolean generateScores;
    /** 重新生成前清掉本生成器造的数据（只动该学期） */
    private Boolean reset;
}
`);

w('vo/SyntheticResultVO.java', `
package ${P}.vo;

import lombok.Data;

import java.util.List;

/** 生成结果摘要 */
@Data
public class SyntheticResultVO {
    private String termCode;
    private String termName;
    private Long termId;
    private Integer students;
    private Integer courses;
    private Integer offerings;
    private Integer teachingClasses;
    private Integer enrollments;
    private Integer sessions;
    private Integer attendanceRecords;
    private Integer papers;
    private Integer submissions;
    private Integer scoreItems;
    private Integer scoreResults;
    private Integer teacherStyles;
    private Long elapsedMs;
    private List<String> notes;
}
`);

console.log('合成数据模块：dto / vo 生成完成');

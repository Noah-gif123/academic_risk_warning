/* gen_score_result_module.js — 第一阶段 1-10 成绩与批改：待批改汇总 / 手工评分项录入 / 成绩矩阵 / 总评汇总 / 导出 */
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

// ================================================================ dto
w('dto/ScoreEntryDTO.java', `
package ${P}.dto;

import lombok.Data;

import java.math.BigDecimal;

/** 手工评分项的一行录入 */
@Data
public class ScoreEntryDTO {
    private Long studentId;
    private BigDecimal score;
    /** 等级制 / 通过制的原始取值，如 优秀 / 通过 */
    private String rawValue;
    /** 缺交/缺考：按 0 分计（与「还没录入」区分） */
    private Integer isAbsent;
    private String remark;
}
`);

w('dto/ScoreEntryBatchRequest.java', `
package ${P}.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/** 批量录入某个手工评分项 */
@Data
public class ScoreEntryBatchRequest {
    @NotNull(message = "教学班不能为空")
    private Long teachingClassId;
    @NotNull(message = "评分项不能为空")
    private Long schemeItemId;
    private Integer timesNo;
    private List<ScoreEntryDTO> items;
}
`);

// ================================================================ vo
w('vo/ScoreItemColumnVO.java', `
package ${P}.vo;

import lombok.Data;

import java.math.BigDecimal;

/** 成绩矩阵的一列 = 一个叶子评分项 */
@Data
public class ScoreItemColumnVO {
    private Long schemeItemId;
    private String itemName;
    private String path;
    private String category;
    private BigDecimal weight;
    private String dataSource;
    private String scoreType;
    private BigDecimal fullScore;
    private Integer expectedTimes;
    private Long parentId;
}
`);

w('vo/ScoreCellVO.java', `
package ${P}.vo;

import lombok.Data;

import java.math.BigDecimal;

/** 矩阵单元格：一个学生在一个评分项上的成绩（百分制） */
@Data
public class ScoreCellVO {
    private BigDecimal score;
    private String rawValue;
    private String source;
    private Integer isAbsent;
    private Integer times;
    private Boolean missing;
    private Boolean manual;
    private String note;
}
`);

w('vo/ScoreRowVO.java', `
package ${P}.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** 矩阵一行 = 一个学生 */
@Data
public class ScoreRowVO {
    private Long enrollmentId;
    private Long studentId;
    private String studentNo;
    private String studentName;
    private Map<Long, ScoreCellVO> cells;
    private BigDecimal weightedScore;
    private BigDecimal deductionPoints;
    private BigDecimal totalScore;
    private Integer absentCount;
    private Integer lateCount;
    private Integer leaveCount;
    private Integer allowedAbsent;
    private Integer effectiveSessions;
    private Boolean hardFail;
    private Boolean fail;
    private String failReason;
    private String scoreStatus;
    /** 还没录成绩的评分项名称 */
    private List<String> missingItems;
}
`);

w('vo/ScoreSummaryVO.java', `
package ${P}.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/** 矩阵 / 总评的汇总信息 */
@Data
public class ScoreSummaryVO {
    private Integer studentCount;
    private Integer scoredCount;
    private Integer incompleteCount;
    private Integer failCount;
    private Integer hardFailCount;
    private BigDecimal passRate;
    private BigDecimal avgTotal;
    private BigDecimal maxTotal;
    private BigDecimal minTotal;
    private Integer passScore;
    private Boolean schemeLocked;
    private String schemeVersion;
    private List<String> missingItemNames;
}
`);

w('vo/ScoreMatrixVO.java', `
package ${P}.vo;

import lombok.Data;

import java.util.List;

/** 成绩矩阵（列 = 评分项，行 = 学生） */
@Data
public class ScoreMatrixVO {
    private Long teachingClassId;
    private String courseName;
    private String className;
    private String teacherName;
    private List<ScoreItemColumnVO> items;
    private List<ScoreRowVO> rows;
    private ScoreSummaryVO summary;
}
`);

w('vo/PendingGradingVO.java', `
package ${P}.vo;

import lombok.Data;

import java.util.List;

/** 待批改汇总：作业/测试待判主观题 + 项目待评分 */
@Data
public class PendingGradingVO {
    private Integer paperPending;
    private Integer projectPending;

    @Data
    public static class PaperPending {
        private Long paperId;
        private String paperName;
        private String paperType;
        private Integer pendingCount;
        private Integer submittedCount;
    }

    @Data
    public static class ProjectPending {
        private Long projectId;
        private String projectName;
        private Integer submittedCount;
        private Integer durationDays;
    }

    private List<PaperPending> papers;
    private List<ProjectPending> projects;
}
`);

w('vo/StudentScoreVO.java', `
package ${P}.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/** 学生端我的成绩（分项 + 总评；不给 Δ 等教务专属口径） */
@Data
public class StudentScoreVO {
    private Long teachingClassId;
    private String courseName;
    private String teacherName;
    private String termName;
    private Integer passScore;
    private BigDecimal weightedScore;
    private BigDecimal deductionPoints;
    private BigDecimal totalScore;
    private Integer absentCount;
    private Integer lateCount;
    private Integer leaveCount;
    private Integer allowedAbsent;
    private Boolean hardFail;
    private Boolean fail;
    private String failReasonText;
    private String scoreStatus;
    private String scoreStatusText;
    private List<Item> items;

    @Data
    public static class Item {
        private String itemName;
        private String path;
        private BigDecimal weight;
        private BigDecimal score;
        private BigDecimal weightPercent;
        private Boolean missing;
    }
}
`);

console.log('成绩模块：dto / vo 生成完成');

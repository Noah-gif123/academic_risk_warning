package com.example.academic_risk_warning.vo;

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

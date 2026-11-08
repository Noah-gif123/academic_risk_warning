package com.example.academic_risk_warning.vo.stats;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * /api/stats 七接口的 DTO 集合（P1-4）。
 *
 * <p>集中在一处便于 Controller/Service 复用，避免为每个指标建独立实体；
 * 这些类只承载只读统计结果，不映射数据库表。</p>
 */
public final class StatsVos {

    private StatsVos() {
    }

    /** overview：当前风险面 + 周期内处置事件。 */
    @Data
    public static class Overview {
        private String termName;
        private Integer classCount;
        private Integer total;
        private Integer red;
        private Integer orange;
        private Integer yellow;
        private Integer green;
        private BigDecimal avgRiskScore;
        private Integer activeAlerts;
        private Integer alertNew;
        private Integer alertClosed;
        private Integer interventions;
        private Integer improved;
    }

    /** trend：按自然周的过程事件序列（当前无风险历史快照，只做事件口径）。 */
    @Data
    public static class Trend {
        private List<TrendPoint> points = new ArrayList<>();
    }

    @Data
    public static class TrendPoint {
        private String weekStart;
        private Integer alertNew;
        private Integer alertClosed;
        private Integer interventions;
        private Integer improved;
    }

    /** distribution：当前风险分布（等级 / 课程 / 班级）。 */
    @Data
    public static class Distribution {
        private Map<String, Integer> levels = new LinkedHashMap<>();
        private List<DistRow> byCourse = new ArrayList<>();
        private List<DistRow> byClass = new ArrayList<>();
    }

    @Data
    public static class DistRow {
        private String code;
        private String name;
        private Integer red;
        private Integer orange;
        private Integer yellow;
        private Integer green;
        private Integer total;
    }

    /** compare：教学班/课程对比（当前快照，无历史环比）。 */
    @Data
    public static class Compare {
        private List<CompareRow> rows = new ArrayList<>();
    }

    @Data
    public static class CompareRow {
        private String classCode;
        private String className;
        private String courseName;
        private Integer studentCount;
        private Integer red;
        private Integer orange;
        private Integer yellow;
        private Integer green;
        private BigDecimal avgRiskScore;
        private BigDecimal avgScore;
        private Integer hardFail;
    }

    /** funnel：周期内新增预警的处置漏斗。 */
    @Data
    public static class Funnel {
        private List<FunnelStage> stages = new ArrayList<>();
    }

    @Data
    public static class FunnelStage {
        private String key;
        private String name;
        private Integer value;

        public FunnelStage() {
        }

        public FunnelStage(String key, String name, Integer value) {
            this.key = key;
            this.name = name;
            this.value = value;
        }
    }

    /** effect：周期内干预的 T+7 复评成效（P1-4 先用 effective 字段口径）。 */
    @Data
    public static class Effect {
        private Integer interventions;
        private Integer due;
        private Integer improved;
        private Integer unchanged;
        private Integer worsened;
        private Integer pending;
        private BigDecimal improveRate;
        private String note;
    }
}

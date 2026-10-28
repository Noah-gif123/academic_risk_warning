package com.example.academic_risk_warning.vo.ai;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 群体报告展示对象（教学班 / 行政班共用） */
@Data
public class GroupReportVO {

    private Long id;
    private String scope;
    private Long targetId;
    private Long teachingClassId;
    private Long adminClassId;
    private String title;
    private String summary;
    private List<Section> sections = new ArrayList<>();
    private List<Map<String, Object>> actions = new ArrayList<>();
    /** DIFY / LLM / TEMPLATE */
    private String source;
    private String createTime;

    @Data
    public static class Section {
        private String title;
        private String text;
    }
}

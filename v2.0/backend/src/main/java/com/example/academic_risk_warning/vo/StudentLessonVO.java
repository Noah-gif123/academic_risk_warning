package com.example.academic_risk_warning.vo;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/** 学生端目录：一行 = 一次课，含课前 / 课中 / 课后（只含已发布资源） */
@Data
public class StudentLessonVO {
    private Long lessonId;
    private Integer weekNo;
    private Integer sessionNo;
    private LocalDate sessionDate;
    private String title;
    private List<Res> before;
    private List<Res> during;
    private List<Res> after;

    @Data
    public static class Res {
        private Long id;
        private String resType;
        private String title;
        private Long refId;
        private String extUrl;
        private Long fileId;
        private String fileExt;
        private String paperType;
        /** 试卷相关：待交 / 已交 / 已批 / 缺考 */
        private String taskStatus;
        private String projectName;
    }
}

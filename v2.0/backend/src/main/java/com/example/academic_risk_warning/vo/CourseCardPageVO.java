package com.example.academic_risk_warning.vo;

import lombok.Data;

import java.util.List;

/** 分页结果 */
@Data
public class CourseCardPageVO {
    private List<CourseCardVO> items;
    private Integer total;
    private Integer page;
    private Integer size;
    private Integer pages;
}

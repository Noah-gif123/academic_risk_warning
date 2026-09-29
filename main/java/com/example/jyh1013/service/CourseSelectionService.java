package com.example.jyh1013.service;

import com.example.jyh1013.entiy.CourseSelection;

import java.util.List;

public interface CourseSelectionService {
    // 查询所有数据
    List<CourseSelection> getAllCourseSelections();

    // 根据联合主键查询
    CourseSelection getCourseSelectionByPk(String studentNo, String courseNo, String termNo);

    // 新增数据
    boolean addCourseSelection(CourseSelection courseSelection);

    // 修改数据
    boolean updateCourseSelection(CourseSelection courseSelection);

    // 删除数据
    boolean deleteCourseSelection(String studentNo, String courseNo, String termNo);
}

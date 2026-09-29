package com.example.dike1010.service;

import com.example.dike1010.entiy.Course;

import java.util.List;

public interface CourseService {
    int addCourse(Course course);
    Course selectByCourseNo(String courseNo);
    int updateCourse(Course course);
    int deleteCourse(String courseNo);
    List<Course> selectAll();


}


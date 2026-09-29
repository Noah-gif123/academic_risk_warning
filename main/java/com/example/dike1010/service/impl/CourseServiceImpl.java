package com.example.dike1010.service.impl;

import com.example.dike1010.entiy.Course;
import com.example.dike1010.mapper.CourseMapper;
import com.example.dike1010.service.CourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CourseServiceImpl implements CourseService {
    @Autowired
    private CourseMapper courseMapper;
    @Override
    public int addCourse(Course course) {
        return courseMapper.insert(course);
    }
    @Override
    public Course selectByCourseNo(String courseNo) {
        return courseMapper.selectByCourseNo(courseNo);
    }
    @Override
    public int updateCourse(Course course) {
        return courseMapper.update(course);
    }
    @Override
    public int deleteCourse(String courseNo) {
        return courseMapper.delete(courseNo);
    }
    @Override
    public List<Course> selectAll() {
        return courseMapper.selectAll();
    }
}

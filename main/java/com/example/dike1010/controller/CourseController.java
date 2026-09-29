package com.example.dike1010.controller;

import com.example.dike1010.entiy.Course;
import com.example.dike1010.service.CourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
public class CourseController {
    @Autowired
    private CourseService courseService;


    @GetMapping("/{courseNo}")
    public Course getByCourseNo(@PathVariable String courseNo) {
        return courseService.selectByCourseNo(courseNo);
    }

    @GetMapping
    public List<Course> listAll() {
        return courseService.selectAll();
    }

    @PostMapping
    public String create(@RequestBody Course course) {
        courseService.addCourse(course);
        return "success";
    }

    @PutMapping
    public String update(@RequestBody Course course) {
        courseService.updateCourse(course);
        return "success";
    }

    @DeleteMapping("/code/{courseNo}")
    public String deleteByCourseNo(@PathVariable String courseNo) {
        courseService.deleteCourse(courseNo);
        return "success";
    }
}
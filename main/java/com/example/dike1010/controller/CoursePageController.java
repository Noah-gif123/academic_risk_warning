package com.example.dike1010.controller;

import com.example.dike1010.entiy.Course;
import com.example.dike1010.service.CourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/courses")
public class CoursePageController {
    @Autowired
    private CourseService courseService;

    @GetMapping("/form")
    public String showForm(Model model) {
        model.addAttribute("course", new Course());
        model.addAttribute("mode", "create");
        return "course_form";
    }

    @PostMapping("/form")
    public String submitForm(@ModelAttribute Course course) {
        courseService.addCourse(course);
        return "redirect:/courses/success";
    }

    @GetMapping("/success")
    public String success() {
        return "success";
    }

    @GetMapping("/list")
    public String list(Model model) {
        List<Course> courses = courseService.selectAll();
        model.addAttribute("courses", courses);
        return "course_list";
    }

    @GetMapping("/edit/{courseNo}")
    public String edit(Model model, @org.springframework.web.bind.annotation.PathVariable String courseNo) {
        Course course = courseService.selectByCourseNo(courseNo);
        if (course == null) {
            course = new Course();
            course.setCourseNo(courseNo);
        }
        model.addAttribute("course", course);
        model.addAttribute("mode", "edit");
        return "course_form";
    }

    @PostMapping("/update")
    public String update(@ModelAttribute Course course) {
        courseService.updateCourse(course);
        return "redirect:/courses/success";
    }

    @org.springframework.web.bind.annotation.PostMapping("/delete")
    public String delete(@org.springframework.web.bind.annotation.RequestParam String courseNo) {
        courseService.deleteCourse(courseNo);
        return "redirect:/courses/list";
    }

    @GetMapping("/search")
    public String search(@org.springframework.web.bind.annotation.RequestParam(required = false) String courseNo, Model model) {
        List<Course> result;
        if (courseNo == null || courseNo.isEmpty()) {
            result = courseService.selectAll();
        } else {
            Course c = courseService.selectByCourseNo(courseNo);
            result = new java.util.ArrayList<>();
            if (c != null) result.add(c);
        }
        model.addAttribute("courses", result);
        return "course_list";
    }
}
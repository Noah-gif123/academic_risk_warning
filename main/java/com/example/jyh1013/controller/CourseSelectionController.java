package com.example.jyh1013.controller;

import com.example.jyh1013.entiy.CourseSelection;
import com.example.jyh1013.service.CourseSelectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * 学生课程关联控制器（接收前端请求，返回视图/数据）
 */
@Controller
@RequestMapping("/courseSelections")  // 统一请求前缀
public class CourseSelectionController {

    @Autowired
    private CourseSelectionService courseSelectionService;

    /**
     * 1. 列表页：查询所有数据并展示
     */
    @GetMapping
    public String list(Model model) {
        List<CourseSelection> selections = courseSelectionService.getAllCourseSelections();
        model.addAttribute("selections", selections);  // 数据存入Model，供前端页面使用
        return "list";  // 返回list.html页面
    }

    /**
     * 2. 跳转到新增页面
     */
    @GetMapping("/add")
    public String toAddPage(Model model) {
        model.addAttribute("selection", new CourseSelection());  // 传入空实体，供表单绑定
        model.addAttribute("action", "/courseSelections/add");  // 表单提交地址
        model.addAttribute("title", "新增选课记录");  // 页面标题
        return "form";  // 返回form.html页面
    }

    /**
     * 3. 执行新增操作
     */
    @PostMapping("/add")
    public String addCourseSelection(CourseSelection selection) {
        boolean success = courseSelectionService.addCourseSelection(selection);
        if (success) {
            return "redirect:/courseSelections";  // 新增成功，重定向到列表页
        } else {
            return "redirect:/courseSelections/add?error";  // 新增失败，返回新增页并提示错误
        }
    }

    /**
     * 4. 跳转到修改页面（根据联合主键查询数据）
     */
    @GetMapping("/edit")
    public String toEditPage(
            @RequestParam String studentNo,
            @RequestParam String courseNo,
            @RequestParam String termNo,
            Model model) {
        // 根据联合主键查询数据
        CourseSelection selection = courseSelectionService.getCourseSelectionByPk(studentNo, courseNo, termNo);
        model.addAttribute("selection", selection);  // 传入查询到的实体，供表单回显
        model.addAttribute("action", "/courseSelections/edit");  // 表单提交地址
        model.addAttribute("title", "修改选课记录");  // 页面标题
        return "form";  // 返回form.html页面
    }

    /**
     * 5. 执行修改操作
     */
    @PostMapping("/edit")
    public String updateCourseSelection(CourseSelection selection) {
        boolean success = courseSelectionService.updateCourseSelection(selection);
        if (success) {
            return "redirect:/courseSelections";  // 修改成功，重定向到列表页
        } else {
            return "redirect:/courseSelections/edit?studentNo=" + selection.getStudentNo() + "&courseNo=" + selection.getCourseNo() + "&termNo=" + selection.getTermNo() + "&error";
        }
    }

    /**
     * 6. 执行删除操作
     */
    @GetMapping("/delete")
    public String deleteCourseSelection(
            @RequestParam String studentNo,
            @RequestParam String courseNo,
            @RequestParam String termNo) {
        courseSelectionService.deleteCourseSelection(studentNo, courseNo, termNo);
        return "redirect:/courseSelections";  // 删除后重定向到列表页
    }
}

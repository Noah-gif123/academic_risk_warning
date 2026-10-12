package com.example.jyh1013.service.impl;

import com.example.jyh1013.entiy.CourseSelection;
import com.example.jyh1013.mapper.CourseSelectionMapper;

import com.example.jyh1013.service.CourseSelectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * 学生课程关联服务实现（业务逻辑具体实现）
 */
@Service  // 标识为Spring服务组件
public class CourseSelectionServiceImpl implements CourseSelectionService {

    // 注入Mapper接口（Spring自动装配）
    @Autowired
    private CourseSelectionMapper courseSelectionMapper;

    @Override
    public List<CourseSelection> getAllCourseSelections() {
        return courseSelectionMapper.selectAll();
    }

    @Override
    public CourseSelection getCourseSelectionByPk(String studentNo, String courseNo, String termNo) {
        return courseSelectionMapper.selectByPrimaryKey(studentNo, courseNo, termNo);
    }

    @Override
    public boolean addCourseSelection(CourseSelection courseSelection) {
        // 校验必填字段（非空校验）
        if (courseSelection.getStudentNo() == null || courseSelection.getCourseNo() == null || courseSelection.getTermNo() == null) {
            return false;
        }
        // 调用Mapper插入数据，返回受影响行数>0则成功
        return courseSelectionMapper.insert(courseSelection) > 0;
    }

    @Override
    public boolean updateCourseSelection(CourseSelection courseSelection) {
        // 调用Mapper更新数据
        return courseSelectionMapper.update(courseSelection) > 0;
    }

    @Override
    public boolean deleteCourseSelection(String studentNo, String courseNo, String termNo) {
        // 调用Mapper删除数据
        return courseSelectionMapper.deleteByPrimaryKey(studentNo, courseNo, termNo) > 0;
    }
}

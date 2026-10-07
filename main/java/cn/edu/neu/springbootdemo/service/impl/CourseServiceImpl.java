package cn.edu.neu.springbootdemo.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cn.edu.neu.springbootdemo.mapper.CourseMapper;
import cn.edu.neu.springbootdemo.model.Course;
import cn.edu.neu.springbootdemo.service.CourseService;

@Service //使用@Service注解，SpringBoot自动扫描
public class CourseServiceImpl implements CourseService {

	@Autowired
	private CourseMapper courseMapper;

	@Override
	public int addCourse(Course course) {
		// 课程名称已存在，返回 1
		if (courseMapper.countCourse(course) > 0) {
			return 1;
		}
		try {
			courseMapper.addCourse(course);
			// 添加成功，返回 0
			return 0;
		} catch (Exception e) {
			e.printStackTrace();
			// 数据输入格式有误，添加失败，返回 2
			return 2;
		}
	}

	@Override
	public List<Course> getCourseList(Course course) {
		return courseMapper.getCourseList(course);
	}

	@Override
	public int getCourseListCount(Course course) {
		return courseMapper.getCourseListCount(course);
	}

	@Override
	public Course getCourse(String courseid) {
		return courseMapper.getCourse(courseid);
	}

	@Override
	public boolean updateCourse(Course course) {
		return courseMapper.updateCourse(course) > 0;
	}

	@Override
	public boolean deleteCourse(Course course) {
		return courseMapper.deleteCourse(course) > 0;
	}

	@Override
	public boolean checkCoursename(Course course) {
		return courseMapper.countCourse(course) == 0;
	}

}

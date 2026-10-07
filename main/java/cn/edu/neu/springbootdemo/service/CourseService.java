package cn.edu.neu.springbootdemo.service;

import java.util.List;

import cn.edu.neu.springbootdemo.model.Course;

public interface CourseService {

	/**
	 * 新增课程
	 * @return 0-添加成功；1-课程名称已存在；2-数据输入有误，添加失败
	 */
	int addCourse(Course course);

	/**
	 * 按条件查询课程（课程名模糊、教师模糊、学分精确的组合查询），无条件时查询全部
	 */
	List<Course> getCourseList(Course course);

	/**
	 * 统计满足组合查询条件的记录总数，用于分页
	 */
	int getCourseListCount(Course course);

	/**
	 * 按课程id查询单个课程，用于修改前的数据回显
	 */
	Course getCourse(String courseid);

	/**
	 * 修改课程
	 */
	boolean updateCourse(Course course);

	/**
	 * 删除课程，courseids 非空时为批量删除
	 */
	boolean deleteCourse(Course course);

	/**
	 * 校验课程名称是否可用，可用返回 true
	 */
	boolean checkCoursename(Course course);

}

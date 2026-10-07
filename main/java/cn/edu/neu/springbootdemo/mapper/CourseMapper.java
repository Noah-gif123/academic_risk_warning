package cn.edu.neu.springbootdemo.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import cn.edu.neu.springbootdemo.model.Course;

@Mapper//使用@Mapper注解，表示这是操作数据库的mapper，SpringBoot自动扫描
public interface CourseMapper {

	/**
	 * 判断课程名是否已被占用，返回同名课程数量（0 表示课程名可用）
	 */
	int countCourse(Course course);

	/**
	 * 新增课程
	 */
	void addCourse(Course course);

	/**
	 * 查询课程列表，参数为空时查询全部；带条件时做组合查询（课程名模糊、教师模糊、学分精确）
	 */
	List<Course> getCourseList(Course course);

	/**
	 * 统计满足组合查询条件的记录总数，用于分页
	 */
	int getCourseListCount(Course course);

	/**
	 * 按课程id查询单个课程（修改前回显数据用）
	 */
	Course getCourse(@Param("courseid") String courseid);

	/**
	 * 修改课程，返回影响行数
	 */
	int updateCourse(Course course);

	/**
	 * 删除课程，返回影响行数
	 * courseids 非空时按批量删除（多个id用英文逗号分隔），否则按 courseid 单条删除
	 */
	int deleteCourse(Course course);

}

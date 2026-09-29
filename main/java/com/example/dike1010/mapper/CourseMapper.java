package com.example.dike1010.mapper;

import com.example.dike1010.entiy.Course;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface CourseMapper {
    @Insert("insert into course(course_no,course_name,depart_id,depart_name) values (#{courseNo},#{courseName},#{departId},#{departName})")
    int insert(Course course);

    @Select("select * from course where course_no=#{courseNo}")
    Course selectByCourseNo(@Param("courseNo") String courseNo);

    @Update("update course set course_name=#{courseName},depart_id=#{departId},depart_name=#{departName} where course_no=#{courseNo}")
    int update(Course course);

    @Delete("delete from course where course_no=#{courseNo}")
    int delete(@Param("courseNo") String courseNo);

    @Select("select * from course")
    List<Course> selectAll();
}
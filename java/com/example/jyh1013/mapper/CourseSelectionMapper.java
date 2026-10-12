package com.example.jyh1013.mapper;

import com.example.jyh1013.entiy.CourseSelection;
import org.apache.ibatis.annotations.*;
import java.util.List;

/**
 * 纯注解实现Mapper接口（无XML）
 */
@Mapper  // 标识为MyBatis Mapper接口（Spring自动扫描）
public interface CourseSelectionMapper {

    /**
     * 1. 查询所有数据
     */
    @Select("SELECT student_no, course_no, term_no, teaching_class_no, teacher_id " +
            "FROM student_course_rel")
    // 手动指定结果集映射（数据库字段 → 实体类属性，若开启下划线转驼峰可省略，但显式配置更稳妥）
    @Results({
            @Result(column = "student_no", property = "studentNo"),
            @Result(column = "course_no", property = "courseNo"),
            @Result(column = "term_no", property = "termNo"),
            @Result(column = "teaching_class_no", property = "teachingClassNo"),
            @Result(column = "teacher_id", property = "teacherId")
    })
    List<CourseSelection> selectAll();

    /**
     * 2. 根据联合主键查询单条数据
     */
    @Select("SELECT student_no, course_no, term_no, teaching_class_no, teacher_id " +
            "FROM student_course_rel " +
            "WHERE student_no = #{studentNo} " +
            "  AND course_no = #{courseNo} " +
            "  AND term_no = #{termNo}")
    @Results({  // 复用结果集映射（也可定义@ResultMap复用，此处简化直接重复）
            @Result(column = "student_no", property = "studentNo"),
            @Result(column = "course_no", property = "courseNo"),
            @Result(column = "term_no", property = "termNo"),
            @Result(column = "teaching_class_no", property = "teachingClassNo"),
            @Result(column = "teacher_id", property = "teacherId")
    })
    CourseSelection selectByPrimaryKey(
            @Param("studentNo") String studentNo,  // @Param指定参数名，与SQL中#{xxx}对应
            @Param("courseNo") String courseNo,
            @Param("termNo") String termNo);

    /**
     * 3. 新增数据
     */
    @Insert("INSERT INTO student_course_rel " +
            "(student_no, course_no, term_no, teaching_class_no, teacher_id) " +
            "VALUES (#{studentNo}, #{courseNo}, #{termNo}, #{teachingClassNo}, #{teacherId})")
    int insert(CourseSelection courseSelection);  // 直接传入实体类，MyBatis自动解析属性

    /**
     * 4. 修改数据（根据联合主键更新）
     */
    @Update("UPDATE student_course_rel " +
            "SET teaching_class_no = #{teachingClassNo}, " +
            "    teacher_id = #{teacherId} " +
            "WHERE student_no = #{studentNo} " +
            "  AND course_no = #{courseNo} " +
            "  AND term_no = #{termNo}")
    int update(CourseSelection courseSelection);

    /**
     * 5. 根据联合主键删除数据
     */
    @Delete("DELETE FROM student_course_rel " +
            "WHERE student_no = #{studentNo} " +
            "  AND course_no = #{courseNo} " +
            "  AND term_no = #{termNo}")
    int deleteByPrimaryKey(
            @Param("studentNo") String studentNo,
            @Param("courseNo") String courseNo,
            @Param("termNo") String termNo);
}

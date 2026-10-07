package cn.edu.neu.springbootdemo.action;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cn.edu.neu.springbootdemo.model.Course;
import cn.edu.neu.springbootdemo.service.CourseService;

/**
 * 控制层：对外只暴露 /course/xxx 接口，跨域统一由 CrossOriginInterceptor 处理
 */
@RestController
@RequestMapping("/course")
public class CourseAction {

	@Autowired
	private CourseService courseService;

	/**
	 * 新增课程
	 * 访问：/course/addCourse?coursename=Java&credit=3&teacher=张老师
	 */
	@RequestMapping("/addCourse")
	public Map<String, String> addCourse(Course course) {
		Map<String, String> map = new HashMap<String, String>();
		int result = courseService.addCourse(course);
		if (result == 0) {
			map.put("result", "yes");
			map.put("msg", "恭喜您，课程添加成功");
		} else if (result == 1) {
			map.put("result", "no");
			map.put("msg", "对不起，该课程名称已存在，请更换");
		} else {
			map.put("result", "no");
			map.put("msg", "对不起，数据输入格式有误，添加失败");
		}
		return map;
	}

	/**
	 * 显示所有课程 / 按条件查询课程
	 * 访问：/course/getCourseList
	 *      /course/getCourseList?coursename=Java
	 *      /course/getCourseList?teacher=张
	 *      /course/getCourseList?credit=3
	 *      /course/getCourseList?pageNo=1&pageSize=5   （分页）
	 */
	@RequestMapping({ "/getCourseList", "/getAllCourses" })
	public Map<String, Object> getCourseList(Course course) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("courseList", courseService.getCourseList(course));
		// 传入 pageSize 时额外返回分页信息（不传则与原来一致，返回全部记录）
		if (course.getPageSize() > 0) {
			int total = courseService.getCourseListCount(course);
			int pageSize = course.getPageSize();
			map.put("total", total);
			map.put("pageNo", course.getPageNo());
			map.put("pageSize", pageSize);
			map.put("totalPage", total % pageSize == 0 ? total / pageSize : total / pageSize + 1);
		}
		return map;
	}

	/**
	 * 按课程id查询单个课程，用于修改前的数据回显
	 * 访问：/course/getCourse?courseid=1
	 */
	@RequestMapping("/getCourse")
	public Map<String, Course> getCourse(String courseid) {
		Map<String, Course> map = new HashMap<String, Course>();
		map.put("course", courseService.getCourse(courseid));
		return map;
	}

	/**
	 * 修改课程
	 * 访问：/course/updateCourse?courseid=1&coursename=Java&credit=3
	 */
	@RequestMapping("/updateCourse")
	public Map<String, String> updateCourse(Course course) {
		Map<String, String> map = new HashMap<String, String>();
		if (courseService.updateCourse(course)) {
			map.put("result", "yes");
			map.put("msg", "恭喜您，课程修改成功");
		} else {
			map.put("result", "no");
			map.put("msg", "对不起，数据输入格式有误，修改失败");
		}
		return map;
	}

	/**
	 * 删除课程，支持批量删除
	 * 访问：/course/deleteCourse?courseid=1
	 *      /course/deleteCourse?courseid=1&courseid=2   （多值参数，批量删除）
	 *      /course/deleteCourse?courseids=1,2,3         （逗号分隔，批量删除）
	 */
	@RequestMapping("/deleteCourse")
	public Map<String, String> deleteCourse(Course course, HttpServletRequest request) {
		Map<String, String> map = new HashMap<String, String>();
		// 形如 courseid=1&courseid=2 的多值参数，拼接后按批量删除处理
		String[] courseidArray = request.getParameterValues("courseid");
		if (courseidArray != null && courseidArray.length > 1) {
			course.setCourseids(String.join(",", courseidArray));
		}
		if (courseService.deleteCourse(course)) {
			map.put("result", "yes");
			map.put("msg", "恭喜您，课程删除成功");
		} else {
			map.put("result", "no");
			map.put("msg", "对不起，删除失败");
		}
		return map;
	}

	/**
	 * 验证课程名称是否可用
	 * 访问：/course/checkCoursename?coursename=Java
	 */
	@RequestMapping("/checkCoursename")
	public Map<String, String> checkCoursename(Course course) {
		Map<String, String> map = new HashMap<String, String>();
		if (courseService.checkCoursename(course)) {
			map.put("result", "yes");
			map.put("msg", "");
		} else {
			map.put("result", "no");
			map.put("msg", "对不起，该课程名称已存在，请更换");
		}
		return map;
	}

}

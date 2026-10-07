package cn.edu.neu.springbootdemo.model;

/**
 * 课程实体类，与 demo_course 表字段对应
 */
public class Course {

	private int courseid;
	// 课程名称
	private String coursename;
	// 学分。使用包装类型，便于复合查询时判断该条件是否传入
	private java.math.BigDecimal credit;
	// 授课教师
	private String teacher;
	// 上课时间（如：周一第1-2节）
	private String coursetime;
	// 上课地点
	private String classroom;
	// 课程简介
	private String description;

	/* 以下字段不对应表中列，仅用于查询与批量操作 */
	// 批量删除时的课程id集合，多个id用英文逗号分隔，如：1,2,3
	private String courseids;
	// 页码，默认第一页
	private int pageNo = 1;
	// 每页显示的记录数，默认 0 表示不分页（查询全部）
	private int pageSize = 0;

	public int getCourseid() {
		return courseid;
	}

	public void setCourseid(int courseid) {
		this.courseid = courseid;
	}

	public String getCoursename() {
		return coursename;
	}

	public void setCoursename(String coursename) {
		this.coursename = coursename;
	}

	public java.math.BigDecimal getCredit() {
		return credit;
	}

	public void setCredit(java.math.BigDecimal credit) {
		this.credit = credit;
	}

	public String getTeacher() {
		return teacher;
	}

	public void setTeacher(String teacher) {
		this.teacher = teacher;
	}

	public String getCoursetime() {
		return coursetime;
	}

	public void setCoursetime(String coursetime) {
		this.coursetime = coursetime;
	}

	public String getClassroom() {
		return classroom;
	}

	public void setClassroom(String classroom) {
		this.classroom = classroom;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getCourseids() {
		return courseids;
	}

	public void setCourseids(String courseids) {
		this.courseids = courseids;
	}

	public int getPageNo() {
		return pageNo;
	}

	public void setPageNo(int pageNo) {
		this.pageNo = pageNo;
	}

	public int getPageSize() {
		return pageSize;
	}

	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}

	/**
	 * 分页查询时 limit 的起始下标，供 Mapper.xml 使用
	 */
	public int getPageStart() {
		if (pageNo < 1) {
			pageNo = 1;
		}
		return (pageNo - 1) * pageSize;
	}

	@Override
	public String toString() {
		return "Course [courseid=" + courseid + ", coursename=" + coursename + ", credit=" + credit + ", teacher="
				+ teacher + ", coursetime=" + coursetime + ", classroom=" + classroom + ", description=" + description
				+ "]";
	}

}

package cn.edu.neu.springbootdemo.model;

/**
 * 用户实体类，与 demo_users 表字段对应
 */
public class User {

	private int userid;
	private String username;
	private String password;
	private String realname;
	private String birthdate;
	private String gender;
	private String interest;
	private String degree;
	private String intro;
	// 头像地址：不对应表中列，由 user_image 表中 is_portrait=1 的记录带出
	private String portrait;

	/* 以下三个字段不对应表中列，仅用于查询与批量操作 */
	// 按生日区间查询时的起始日期
	private String birthdate1;
	// 按生日区间查询时的结束日期
	private String birthdate2;
	// 批量删除时的用户id集合，多个id用英文逗号分隔，如：1,2,3
	private String userids;

	/* 以下两个字段用于分页查询，同样不对应表中列 */
	// 页码，默认第一页
	private int pageNo = 1;
	// 每页显示的记录数，默认 0 表示不分页（查询全部）
	private int pageSize = 0;

	public int getUserid() {
		return userid;
	}

	public void setUserid(int userid) {
		this.userid = userid;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getRealname() {
		return realname;
	}

	public void setRealname(String realname) {
		this.realname = realname;
	}

	public String getBirthdate() {
		return birthdate;
	}

	public void setBirthdate(String birthdate) {
		this.birthdate = birthdate;
	}

	public String getGender() {
		return gender;
	}

	public void setGender(String gender) {
		this.gender = gender;
	}

	public String getInterest() {
		return interest;
	}

	public void setInterest(String interest) {
		this.interest = interest;
	}

	public String getDegree() {
		return degree;
	}

	public void setDegree(String degree) {
		this.degree = degree;
	}

	public String getIntro() {
		return intro;
	}

	public void setIntro(String intro) {
		this.intro = intro;
	}

	public String getPortrait() {
		return portrait;
	}

	public void setPortrait(String portrait) {
		this.portrait = portrait;
	}

	public String getBirthdate1() {
		return birthdate1;
	}

	public void setBirthdate1(String birthdate1) {
		this.birthdate1 = birthdate1;
	}

	public String getBirthdate2() {
		return birthdate2;
	}

	public void setBirthdate2(String birthdate2) {
		this.birthdate2 = birthdate2;
	}

	public String getUserids() {
		return userids;
	}

	public void setUserids(String userids) {
		this.userids = userids;
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
		return "User [userid=" + userid + ", username=" + username + ", password=" + password + ", realname="
				+ realname + ", birthdate=" + birthdate + ", gender=" + gender + ", interest=" + interest
				+ ", degree=" + degree + ", intro=" + intro + ", birthdate1=" + birthdate1 + ", birthdate2="
				+ birthdate2 + ", userids=" + userids + "]";
	}

}

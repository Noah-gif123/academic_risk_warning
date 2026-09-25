package cn.edu.neu.springbootdemo.model;

public class Attendance {
	private int attid;
	private int userid;
	private String username;
	private String attTime;
	private String attTime1;
	private String attTime2;
	
	public int getAttid() {
		return attid;
	}
	public void setAttid(int attid) {
		this.attid = attid;
	}
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
	public String getAttTime() {
		return attTime;
	}
	public void setAttTime(String attTime) {
		this.attTime = attTime;
	}
	public String getAttTime1() {
		return attTime1;
	}
	public void setAttTime1(String attTime1) {
		this.attTime1 = attTime1;
	}
	public String getAttTime2() {
		return attTime2;
	}
	public void setAttTime2(String attTime2) {
		this.attTime2 = attTime2;
	}
	@Override
	public String toString() {
		return "Attendance [attid=" + attid + ", userid=" + userid + ", username=" + username + ", attTime=" + attTime
				+ ", attTime1=" + attTime1 + ", attTime2=" + attTime2 + "]";
	}
	
}

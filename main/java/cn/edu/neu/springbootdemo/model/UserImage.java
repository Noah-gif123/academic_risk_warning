package cn.edu.neu.springbootdemo.model;

import org.springframework.web.multipart.MultipartFile;

public class UserImage {
	private int imageid;
	private int userid;
	private MultipartFile file;
	private String imageUrl;
	private int isPortrait;

	// 照片墙总览页要连带展示照片所属的用户名与真实姓名
	private String username;
	private String realname;
	
	public int getUserid() {
		return userid;
	}
	public void setUserid(int userid) {
		this.userid = userid;
	}
	public MultipartFile getFile() {
		return file;
	}
	public void setFile(MultipartFile file) {
		this.file = file;
	}
	public int getImageid() {
		return imageid;
	}
	public void setImageid(int imageid) {
		this.imageid = imageid;
	}
	public String getImageUrl() {
		return imageUrl;
	}
	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}
	
	public int getIsPortrait() {
		return isPortrait;
	}
	public void setIsPortrait(int isPortrait) {
		this.isPortrait = isPortrait;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getRealname() {
		return realname;
	}
	public void setRealname(String realname) {
		this.realname = realname;
	}
	@Override
	public String toString() {
		return "UserImage [imageid=" + imageid + ", userid=" + userid + ", file=" + file + ", imageUrl=" + imageUrl
				+ ", isPortrait=" + isPortrait + "]";
	}
}

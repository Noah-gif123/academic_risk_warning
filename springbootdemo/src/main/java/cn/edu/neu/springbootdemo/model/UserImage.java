package cn.edu.neu.springbootdemo.model;

import org.springframework.web.multipart.MultipartFile;

public class UserImage {
	private int imageid;
	private int userid;
	private MultipartFile file;
	private String imageUrl;
	private int isPortrait;
	
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
	@Override
	public String toString() {
		return "UserImage [imageid=" + imageid + ", userid=" + userid + ", file=" + file + ", imageUrl=" + imageUrl
				+ ", isPortrait=" + isPortrait + "]";
	}
}

package cn.edu.neu.springbootdemo.model;

public class Image {
	private int userid;
	private String img;
	private String imgType;
	
	public int getUserid() {
		return userid;
	}
	public void setUserid(int userid) {
		this.userid = userid;
	}
	public String getImg() {
		return img;
	}
	public void setImg(String img) {
		this.img = img;
	}
	public String getImgType() {
		return imgType;
	}
	public void setImgType(String imgType) {
		this.imgType = imgType;
	}
	@Override
	public String toString() {
		return "Image [userid=" + userid + ", img=" + img + ", imgType=" + imgType + "]";
	}
}

package cn.edu.neu.springbootdemo.service;

import java.util.List;

import cn.edu.neu.springbootdemo.model.UserImage;

/**
 * 照片墙业务接口
 */
public interface UserImageService {

	/**
	 * 查询照片墙总览：全部用户的照片，userid 大于 0 时只查该用户
	 */
	List<UserImage> getAllImages(Integer userid);

	/**
	 * 查询用户的照片列表
	 */
	List<UserImage> getUserImages(int userid);

	/**
	 * 保存上传成功的照片，返回带主键的照片对象
	 */
	UserImage addUserImage(int userid, String imageUrl);

	/**
	 * 删除照片：先删数据库记录，再删磁盘文件
	 */
	boolean deleteUserImage(int imageid);

	/**
	 * 设为头像：先取消该用户原有头像，再把指定照片设为头像
	 */
	boolean setPortrait(int userid, int imageid);

}

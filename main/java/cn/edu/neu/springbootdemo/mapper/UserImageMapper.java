package cn.edu.neu.springbootdemo.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import cn.edu.neu.springbootdemo.model.UserImage;

/**
 * 照片墙：用户照片表（user_image）的持久层接口
 */
@Mapper
public interface UserImageMapper {

	/**
	 * 查询照片墙总览数据：全部用户的照片，可按用户筛选；头像排最前，其余按上传时间倒序
	 */
	List<UserImage> getAllImages(Integer userid);

	/**
	 * 查询某个用户的全部照片，头像排在最前，其余按上传时间倒序
	 */
	List<UserImage> getUserImages(int userid);

	/**
	 * 按照片id查询单张照片，删除前需要拿到图片地址
	 */
	UserImage getUserImage(int imageid);

	/**
	 * 保存一条照片记录，新增照片默认不是头像
	 */
	int addUserImage(UserImage userImage);

	/**
	 * 删除一条照片记录
	 */
	int deleteUserImage(int imageid);

	/**
	 * 取消该用户原有头像
	 */
	int clearPortrait(int userid);

	/**
	 * 把指定照片设为该用户的头像
	 */
	int setPortrait(@Param("userid") int userid, @Param("imageid") int imageid);

}

package cn.edu.neu.springbootdemo.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cn.edu.neu.springbootdemo.core.Constants;
import cn.edu.neu.springbootdemo.core.util.FileUtil;
import cn.edu.neu.springbootdemo.mapper.UserImageMapper;
import cn.edu.neu.springbootdemo.model.UserImage;
import cn.edu.neu.springbootdemo.service.UserImageService;

@Service
public class UserImageServiceImpl implements UserImageService {

	@Autowired
	private UserImageMapper userImageMapper;

	@Override
	public List<UserImage> getAllImages(Integer userid) {
		return userImageMapper.getAllImages(userid);
	}

	@Override
	public List<UserImage> getUserImages(int userid) {
		return userImageMapper.getUserImages(userid);
	}

	@Override
	public UserImage addUserImage(int userid, String imageUrl) {
		UserImage userImage = new UserImage();
		userImage.setUserid(userid);
		userImage.setImageUrl(imageUrl);
		userImageMapper.addUserImage(userImage);
		return userImage;
	}

	@Override
	public boolean deleteUserImage(int imageid) {
		// 删除前先查出图片地址，删库成功后再删磁盘文件
		UserImage userImage = userImageMapper.getUserImage(imageid);
		if (userImage == null) {
			return false;
		}
		if (userImageMapper.deleteUserImage(imageid) <= 0) {
			return false;
		}
		FileUtil.deleteFileInDir(Constants.UPLOAD_DIR, userImage.getImageUrl());
		return true;
	}

	@Override
	@Transactional
	public boolean setPortrait(int userid, int imageid) {
		// 同一用户只允许一张头像，设置前先清空原有头像标记
		userImageMapper.clearPortrait(userid);
		return userImageMapper.setPortrait(userid, imageid) > 0;
	}

}

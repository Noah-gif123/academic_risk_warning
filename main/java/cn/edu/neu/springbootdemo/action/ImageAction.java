package cn.edu.neu.springbootdemo.action;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import cn.edu.neu.springbootdemo.core.Constants;
import cn.edu.neu.springbootdemo.core.util.FileUtil;
import cn.edu.neu.springbootdemo.model.UserImage;
import cn.edu.neu.springbootdemo.service.UserImageService;

/**
 * 控制层：照片墙相关接口，上传的文件保存在磁盘 upload 目录，通过 /upload/** 静态访问
 */
@RestController
@RequestMapping("/image")
public class ImageAction {

	@Autowired
	private UserImageService userImageService;

	/**
	 * 上传照片（可一次选多张，前端逐个调用）
	 * 访问：/springbootdemo/image/upload  表单：file、userid
	 */
	@RequestMapping("/upload")
	public Map<String, Object> upload(@RequestParam("file") MultipartFile file, int userid,
			HttpServletRequest request) {
		Map<String, Object> map = new HashMap<String, Object>();
		if (userid <= 0) {
			map.put("result", "no");
			map.put("msg", "对不起，未指定上传照片的用户");
			return map;
		}
		if (file == null || file.isEmpty()) {
			map.put("result", "no");
			map.put("msg", "对不起，请选择要上传的照片");
			return map;
		}
		if (!isImage(file)) {
			map.put("result", "no");
			map.put("msg", "对不起，只能上传 jpg/png/gif 格式的图片");
			return map;
		}

		String fileName = FileUtil.uploadFileToDir(file, Constants.UPLOAD_DIR);
		if (fileName == null) {
			map.put("result", "no");
			map.put("msg", "对不起，照片保存失败");
			return map;
		}

		// 带上 context-path，前端无论是走 devServer 代理还是部署到同一个站点，都能直接访问
		String imageUrl = request.getContextPath() + Constants.IMAGE_BASEURL + fileName;
		UserImage userImage = userImageService.addUserImage(userid, imageUrl);
		map.put("result", "yes");
		map.put("msg", "恭喜您，上传成功");
		map.put("imageUrl", imageUrl);
		map.put("imageid", userImage.getImageid());
		return map;
	}

	/**
	 * 照片墙总览：查询全部照片（带所属用户名与真实姓名），userid 非空时只查该用户
	 */
	@RequestMapping("/getAllImages")
	public Map<String, Object> getAllImages(@RequestParam(required = false) Integer userid) {
		Map<String, Object> map = new HashMap<String, Object>();
		List<UserImage> imageList = userImageService.getAllImages(userid);
		map.put("imageList", imageList);
		map.put("total", imageList.size());
		return map;
	}

	/**
	 * 查询某个用户的照片列表
	 */
	@RequestMapping("/getUserImages")
	public Map<String, Object> getUserImages(int userid) {
		Map<String, Object> map = new HashMap<String, Object>();
		List<UserImage> imageList = userImageService.getUserImages(userid);
		map.put("imageList", imageList);
		return map;
	}

	/**
	 * 删除照片：同时删除数据库记录与磁盘文件
	 * 访问：/springbootdemo/image/deleteImage?imageid=1
	 */
	@RequestMapping("/deleteImage")
	public Map<String, String> deleteImage(int imageid) {
		Map<String, String> map = new HashMap<String, String>();
		if (userImageService.deleteUserImage(imageid)) {
			map.put("result", "yes");
			map.put("msg", "恭喜您，删除成功");
		} else {
			map.put("result", "no");
			map.put("msg", "对不起，删除失败");
		}
		return map;
	}

	/**
	 * 设为头像
	 * 访问：/springbootdemo/image/setPortrait?userid=1&imageid=1
	 */
	@RequestMapping("/setPortrait")
	public Map<String, String> setPortrait(int userid, int imageid) {
		Map<String, String> map = new HashMap<String, String>();
		if (userImageService.setPortrait(userid, imageid)) {
			map.put("result", "yes");
			map.put("msg", "恭喜您，设置成功");
		} else {
			map.put("result", "no");
			map.put("msg", "对不起，设置失败");
		}
		return map;
	}

	// 只接收图片文件：按内容类型判断，类型为空时按扩展名兜底
	private boolean isImage(MultipartFile file) {
		String contentType = file.getContentType();
		if (contentType != null && contentType.startsWith("image/")) {
			return true;
		}
		String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
		return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".gif");
	}

}

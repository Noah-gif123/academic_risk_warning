package cn.edu.neu.springbootdemo.core;

public class Constants {
	public static final String LOGIN_ERR = "登录失败";
	public static final String LOGIN_PROMPT = "请先登录";
	public static final String INDEX_PAGE = "/";

	public static final String SUCCESS = "success";
	public static final String ERROR = "error";

	/* 系统内部编码 */
	public static final String ENCODING = "UTF-8";

	/* 通用操作结果页面返回值 */
	public static final String EXECUTE_RESULT = "/common/execute_result";
	public static final String EXECUTE_RESULT_NAME = "execute_result";

	/* 默认分页尺寸及分页标记 */
	public static final int DEFAULT_PAGE_SIZE = 10;
	public static final int MAX_PAGE_SIZE = 1000;
	public static final String NORMAL_MARK = "?";
	public static final String START_MARK = ":_START_INDEX_";
	public static final String END_MARK = ":_END_INDEX_";

	/* 记录返回页面地址用的session key */
	public static final String REFER_URL = "_REFER_URL_";
	public static final String REFER_URL_DEFAULT_KEY = "_REFER_URL_DEFAULT_KEY_";

	/* 登录后用户信息在Session中的Key */
	public static final String LOGIN_USER = "_LOGIN_USER_";

	/* 记录用户登录前想要访问的地址在Session中的Key */
	public static final String ORIGINAL_URL = "_ORIGINAL_URL_";
	
	/* 上传文件路径 */
	public static final String UPLOAD_PATH = "/static/upload/";
	public static final String IMAGE_BASEURL = "/upload/";

	/* 照片墙：上传文件实际保存的磁盘目录（项目运行目录下的 upload 目录），
	   与静态资源映射 /upload/** 对应，避免放在 classpath 下被打包清理掉 */
	public static final String UPLOAD_DIR = System.getProperty("user.dir") + "/upload/";
	
	/* 设置APPID/AK/SK */
    public static final String APP_ID = "24467902";//你的 App ID
    public static final String API_KEY = "UunpFXo02GGnYm3622ULeKoa";//你的 Api Key
    public static final String SECRET_KEY = "znXPeW7nyZGi8RxlzPhB00bTv3EfvWIN";//你的 Secret Key
	
	/* 人脸识别，人脸库用户组ID和默认图片类型 */
	public static final String GROUP_ID = "neusoft";
	public static final String IMAGE_TYPE = "BASE64";

}
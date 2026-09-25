package cn.edu.neu.springbootdemo.aipface;

import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONObject;

import com.baidu.aip.face.AipFace;
import com.baidu.aip.face.MatchRequest;

import cn.edu.neu.springbootdemo.core.Constants;
import cn.edu.neu.springbootdemo.model.Image;
import cn.edu.neu.springbootdemo.model.User;

public class AipUtil {
	//两张人脸图片对比
	public static JSONObject matchFace(AipFace client,Image imageU,Image imageC){		
		MatchRequest req1 = new MatchRequest(imageU.getImg(), imageU.getImgType());
	    MatchRequest req2 = new MatchRequest(imageC.getImg(), imageC.getImgType());
	    ArrayList<MatchRequest> requests = new ArrayList<MatchRequest>();
	    requests.add(req1);
	    requests.add(req2);
	    JSONObject res = client.match(requests);
		return res;//res.toString(2);
	}
	
	//人脸库搜索
	public static JSONObject findFace(AipFace client,Image imageU,String userid){
		// 传入可选参数调用接口
	    HashMap<String, Object> options = new HashMap<String, Object>();
	    options.put("max_face_num", "1");
	    options.put("match_threshold", "80");
	    options.put("quality_control", "NORMAL");
	    options.put("liveness_control", "LOW");
	    options.put("user_id", userid);
	    options.put("max_user_num", "1");
	    
	    String groupIdList = Constants.GROUP_ID;//你的人脸库名称
	    
	    // 人脸搜索
	    JSONObject res = client.search(imageU.getImg(), imageU.getImgType(), groupIdList, options);
	    return res;//res.toString(2);
	}
	
	//人脸注册
	public static JSONObject registFace(AipFace client,String groupid,User user,Image image){
		// 传入可选参数调用接口
	    HashMap<String, String> options = new HashMap<String, String>();
	    options.put("user_info", user.getUsername());
	    options.put("quality_control", "NORMAL");
	    options.put("liveness_control", "LOW");
	    //操作方式 APPEND: 当user_id在库中已经存在时，对此user_id重复注册时，新注册的图片默认会追加到该user_id下,REPLACE : 当对此user_id重复注册时,则会用新图替换库中该user_id下所有图片,默认使用APPEND
	    options.put("action_type", "APPEND");
	    
	    // 人脸注册
	    JSONObject res = client.addUser(image.getImg(), image.getImgType(), groupid, user.getUserid()+"", options);
	    
		return res;//res.toString(2); 
	}
	
	//人脸修改
	public static JSONObject updateFace(AipFace client,String groupid,User user,Image image){
		// 传入可选参数调用接口
	    HashMap<String, String> options = new HashMap<String, String>();
	    options.put("user_info", user.getUsername());
	    options.put("quality_control", "NORMAL");
	    options.put("liveness_control", "LOW");
	    options.put("action_type", "REPLACE");
	    
	    // 人脸更新
	    JSONObject res = client.updateUser(image.getImg(), image.getImgType(), groupid, user.getUserid()+"", options);
	    System.out.println(res.toString(2));
	    
	    return res;//res.toString(2);
	}
	
	//人脸检测
	public static JSONObject detectFace(AipFace client,Image image){
		// 传入可选参数调用接口
		HashMap<String, Object> options= new HashMap<String, Object>();
		options.put("face_field", "age,beauty,expression,face_shape,gender,glasses");//返回的人脸信息
		options.put("max_face_num", "1");//最多处理人脸的数目，默认值为1，仅检测图片中面积最大的那个人脸；最大值10，检测图片中面积最大的几张人脸。
		options.put("face_type", "LIVE");//人脸的类型 LIVE表示生活照
	    options.put("liveness_control", "LOW");//较低的活体要求
	    
	    // 人脸检测
		JSONObject res=client.detect(image.getImg(), image.getImgType(), options);
		
		return res;//res.toString(2);
	}

	//用户删除
	public static JSONObject deleteUser(AipFace client, String groupid, String userid){
		// 传入可选参数调用接口
	    HashMap<String, String> options = new HashMap<String, String>();
	    
	    // 删除用户
	    JSONObject res = client.deleteUser(groupid, userid, options);
	    System.out.println(res.toString(2));
	    
	    return res;//res.toString(2);
	}
}

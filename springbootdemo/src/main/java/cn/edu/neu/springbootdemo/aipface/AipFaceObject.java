package cn.edu.neu.springbootdemo.aipface;

import com.baidu.aip.face.AipFace;

import cn.edu.neu.springbootdemo.core.Constants;

public class AipFaceObject {
    
    //初始化一个AipFace
    private static AipFace client = new AipFace(Constants.APP_ID, Constants.API_KEY, Constants.SECRET_KEY);
    
    //单例使用，避免重复获取access_token
    public static AipFace getClient(){
    	// 可选：设置网络连接参数
        client.setConnectionTimeoutInMillis(2000);
        client.setSocketTimeoutInMillis(60000);

        // 可选：设置代理服务器地址, http和socket二选一，或者均不设置
        //client.setHttpProxy("proxy_host", proxy_port);  // 设置http代理
        //client.setSocketProxy("proxy_host", proxy_port);  // 设置socket代理

    	return client;
    }
}

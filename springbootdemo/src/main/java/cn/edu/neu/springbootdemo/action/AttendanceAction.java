package cn.edu.neu.springbootdemo.action;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cn.edu.neu.springbootdemo.aipface.AipFaceObject;
import cn.edu.neu.springbootdemo.aipface.AipUtil;
import cn.edu.neu.springbootdemo.core.Constants;
import cn.edu.neu.springbootdemo.model.Attendance;
import cn.edu.neu.springbootdemo.model.Image;
import cn.edu.neu.springbootdemo.model.User;
import cn.edu.neu.springbootdemo.service.AttendanceService;

@RestController
@RequestMapping("/att")
public class AttendanceAction {
	

}

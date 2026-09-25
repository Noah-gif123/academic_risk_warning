package cn.edu.neu.springbootdemo.action;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import cn.edu.neu.springbootdemo.core.Constants;
import cn.edu.neu.springbootdemo.model.User;
import cn.edu.neu.springbootdemo.service.UserService;

@CrossOrigin
@RestController
@RequestMapping("/user")
public class UserAction { 

}

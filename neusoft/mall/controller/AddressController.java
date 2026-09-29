package cn.edu.neusoft.mall.controller;

import cn.edu.neusoft.framework.annotations.Controller;
import cn.edu.neusoft.framework.annotations.RequestMapping;
import cn.edu.neusoft.framework.annotations.RequestParam;
import cn.edu.neusoft.framework.db.PageInfo;
import cn.edu.neusoft.framework.enums.RequestMethod;
import cn.edu.neusoft.framework.model.ModelAndView;
import cn.edu.neusoft.mall.entiy.Address;
import cn.edu.neusoft.mall.entiy.User;
import cn.edu.neusoft.mall.service.AddressService;
import cn.edu.neusoft.mall.service.UserService;
import cn.edu.neusoft.mall.service.impl.AddressServiceImpl;
import cn.edu.neusoft.mall.service.impl.UserServiceImpl;

import cn.edu.neusoft.mall.until.Constant;
import com.mysql.cj.util.StringUtils;

import javax.servlet.http.HttpSession;
import java.util.UUID;

@Controller
@RequestMapping("/address")
public class AddressController {

    UserService userService = new UserServiceImpl();
    AddressService addressService = new AddressServiceImpl();
    @RequestMapping("/getAddressList")
    public ModelAndView getAddressList(HttpSession session, @RequestParam(name = "page", defaultValue = "1") Integer page)
    {
        System.out.println("########## 进入 getAddressList 方法 ##########");
        ModelAndView modelAndView = new ModelAndView("/address/addressList");
        User userInfo = userService.getCurrentUser(session);
        if (userInfo != null) {
            PageInfo<Address> addressList = addressService.getAddressList(page, String.valueOf(userInfo.getId()));
            modelAndView.addObject("addressList", addressList);
        }
        return modelAndView;

    }

    @RequestMapping("/putAddress")
    public String putAddress(Address address, HttpSession session) {
        User userInfo = (User) session.getAttribute(Constant.LOGIN_USER);
        if (userInfo != null) {
            address.setUserId(String.valueOf(userInfo.getId()));
        }
        if (address.getId() == null || address.getId().equals("")) {
            address.setId(UUID.randomUUID().toString());
            int i = addressService.insertAddress(address);
            if (i > 0) {
                return "redirect:../address/getAddressList.action?put-success=1";
            } else {
                return "redirect:../address/getAddressList.action?put-success=-1";
            }
        } else {
            int i = addressService.modifyAddress(address);
            if (i > 0) {
                return "redirect:../address/getAddressList.action?put-success=1";
            } else {
                return "redirect:../address/getAddressList.action?put-success=-1";
            }
        }
    }

    @RequestMapping(value="/deleteAddress",methods= RequestMethod.POST)
    public String deleteAddress(@RequestParam(name = "id") String id) {
        addressService.deleteAddress(id);
        return "redirect:getAddressList.action";
    }

}

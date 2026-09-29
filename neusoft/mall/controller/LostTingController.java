package cn.edu.neusoft.mall.controller;

import cn.edu.neusoft.framework.annotations.Controller;
import cn.edu.neusoft.framework.annotations.RequestMapping;
import cn.edu.neusoft.framework.model.ModelAndView;
import cn.edu.neusoft.mall.entiy.LostTing;
import cn.edu.neusoft.mall.entiy.User;
import cn.edu.neusoft.mall.service.LostTingService;
import cn.edu.neusoft.mall.service.impl.LostTingServiceImpl;
import cn.edu.neusoft.mall.until.Constant;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/lostThing")
public class LostTingController {
    LostTingService lostTingService = new LostTingServiceImpl();

    @RequestMapping("/publishLostThing")
    public ModelAndView publishLostThing(HttpServletRequest request) {
        System.out.println("########## 进入 publishLostThing 方法 ##########");
        ModelAndView mv = new ModelAndView("/lostThing/LostThingList");

        try {
            // 1. 处理文件上传
            String uploadDir = request.getServletContext().getRealPath("/uploads/");
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
                System.out.println("创建上传目录: " + uploadDir);
            }

            StringBuilder imageUrls = new StringBuilder();

            // 检查是否为文件上传请求
            boolean isMultipart = request.getContentType() != null
                    && request.getContentType().toLowerCase().startsWith("multipart/form-data");

            if (isMultipart) {
                // 处理多部分请求
                for (Part part : request.getParts()) {
                    if (part.getName().equals("image_path") && part.getSize() > 0) {
                        String fileName = getFileName(part);
                        if (fileName != null && !fileName.isEmpty()) {
                            // 生成唯一文件名
                            String fileExt = fileName.substring(fileName.lastIndexOf("."));
                            String uniqueFileName = UUID.randomUUID().toString() + fileExt;

                            // 保存文件
                            part.write(uploadDir + uniqueFileName);
                            System.out.println("图片保存成功: " + uploadDir + uniqueFileName);

                            // 记录图片URL
                            imageUrls.append("/uploads/").append(uniqueFileName).append(",");
                        }
                    }
                }
            } else {
                System.out.println("警告：非文件上传请求，可能缺少enctype=\"multipart/form-data\"");
            }

            // 去除最后一个逗号
            if (imageUrls.length() > 0) {
                imageUrls.setLength(imageUrls.length() - 1);
            }
            System.out.println("上传的图片URL: " + imageUrls.toString());

            // 2. 获取并校验发布类型
            String publishType = request.getParameter("lostThingList");
            System.out.println("lostThingList: " + publishType);
            if (publishType == null || !(publishType.equalsIgnoreCase("lost") || publishType.equalsIgnoreCase("found"))) {
                throw new IllegalArgumentException("无效的发布类型: " + (publishType != null ? publishType : "空值"));
            }

            // 3. 获取并校验所有必需参数
            System.out.println("开始校验必需参数...");

            String itemName = getRequiredParam(request, "itemName", "物品类型不能为空");
            System.out.println("成功获取 itemName: " + itemName);

            int categoryId;
            try {
                String categoryIdStr = request.getParameter("categoryId");
                System.out.println("获取到的categoryId参数: " + categoryIdStr);
                if (categoryIdStr == null || categoryIdStr.trim().isEmpty()) {
                    throw new IllegalArgumentException("分类ID不能为空");
                }
                categoryId = Integer.parseInt(categoryIdStr);
                System.out.println("解析后的categoryId: " + categoryId);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("分类ID必须为整数");
            }

            String location = getRequiredParam(request, "location", "地点不能为空");
            System.out.println("成功获取 location: " + location);

            String timeStr = getRequiredParam(request, "time", "时间不能为空");
            System.out.println("成功获取 time: " + timeStr);

            String contact_info = getRequiredParam(request, "contact_info", "联系方式不能为空");
            System.out.println("成功获取 contact_info: " + contact_info);

            String itemDescription = getRequiredParam(request, "description", "物品描述不能为空");
            System.out.println("成功获取 description: " + itemDescription);

            String title = getRequiredParam(request, "title", "物品名称不能为空");
            System.out.println("成功获取 title: " + title);

            // 4. 获取可选参数
            System.out.println("开始获取可选参数...");

            String uniqueFeatures = request.getParameter("uniqueFeatures");
            System.out.println("uniqueFeatures: " + (uniqueFeatures != null ? uniqueFeatures : "无"));

            boolean privateContact = "true".equalsIgnoreCase(request.getParameter("privateContact"));
            System.out.println("privateContact: " + privateContact);

            boolean verifyIdentity = "true".equalsIgnoreCase(request.getParameter("verifyIdentity"));
            System.out.println("verifyIdentity: " + verifyIdentity);

            // 5. 处理时间格式
            System.out.println("开始解析日期: " + timeStr);
            Date time;
            try {
                time = parseDate(timeStr);
                System.out.println("日期解析成功: " + time);
            } catch (ParseException e) {
                throw new IllegalArgumentException("时间格式错误，请使用YYYY-MM-DDTHH:mm格式");
            }

            // 6. 创建并设置 LostTing 对象
            LostTing lostTing = new LostTing();
            lostTing.setItemName(itemName);
            lostTing.setTitle(title);
            lostTing.setCategoryId(categoryId);
            lostTing.setLocation(location);
            lostTing.setDate(time);
            lostTing.setDescription(itemDescription);
            lostTing.setStatus(publishType.equalsIgnoreCase("lost") ? "丢失" : "拾到");
            lostTing.setUniqueFeatures(uniqueFeatures);
            lostTing.setPrivateContact(privateContact);
            lostTing.setVerifyIdentity(verifyIdentity);
            lostTing.setImage_path(imageUrls.toString()); // 设置图片URL

            System.out.println("创建的LostTing对象: " + lostTing);

            // 7. 获取用户ID
            Integer userId = null;
            HttpSession session = request.getSession(false);

            if (session != null) {
                Object userObj = session.getAttribute(Constant.LOGIN_USER);
                if (userObj != null && userObj instanceof User) {
                    User userInfo = (User) userObj;

                    int id = userInfo.getId();
                    if (id > 0) {
                        userId = id;
                        System.out.println("从session获取到有效用户ID: " + userId);
                    } else {
                        System.out.println("用户ID无效（需 > 0）");
                    }

                } else {
                    System.out.println("session中未找到有效用户信息（类型不匹配或为空）");
                }
            } else {
                System.out.println("用户session不存在，未登录");
            }

            // 8. 调用Service处理业务逻辑
            Map<String, Object> serviceResult;
            if (userId != null && userId > 0) {
                System.out.println("调用Service处理业务逻辑...");
                serviceResult = lostTingService.publishItem(lostTing, userId);
                System.out.println("Service返回结果: " + serviceResult);
            } else {
                throw new IllegalStateException("用户未登录或登录状态失效，请重新登录");
            }

            // 9. 将 Service 结果传递给视图
            for (Map.Entry<String, Object> entry : serviceResult.entrySet()) {
                mv.addObject(entry.getKey(), entry.getValue());
            }

            // 设置成功标志
            mv.addObject("success", true);
            System.out.println("发布成功，返回视图");

        } catch (IllegalArgumentException e) {
            System.out.println("业务参数异常: " + e.getMessage());
            mv.addObject("success", false);
            mv.addObject("message", "表单填写错误: " + e.getMessage());
        } catch (IllegalStateException e) {
            System.out.println("业务状态异常: " + e.getMessage());
            mv.addObject("success", false);
            mv.addObject("message", e.getMessage());
        } catch (IOException e) {
            System.out.println("文件上传异常: " + e.getMessage());
            mv.addObject("success", false);
            mv.addObject("message", "图片上传失败: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("系统异常: " + e.getClass().getName() + " - " + e.getMessage());
            mv.addObject("success", false);
            mv.addObject("message", "系统错误: " + e.getMessage());
            e.printStackTrace();
        }

        return mv;
    }

    // 辅助方法：获取文件名
    private String getFileName(Part part) {
        for (String content : part.getHeader("content-disposition").split(";")) {
            if (content.trim().startsWith("filename")) {
                return content.substring(content.indexOf('=') + 1).trim().replace("\"", "");
            }
        }
        return null;
    }

    // 辅助方法：获取必填参数
    private String getRequiredParam(HttpServletRequest request, String paramName, String errorMsg) {
        System.out.println("尝试获取参数: " + paramName);
        String value = request.getParameter(paramName);
        System.out.println(paramName + " 的值: " + (value != null ? value : "空值"));

        if (value == null || value.trim().isEmpty()) {
            System.out.println("参数 " + paramName + " 为空，抛出异常");
            throw new IllegalArgumentException(errorMsg);
        }
        return value;
    }

    // 辅助方法：解析日期
    private Date parseDate(String timeStr) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
        return sdf.parse(timeStr);
    }

    @RequestMapping("/toClaimPage")
    public ModelAndView toClaimPage(HttpServletRequest request) {
        ModelAndView mv = new ModelAndView("claimPage");
        try {
            // 获取失物ID
            String lostThingIdStr = request.getParameter("id");
            if (lostThingIdStr == null || lostThingIdStr.trim().isEmpty()) {
                throw new IllegalArgumentException("失物ID不能为空");
            }
            int lostThingId = Integer.parseInt(lostThingIdStr);

            // 获取失物信息
            LostTing lostTing = lostTingService.getById(lostThingId);
            if (lostTing == null) {
                throw new IllegalArgumentException("未找到该失物");
            }

            // 检查失物状态是否为可认领
            if (!"丢失".equals(lostTing.getStatus())) {
                throw new IllegalArgumentException("该失物不可认领");
            }

            // 获取用户信息
            HttpSession session = request.getSession(false);
            if (session != null) {
                User userInfo = (User) session.getAttribute(Constant.LOGIN_USER);
                if (userInfo != null) {
                    mv.addObject("user", userInfo);
                }
            }

            mv.addObject("lostTing", lostTing);
        } catch (IllegalArgumentException e) {
            mv.addObject("success", false);
            mv.addObject("message", e.getMessage());
        } catch (Exception e) {
            mv.addObject("success", false);
            mv.addObject("message", "系统错误: " + e.getMessage());
            e.printStackTrace();
        }
        return mv;
    }
}
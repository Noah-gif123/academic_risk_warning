package cn.edu.neusoft.mall.service;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/user/logout.action")
public class LogoutServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // 销毁当前会话（清除登录状态）
        request.getSession().invalidate();
        // 重定向到首页或登录页
        response.sendRedirect(request.getContextPath() + "/index.action");
    }
}

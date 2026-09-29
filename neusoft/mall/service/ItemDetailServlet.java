package cn.edu.neusoft.mall.service;

import cn.edu.neusoft.mall.entiy.LostTing;
import cn.edu.neusoft.mall.service.impl.LostTingServiceImpl;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Logger;

@WebServlet("/item/detail")
public class ItemDetailServlet extends HttpServlet {
    private static final Logger logger = Logger.getLogger(ItemDetailServlet.class.getName());
    private LostTingService itemService = new LostTingServiceImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String id = request.getParameter("id");
        if (id == null || id.isEmpty()) {
            logger.warning("请求参数ID为空，重定向到物品列表");
            response.sendRedirect(request.getContextPath() + "/items");
            return;
        }

        try {
            int itemId = Integer.parseInt(id);
            LostTing item = itemService.getById(itemId);
            if (item == null) {
                logger.warning("未找到ID为 " + itemId + " 的物品，重定向到物品列表");
                response.sendRedirect(request.getContextPath() + "/items");
                return;
            }

            request.setAttribute("item", item);
            request.getRequestDispatcher("/WEB-INF/views/item-detail.jsp").forward(request, response);
        } catch (NumberFormatException e) {
            logger.warning("ID格式错误: " + id);
            response.sendRedirect(request.getContextPath() + "/items");
        } catch (Exception e) {
            logger.severe("获取物品详情异常: " + e.getMessage());
            request.setAttribute("errorMessage", "获取物品详情失败，请稍后再试");
            request.getRequestDispatcher("/error.jsp").forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }
}
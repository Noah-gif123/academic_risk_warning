// src/main/java/com/example/redchina/service/ShareService.java
package com.example.redchina.service;

import com.example.redchina.entity.SysStory;
import com.example.redchina.util.MessageUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;


import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

@Service
public class ShareService {

    @Resource
    private SysStoryService storyService;
    @Resource
    private MessageUtil messageUtil; // 多语言工具类

    // 生成剧情分享图片
    public String generateStoryShareImage(Long userId, Long storyId, String language) throws IOException {
        SysStory story = storyService.getById(storyId);
        if (story == null) {
            throw new RuntimeException("剧情不存在");
        }

        // 创建图片（简化示例）
        BufferedImage image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 800, 600);

        // 绘制标题（多语言）
        g.setColor(Color.BLACK);
        g.setFont(new Font("SimHei", Font.BOLD, 24));
        g.drawString(story.getStoryTitle(), 50, 50);
        g.drawString(messageUtil.getMessage("story.intro", new String[]{language}) + story.getStoryIntro(), 50, 100);

        // 保存图片
        String path = "upload/share/" + userId + "_" + storyId + ".png";
        File file = new File(path);
        ImageIO.write(image, "png", file);
        return path;
    }
}

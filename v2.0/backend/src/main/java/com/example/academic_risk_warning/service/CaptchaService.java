package com.example.academic_risk_warning.service;

import com.example.academic_risk_warning.common.BizException;
import com.example.academic_risk_warning.vo.CaptchaVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录图形验证码：JDK 自带的 AWT 画图（离线可用，不依赖第三方库）。
 * 规则：4 位字符（去掉容易混淆的 0/O/1/I）、5 分钟有效、**校验一次即失效**（防重放）。
 */
@Slf4j
@Service
public class CaptchaService {

    private static final String CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final int WIDTH = 132;
    private static final int HEIGHT = 44;
    private static final int CODE_LEN = 4;
    private static final long TTL_MILLIS = 5 * 60 * 1000L;

    private final SecureRandom random = new SecureRandom();
    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    /** 是否启用验证码（演示或联调可用 app.auth.captcha-enabled=false 关闭） */
    @Value("${app.auth.captcha-enabled:true}")
    private boolean enabled;

    private static class Entry {
        String code;
        long expireAt;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public CaptchaVO generate() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < CODE_LEN; i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        String code = sb.toString();
        String captchaId = UUID.randomUUID().toString().replace("-", "");
        Entry e = new Entry();
        e.code = code;
        e.expireAt = System.currentTimeMillis() + TTL_MILLIS;
        store.put(captchaId, e);
        cleanup();
        return CaptchaVO.builder()
                .captchaId(captchaId)
                .image("data:image/png;base64," + Base64.getEncoder().encodeToString(render(code)))
                .expireSeconds((int) (TTL_MILLIS / 1000))
                .build();
    }

    /** 校验（一次性）：不通过直接抛业务异常 */
    public void verify(String captchaId, String captchaCode) {
        if (!enabled) {
            return;
        }
        if (captchaId == null || captchaId.isBlank() || captchaCode == null || captchaCode.isBlank()) {
            throw new BizException(400, "请输入验证码");
        }
        Entry e = store.remove(captchaId);
        if (e == null) {
            throw new BizException(400, "验证码已失效，请点击图片刷新");
        }
        if (e.expireAt < System.currentTimeMillis()) {
            throw new BizException(400, "验证码已过期，请点击图片刷新");
        }
        if (!e.code.equalsIgnoreCase(captchaCode.trim())) {
            throw new BizException(400, "验证码错误，请重新输入");
        }
    }

    /** 调试用（默认关闭）：取回某次验证码的明文，供自动化脚本联调 */
    public String peek(String captchaId) {
        Entry e = store.get(captchaId);
        return e == null || e.expireAt < System.currentTimeMillis() ? null : e.code;
    }

    // ------------------------------------------------------------ 画图
    private byte[] render(String code) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // 背景
            g.setColor(new Color(245, 248, 252));
            g.fillRect(0, 0, WIDTH, HEIGHT);
            // 干扰线
            g.setStroke(new BasicStroke(1.2f));
            for (int i = 0; i < 5; i++) {
                g.setColor(randomColor(150, 210));
                g.drawLine(random.nextInt(WIDTH), random.nextInt(HEIGHT),
                        random.nextInt(WIDTH), random.nextInt(HEIGHT));
            }
            // 噪点
            for (int i = 0; i < 40; i++) {
                g.setColor(randomColor(150, 220));
                g.fillOval(random.nextInt(WIDTH), random.nextInt(HEIGHT), 2, 2);
            }
            // 字符（逐个旋转，防止简单 OCR）
            int step = (WIDTH - 20) / CODE_LEN;
            for (int i = 0; i < code.length(); i++) {
                int fontSize = 24 + random.nextInt(7);
                g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, fontSize));
                g.setColor(randomColor(20, 120));
                double angle = (random.nextDouble() - 0.5) * 0.6;
                int x = 12 + i * step;
                int y = HEIGHT - 12 - random.nextInt(6);
                g.rotate(angle, x + fontSize / 2.0, y - fontSize / 2.0);
                g.drawString(String.valueOf(code.charAt(i)), x, y);
                g.rotate(-angle, x + fontSize / 2.0, y - fontSize / 2.0);
            }
        } finally {
            g.dispose();
        }
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            log.error("验证码图片生成失败", e);
            throw new BizException("验证码生成失败，请重试");
        }
    }

    private Color randomColor(int min, int max) {
        int range = Math.max(1, max - min);
        return new Color(min + random.nextInt(range), min + random.nextInt(range), min + random.nextInt(range));
    }

    private void cleanup() {
        long now = System.currentTimeMillis();
        store.entrySet().removeIf(e -> e.getValue().expireAt < now);
    }
}

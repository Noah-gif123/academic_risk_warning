package com.example.academic_risk_warning.service.ai;

import com.example.academic_risk_warning.common.BizException;
import com.example.academic_risk_warning.vo.ai.StudentReportVO;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 把学生课程报告渲染成真实 PDF。
 *
 * <p>优先使用本机 Chrome headless；没有 Chrome 时明确报错，不静默退回打印。
 * 通过 REPORT_CHROME_PATH 可覆盖浏览器路径。</p>
 */
@Service
public class ReportPdfService {

    public byte[] render(StudentReportVO r) {
        Path html = null;
        Path pdf = null;
        try {
            html = Files.createTempFile("dsh-report-", ".html");
            pdf = Files.createTempFile("dsh-report-", ".pdf");
            Files.writeString(html, html(r), StandardCharsets.UTF_8);
            Process p = new ProcessBuilder(chrome(), "--headless=new", "--disable-gpu", "--no-sandbox",
                    "--no-pdf-header-footer", "--print-to-pdf=" + pdf, html.toUri().toString())
                    .redirectErrorStream(true).start();
            if (!p.waitFor(30, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                throw new BizException(500, "PDF 渲染超时");
            }
            if (p.exitValue() != 0 || !Files.exists(pdf) || Files.size(pdf) == 0) {
                throw new BizException(500, "PDF 渲染失败");
            }
            return Files.readAllBytes(pdf);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(500, "PDF 生成失败：" + e.getMessage());
        } finally {
            try { if (html != null) Files.deleteIfExists(html); } catch (Exception ignored) { }
            try { if (pdf != null) Files.deleteIfExists(pdf); } catch (Exception ignored) { }
        }
    }

    private String chrome() {
        String env = System.getenv("REPORT_CHROME_PATH");
        if (env != null && !env.isBlank() && new File(env).exists()) return env;
        String[] candidates = {
                "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe",
                "C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe",
                "/usr/bin/google-chrome", "/usr/bin/chromium", "/usr/bin/chromium-browser"
        };
        for (String c : candidates) {
            if (new File(c).exists()) return c;
        }
        throw new BizException(500, "未找到 Chrome；请设置 REPORT_CHROME_PATH");
    }

    private String html(StudentReportVO r) {
        StringBuilder b = new StringBuilder();
        b.append("<!doctype html><html lang='zh-CN'><head><meta charset='utf-8'><style>")
                .append("body{font-family:'Microsoft YaHei','PingFang SC',sans-serif;color:#1f2937;margin:36px;font-size:12px;line-height:1.7}")
                .append("h1{font-size:24px;margin:0 0 6px;color:#17324b}.meta{color:#6b7c8f;font-size:11px;margin-bottom:18px}")
                .append(".hero{border:1px solid #dbe7f1;border-radius:10px;padding:14px;background:#f8fbfe;margin:12px 0}")
                .append(".hero b{font-size:22px;color:#c43b3b}.tag{display:inline-block;background:#fbe8e8;color:#c43b3b;border-radius:99px;padding:2px 8px;font-size:10px}")
                .append("h2{font-size:15px;border-bottom:1px solid #e7eef5;padding-bottom:5px;margin:18px 0 8px;color:#111827}")
                .append("table{width:100%;border-collapse:collapse}td,th{border:1px solid #e7eef5;padding:6px 8px;font-size:11px}th{background:#f7fafd;text-align:left}")
                .append(".dim{display:grid;grid-template-columns:80px 1fr 40px;gap:6px;align-items:center;margin:5px 0;font-size:10px}")
                .append(".track{height:6px;background:#edf3f8;border-radius:99px;overflow:hidden}.s{height:100%;background:#0f9d8a}.c{height:100%;background:#b9cad9}")
                .append(".sec{white-space:pre-line;margin:5px 0}.foot{margin-top:22px;border-top:1px dashed #dbe6f0;padding-top:8px;font-size:9px;color:#9aacbc}")
                .append("</style></head><body>");
        b.append("<h1>").append(esc(r.getCourseName())).append(" · 课程学情报告</h1>");
        b.append("<div class='meta'>").append(esc(r.getClassName()))
                .append(" · ").append(esc(r.getTermName())).append(" · 报告 #").append(r.getId()).append("</div>");
        b.append("<div class='hero'><b>").append(r.getTotalScore() == null ? "—" : r.getTotalScore()).append("</b> 分")
                .append(" <span class='tag'>").append(esc(r.getRiskLevel())).append("</span><br>")
                .append(esc(r.getSummary())).append("</div>");
        renderMetrics(b, r.getMetrics());
        b.append("<h2>报告正文</h2>");
        for (StudentReportVO.Section s : r.getSections()) {
            b.append("<h3>").append(esc(s.getTitle())).append("</h3><div class='sec'>").append(esc(s.getText())).append("</div>");
        }
        b.append("<div class='foot'>由 v3.0 学情预警原型生成 · 数据来自 risk_state / 成绩 / 考勤 · 报告 #")
                .append(r.getId()).append("</div></body></html>");
        return b.toString();
    }

    @SuppressWarnings("unchecked")
    private void renderMetrics(StringBuilder b, Map<String, Object> metrics) {
        if (metrics == null || !(metrics.get("dimensions") instanceof List<?> list) || list.isEmpty()) return;
        b.append("<h2>六维画像</h2>");
        for (Object o : list) {
            if (!(o instanceof Map<?, ?> m)) continue;
            Object student = m.get("student"), avg = m.get("classAvg");
            double sv = student instanceof Number n ? n.doubleValue() : 0;
            double cv = avg instanceof Number n ? n.doubleValue() : 0;
            b.append("<div class='dim'><span>").append(esc(String.valueOf(m.get("label")))).append("</span>")
                    .append("<div class='track'><div class='s' style='width:").append(Math.min(100, Math.max(0, sv))).append("%'></div>")
                    .append("<div class='c' style='width:").append(Math.min(100, Math.max(0, cv))).append("%'></div></div>")
                    .append("<b>").append(student == null ? "—" : student).append("</b></div>");
        }
    }

    private String esc(String s) {
        if (s == null) return "—";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}

package com.example.academic_risk_warning.service.ai;

import com.example.academic_risk_warning.common.BizException;
import com.example.academic_risk_warning.vo.ai.StudentReportVO;
import com.lowagie.text.*;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

/**
 * 学生课程报告 PDF。使用 OpenPDF 在 JVM 内生成，不启动浏览器子进程。
 */
@Service
public class ReportPdfService {

    public byte[] renderGroup(com.example.academic_risk_warning.vo.ai.GroupReportVO r) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 36, 36, 42, 42);
            PdfWriter.getInstance(doc, out);
            doc.open();
            BaseFont bf = font();
            Font title = new Font(bf, 20, Font.BOLD, new Color(23, 50, 75));
            Font h2 = new Font(bf, 13, Font.BOLD, new Color(17, 24, 39));
            Font body = new Font(bf, 10, Font.NORMAL, new Color(45, 60, 75));
            Font small = new Font(bf, 8, Font.NORMAL, new Color(120, 135, 150));
            doc.add(new Paragraph(nz(r.getTitle()) + " · 学情报告", title));
            doc.add(new Paragraph("报告 #" + r.getId() + " · " + nz(r.getCreateTime()) + " · " + nz(r.getSource()), small));
            doc.add(Chunk.NEWLINE);
            doc.add(new Paragraph(nz(r.getSummary()), body));
            doc.add(new Paragraph("一、报告正文", h2));
            if (r.getSections() != null) {
                for (var s : r.getSections()) {
                    doc.add(new Paragraph(nz(s.getTitle()), new Font(bf, 11, Font.BOLD, new Color(30, 50, 70))));
                    Paragraph p = new Paragraph(nz(s.getText()), body);
                    p.setLeading(15);
                    doc.add(p);
                }
            }
            if (r.getRunSteps() != null && !r.getRunSteps().isEmpty()) {
                doc.add(new Paragraph("二、生成链路", h2));
                for (var s : r.getRunSteps()) {
                    doc.add(new Paragraph(s.getStepNo() + ". " + nz(s.getName()) + " · "
                            + nz(s.getValidationStatus()) + " · " + (s.getDurationMs() == null ? "—" : s.getDurationMs() + "ms"), small));
                }
            }
            doc.add(Chunk.NEWLINE);
            doc.add(new Paragraph("由 v3.0 学情预警系统生成 · 数字可回溯到判定与成绩数据", small));
            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new BizException(500, "群体报告 PDF 生成失败：" + e.getMessage());
        }
    }

    public byte[] render(StudentReportVO r) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 36, 36, 42, 42);
            PdfWriter.getInstance(doc, out);
            doc.open();

            BaseFont bf = font();
            Font title = new Font(bf, 20, Font.BOLD, new Color(23, 50, 75));
            Font h2 = new Font(bf, 13, Font.BOLD, new Color(17, 24, 39));
            Font body = new Font(bf, 10, Font.NORMAL, new Color(45, 60, 75));
            Font small = new Font(bf, 8, Font.NORMAL, new Color(120, 135, 150));

            doc.add(new Paragraph(nz(r.getCourseName()) + " · 课程学情报告", title));
            doc.add(new Paragraph("学生报告 #" + r.getId() + " · " + nz(r.getTermName())
                    + " · " + nz(r.getClassName()), small));
            doc.add(Chunk.NEWLINE);

            Paragraph summary = new Paragraph(nz(r.getSummary()), body);
            summary.setLeading(16);
            doc.add(summary);
            doc.add(Chunk.NEWLINE);

            doc.add(new Paragraph("一、核心指标", h2));
            PdfPTable kpi = new PdfPTable(new float[]{2, 1, 1});
            kpi.setWidthPercentage(100);
            cell(kpi, "总评成绩", body);
            cell(kpi, r.getTotalScore() == null ? "—" : String.valueOf(r.getTotalScore()), body);
            cell(kpi, nz(r.getRiskLevel()), body);
            doc.add(kpi);
            doc.add(Chunk.NEWLINE);

            doc.add(new Paragraph("二、六维画像", h2));
            renderMetrics(doc, r.getMetrics(), bf, body, small);
            doc.add(Chunk.NEWLINE);

            doc.add(new Paragraph("三、报告正文", h2));
            List<StudentReportVO.Section> sections = r.getSections();
            if (sections != null) {
                for (StudentReportVO.Section s : sections) {
                    Paragraph t = new Paragraph(nz(s.getTitle()), new Font(bf, 11, Font.BOLD, new Color(30, 50, 70)));
                    t.setSpacingBefore(6);
                    doc.add(t);
                    Paragraph p = new Paragraph(nz(s.getText()), body);
                    p.setLeading(15);
                    doc.add(p);
                }
            }

            doc.add(Chunk.NEWLINE);
            doc.add(new Paragraph("由 v3.0 学情预警原型生成 · 数据来自 risk_state / 成绩 / 考勤 · 报告 #"
                    + r.getId(), small));
            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new BizException(500, "PDF 生成失败：" + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void renderMetrics(Document doc, Map<String, Object> metrics, BaseFont bf, Font body, Font small) throws DocumentException {
        if (metrics == null || !(metrics.get("dimensions") instanceof List<?> list) || list.isEmpty()) {
            doc.add(new Paragraph("暂无六维指标", small));
            return;
        }
        PdfPTable table = new PdfPTable(new float[]{1.6f, 1f, 1f});
        table.setWidthPercentage(100);
        cell(table, "维度", body);
        cell(table, "学生", body);
        cell(table, "班级均值", body);
        for (Object o : list) {
            if (!(o instanceof Map<?, ?> m)) continue;
            cell(table, String.valueOf(m.get("label")), body);
            cell(table, m.get("student") == null ? "—" : String.valueOf(m.get("student")), body);
            cell(table, m.get("classAvg") == null ? "—" : String.valueOf(m.get("classAvg")), body);
        }
        doc.add(table);
    }

    private void cell(PdfPTable table, String text, Font font) {
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setPadding(6);
        c.setBorderColor(new Color(225, 235, 243));
        table.addCell(c);
    }

    private BaseFont font() {
        try {
            return BaseFont.createFont("C:/Windows/Fonts/msyh.ttc,0", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        } catch (Exception ignore) {
            try {
                return BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
            } catch (Exception e) {
                return null;
            }
        }
    }

    private String nz(String s) {
        return s == null ? "—" : s;
    }
}

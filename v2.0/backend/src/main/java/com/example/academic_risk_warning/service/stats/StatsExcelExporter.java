package com.example.academic_risk_warning.service.stats;

import com.example.academic_risk_warning.vo.stats.StatsEnvelope;
import com.example.academic_risk_warning.vo.stats.StatsVos;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;

/**
 * P1-7 统计 Excel 导出（POI）。
 *
 * <p>与 CSV 同一数据源：Controller 先调 Service 得到 StatsEnvelope，再交给本类写工作簿，
 * 保证导出数字与页面/接口一致。</p>
 */
@Component
public class StatsExcelExporter {

    public byte[] overview(StatsEnvelope<StatsVos.Overview> envelope) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sh = wb.createSheet("总览");
            StatsVos.Overview o = envelope.getCurrent();
            int r = 0;
            r = meta(wb, sh, r, envelope);
            r = head(sh, r, "指标", "数值");
            r = row(sh, r, "学期", o.getTermName());
            r = row(sh, r, "风险学生", o.getTotal());
            r = row(sh, r, "红", o.getRed());
            r = row(sh, r, "橙", o.getOrange());
            r = row(sh, r, "黄", o.getYellow());
            r = row(sh, r, "绿", o.getGreen());
            r = row(sh, r, "红黄占比(%)", o.getRiskRatio());
            r = row(sh, r, "平均风险分", o.getAvgRiskScore());
            r = row(sh, r, "预警关闭率(%)", o.getCloseRate());
            r = row(sh, r, "平均处置时长(h)", o.getAvgHandleHours());
            r = row(sh, r, "周期新增预警", o.getAlertNew());
            r = row(sh, r, "周期关闭预警", o.getAlertClosed());
            r = row(sh, r, "周期干预", o.getInterventions());
            r = row(sh, r, "周期改善", o.getImproved());
            return bytes(wb);
        } catch (Exception e) {
            throw new IllegalStateException("导出总览 Excel 失败", e);
        }
    }

    public byte[] distribution(StatsEnvelope<StatsVos.Distribution> envelope) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sh = wb.createSheet("风险分布");
            StatsVos.Distribution d = envelope.getCurrent();
            int r = meta(wb, sh, 0, envelope);
            r = head(sh, r, "等级", "数量");
            for (var e : d.getLevels().entrySet()) {
                r = row(sh, r, e.getKey(), e.getValue());
            }
            r++;
            r = head(sh, r, "课程ID", "课程", "红", "橙", "黄", "绿", "合计");
            for (StatsVos.DistRow c : d.getByCourse()) {
                r = row(sh, r, c.getCode(), c.getName(), c.getRed(), c.getOrange(), c.getYellow(), c.getGreen(), c.getTotal());
            }
            r++;
            r = head(sh, r, "课程ID", "课程", "挂科率(%)", "课程因子", "已初始化");
            for (StatsVos.Calibration c : d.getCalibration()) {
                r = row(sh, r, c.getCourseId(), c.getCourseName(), c.getFailRate(), c.getCourseFactor(),
                        Boolean.TRUE.equals(c.getInitialized()) ? "是" : "否");
            }
            return bytes(wb);
        } catch (Exception e) {
            throw new IllegalStateException("导出分布 Excel 失败", e);
        }
    }

    public byte[] compare(StatsEnvelope<StatsVos.Compare> envelope) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sh = wb.createSheet("班级对比");
            int r = meta(wb, sh, 0, envelope);
            r = head(sh, r, "教学班", "班级", "课程", "人数", "红", "橙", "黄", "绿",
                    "平均风险分", "平均分", "缺课硬线");
            for (StatsVos.CompareRow c : envelope.getCurrent().getRows()) {
                r = row(sh, r, c.getClassCode(), c.getClassName(), c.getCourseName(), c.getStudentCount(),
                        c.getRed(), c.getOrange(), c.getYellow(), c.getGreen(), c.getAvgRiskScore(),
                        c.getAvgScore(), c.getHardFail());
            }
            return bytes(wb);
        } catch (Exception e) {
            throw new IllegalStateException("导出班级对比 Excel 失败", e);
        }
    }

    public byte[] funnel(StatsEnvelope<StatsVos.Funnel> envelope) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sh = wb.createSheet("成效漏斗");
            int r = meta(wb, sh, 0, envelope);
            r = head(sh, r, "阶段", "数量");
            for (StatsVos.FunnelStage f : envelope.getCurrent().getStages()) {
                r = row(sh, r, f.getName(), String.valueOf(f.getValue()));
            }
            return bytes(wb);
        } catch (Exception e) {
            throw new IllegalStateException("导出漏斗 Excel 失败", e);
        }
    }

    public byte[] effect(StatsEnvelope<StatsVos.Effect> envelope) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sh = wb.createSheet("干预成效");
            int r = meta(wb, sh, 0, envelope);
            r = head(sh, r, "组别", "干预数", "到期复评", "改善", "持平", "恶化", "改善率%");
            for (StatsVos.Cohort c : envelope.getCurrent().getCohorts()) {
                r = row(sh, r, c.getCohort(), String.valueOf(c.getIntervened()), String.valueOf(c.getDue()),
                        String.valueOf(c.getImproved()), String.valueOf(c.getUnchanged()),
                        String.valueOf(c.getWorsened()), String.valueOf(c.getImproveRate()));
            }
            StatsVos.Effect v = envelope.getCurrent();
            r = row(sh, r, "TREAT−CONTROL(pp)", String.valueOf(v.getDiffPp()));
            return bytes(wb);
        } catch (Exception e) {
            throw new IllegalStateException("导出成效 Excel 失败", e);
        }
    }

    // ------------------------------------------------------------ 单元格工具
    private int meta(XSSFWorkbook wb, Sheet sh, int r, StatsEnvelope<?> e) {
        r = row(sh, r, "数据状态", e.getDataStatus());
        r = row(sh, r, "说明", e.getReason());
        r = row(sh, r, "生成时间", e.getAsOf());
        r = row(sh, r, "统计区间", e.getPeriodStart() + " ~ " + e.getPeriodEnd());
        r = row(sh, r, "引擎版本", e.getEngineVersion());
        return r + 1;
    }

    private int head(Sheet sh, int r, String... values) {
        Row row = sh.createRow(r);
        CellStyle bold = sh.getWorkbook().createCellStyle();
        Font f = sh.getWorkbook().createFont();
        f.setBold(true);
        bold.setFont(f);
        for (int i = 0; i < values.length; i++) {
            Cell c = row.createCell(i);
            c.setCellValue(values[i]);
            c.setCellStyle(bold);
        }
        return r + 1;
    }

    private int row(Sheet sh, int r, String... values) {
        Row row = sh.createRow(r);
        for (int i = 0; i < values.length; i++) {
            row.createCell(i).setCellValue(values[i] == null ? "" : values[i]);
        }
        return r + 1;
    }

    private int row(Sheet sh, int r, Object... values) {
        Row row = sh.createRow(r);
        for (int i = 0; i < values.length; i++) {
            Cell c = row.createCell(i);
            Object v = values[i];
            if (v == null) {
                c.setBlank();
            } else if (v instanceof Number) {
                c.setCellValue(((Number) v).doubleValue());
            } else {
                c.setCellValue(String.valueOf(v));
            }
        }
        return r + 1;
    }

    private byte[] bytes(XSSFWorkbook wb) throws Exception {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            wb.write(out);
            return out.toByteArray();
        }
    }

    /** 保持与 CSV 相同的小数位（供自定义汇总行使用）。 */
    static BigDecimal scale(BigDecimal v) {
        return v == null ? null : v.setScale(2, java.math.RoundingMode.HALF_UP);
    }
}

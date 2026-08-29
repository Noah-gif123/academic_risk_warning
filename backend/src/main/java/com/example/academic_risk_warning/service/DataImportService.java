package com.example.academic_risk_warning.service;

import com.example.academic_risk_warning.entity.Attendance;
import com.example.academic_risk_warning.entity.HomeworkInfo;
import com.example.academic_risk_warning.entity.ScoreInfo;
import com.example.academic_risk_warning.mapper.AttendanceMapper;
import com.example.academic_risk_warning.mapper.HomeworkInfoMapper;
import com.example.academic_risk_warning.mapper.ScoreInfoMapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 批量数据导入服务：Excel模板解析与导入
 */
@Service
public class DataImportService {

    private static final Logger log = LoggerFactory.getLogger(DataImportService.class);

    private final ScoreInfoMapper scoreInfoMapper;
    private final HomeworkInfoMapper homeworkInfoMapper;
    private final AttendanceMapper attendanceMapper;

    public DataImportService(ScoreInfoMapper scoreInfoMapper,
                             HomeworkInfoMapper homeworkInfoMapper,
                             AttendanceMapper attendanceMapper) {
        this.scoreInfoMapper = scoreInfoMapper;
        this.homeworkInfoMapper = homeworkInfoMapper;
        this.attendanceMapper = attendanceMapper;
    }

    /**
     * 导入 Excel 文件
     * @return 导入统计结果
     */
    public ImportResult importExcel(MultipartFile file) throws IOException {
        ImportResult result = new ImportResult();
        try (InputStream is = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(is)) {

            // Sheet 0: 学生成绩
            Sheet scoreSheet = workbook.getSheetAt(0);
            if (scoreSheet != null) {
                result.scoreCount = importScores(scoreSheet);
            }

            // Sheet 1: 作业信息
            if (workbook.getNumberOfSheets() > 1) {
                Sheet hwSheet = workbook.getSheetAt(1);
                if (hwSheet != null) {
                    result.homeworkCount = importHomeworks(hwSheet);
                }
            }

            // Sheet 2: 出勤信息
            if (workbook.getNumberOfSheets() > 2) {
                Sheet attSheet = workbook.getSheetAt(2);
                if (attSheet != null) {
                    result.attendanceCount = importAttendances(attSheet);
                }
            }
        }
        return result;
    }

    private int importScores(Sheet sheet) {
        List<ScoreInfo> list = new ArrayList<>();
        for (int i = 1; i <= sheet.getLastRowNum(); i++) { // skip header
            Row row = sheet.getRow(i);
            if (row == null || isEmptyRow(row)) continue;
            try {
                ScoreInfo s = new ScoreInfo();
                s.setStudentId(getLong(row, 0));
                s.setCourseId(getLong(row, 1));
                s.setUsualScore(getInt(row, 2));
                s.setMidScore(getInt(row, 3));
                s.setFinalScore(getInt(row, 4));
                list.add(s);
            } catch (Exception e) {
                log.warn("[DataImport] 成绩行{}解析失败: {}", i, e.getMessage());
            }
        }
        for (ScoreInfo s : list) {
            scoreInfoMapper.insert(s);
        }
        return list.size();
    }

    private int importHomeworks(Sheet sheet) {
        List<HomeworkInfo> list = new ArrayList<>();
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null || isEmptyRow(row)) continue;
            try {
                HomeworkInfo h = new HomeworkInfo();
                h.setStudentId(getLong(row, 0));
                h.setCourseId(getLong(row, 1));
                h.setTotalHomework(getInt(row, 2));
                h.setSubmitCount(getInt(row, 3));
                h.setNotSubmitCount(getInt(row, 4));
                h.setLateSubmitCount(getInt(row, 5));
                h.setAvgScore(getInt(row, 6));
                list.add(h);
            } catch (Exception e) {
                log.warn("[DataImport] 作业行{}解析失败: {}", i, e.getMessage());
            }
        }
        for (HomeworkInfo h : list) {
            homeworkInfoMapper.insert(h);
        }
        return list.size();
    }

    private int importAttendances(Sheet sheet) {
        List<Attendance> list = new ArrayList<>();
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null || isEmptyRow(row)) continue;
            try {
                Attendance a = new Attendance();
                a.setStudentId(getLong(row, 0));
                a.setCourseId(getLong(row, 1));
                a.setTotalHours(getInt(row, 2));
                a.setAbsentCount(getInt(row, 3));
                a.setLateCount(getInt(row, 4));
                a.setAttendanceRate(getDecimal(row, 5));
                list.add(a);
            } catch (Exception e) {
                log.warn("[DataImport] 出勤行{}解析失败: {}", i, e.getMessage());
            }
        }
        for (Attendance a : list) {
            attendanceMapper.insert(a);
        }
        return list.size();
    }

    private Long getLong(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case NUMERIC -> (long) cell.getNumericCellValue();
            case STRING -> {
                try { yield Long.parseLong(cell.getStringCellValue().trim()); }
                catch (NumberFormatException e) { yield null; }
            }
            default -> null;
        };
    }

    private Integer getInt(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case NUMERIC -> (int) cell.getNumericCellValue();
            case STRING -> {
                try { yield Integer.parseInt(cell.getStringCellValue().trim()); }
                catch (NumberFormatException e) { yield null; }
            }
            default -> null;
        };
    }

    private BigDecimal getDecimal(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case NUMERIC -> BigDecimal.valueOf(cell.getNumericCellValue());
            case STRING -> {
                try { yield new BigDecimal(cell.getStringCellValue().trim()); }
                catch (NumberFormatException e) { yield null; }
            }
            default -> null;
        };
    }

    private boolean isEmptyRow(Row row) {
        for (int c = 0; c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK) return false;
        }
        return true;
    }

    /**
     * 生成空模板 Excel
     */
    public byte[] generateTemplate() throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            CellStyle headerStyle = createHeaderStyle(wb);

            // Sheet 0: 学生成绩
            Sheet s0 = wb.createSheet("学生成绩");
            createHeaderRow(s0, headerStyle, "学生ID", "课程ID", "平时成绩", "期中成绩", "期末成绩");

            // Sheet 1: 作业信息
            Sheet s1 = wb.createSheet("作业信息");
            createHeaderRow(s1, headerStyle, "学生ID", "课程ID", "作业总数", "已提交数", "未提交数", "迟交数", "平均分");

            // Sheet 2: 出勤信息
            Sheet s2 = wb.createSheet("出勤信息");
            createHeaderRow(s2, headerStyle, "学生ID", "课程ID", "总课时", "缺勤次数", "迟到次数", "出勤率(%)");

            // 自动调整列宽
            for (int i = 0; i < 3; i++) {
                Sheet s = wb.getSheetAt(i);
                for (int c = 0; c < s.getRow(0).getLastCellNum(); c++) {
                    s.autoSizeColumn(c);
                }
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            wb.write(bos);
            return bos.toByteArray();
        }
    }

    private void createHeaderRow(Sheet sheet, CellStyle style, String... headers) {
        Row row = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(style);
        }
    }

    private CellStyle createHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        return style;
    }

    public static class ImportResult {
        public int scoreCount;
        public int homeworkCount;
        public int attendanceCount;

        public int total() { return scoreCount + homeworkCount + attendanceCount; }
    }
}

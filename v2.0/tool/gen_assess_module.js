/* gen_assess_module.js — 第一阶段 1-7 测验与作业（导入校对 / 发布 / 在线作答 / 自动判分 / 主观题批阅 / 缺考记 0） */
'use strict';
const fs = require('fs');
const path = require('path');

const BE = 'E:/JavaDEVIDEA/workspace/JavaProject/academic_risk_warning/v2.0/backend';
const J = path.join(BE, 'src/main/java/com/example/academic_risk_warning');
const P = 'com.example.academic_risk_warning';

function w(rel, content) {
  const p = path.join(J, rel);
  fs.mkdirSync(path.dirname(p), { recursive: true });
  fs.writeFileSync(p, content.replace(/^\n/, ''), 'utf8');
}

// ================================================================ util
w('util/MiniJson.java', `
package ${P}.util;

import java.util.ArrayList;
import java.util.List;

/**
 * 极简 JSON 工具：只处理本项目需要的几种固定结构（答案 / 选项），
 * 避免引入对象映射带来的迁移成本。写：quote/array；读：arrayStrings/boolValue。
 */
public final class MiniJson {

    private MiniJson() {
    }

    public static String quote(String s) {
        if (s == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder("\\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\\\\""); break;
                case '\\\\': sb.append("\\\\\\\\"); break;
                case '\\n': sb.append("\\\\n"); break;
                case '\\r': sb.append("\\\\r"); break;
                case '\\t': sb.append("\\\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.append('"').toString();
    }

    /** 把若干字符串写成 JSON 数组 */
    public static String array(List<String> values) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(quote(values.get(i)));
        }
        return sb.append(']').toString();
    }

    /** 取 {"key":[...]} 里的字符串元素 */
    public static List<String> arrayStrings(String json, String key) {
        List<String> out = new ArrayList<>();
        int k = indexOfKey(json, key);
        if (k < 0) {
            return out;
        }
        int lb = json.indexOf('[', k);
        if (lb < 0) {
            return out;
        }
        int depth = 0;
        for (int i = lb; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '[') {
                depth++;
            } else if (c == ']') {
                depth--;
                if (depth == 0) {
                    break;
                }
            } else if (c == '"') {
                StringBuilder sb = new StringBuilder();
                int j = i + 1;
                while (j < json.length()) {
                    char ch = json.charAt(j);
                    if (ch == '\\\\') {
                        char nx = j + 1 < json.length() ? json.charAt(j + 1) : ' ';
                        if (nx == 'n') sb.append('\\n');
                        else if (nx == 'r') sb.append('\\r');
                        else if (nx == 't') sb.append('\\t');
                        else if (nx == 'u' && j + 5 < json.length()) {
                            sb.append((char) Integer.parseInt(json.substring(j + 2, j + 6), 16));
                            j += 4;
                        } else sb.append(nx);
                        j += 2;
                        continue;
                    }
                    if (ch == '"') {
                        break;
                    }
                    sb.append(ch);
                    j++;
                }
                out.add(sb.toString());
                i = j;
            }
        }
        return out;
    }

    /** 取 {"key":true} 的布尔值 */
    public static Boolean boolValue(String json, String key) {
        int k = indexOfKey(json, key);
        if (k < 0) {
            return null;
        }
        int colon = json.indexOf(':', k);
        if (colon < 0) {
            return null;
        }
        String rest = json.substring(colon + 1).trim();
        if (rest.startsWith("true")) {
            return Boolean.TRUE;
        }
        if (rest.startsWith("false")) {
            return Boolean.FALSE;
        }
        return null;
    }

    private static int indexOfKey(String json, String key) {
        if (json == null) {
            return -1;
        }
        String needle = '"' + key + '"';
        int i = json.indexOf(needle);
        while (i >= 0) {
            int j = i + needle.length();
            while (j < json.length() && Character.isWhitespace(json.charAt(j))) {
                j++;
            }
            if (j < json.length() && json.charAt(j) == ':') {
                return j;
            }
            i = json.indexOf(needle, i + 1);
        }
        return -1;
    }
}
`);

// ================================================================ dto
w('dto/QuestionDTO.java', `
package ${P}.dto;

import lombok.Data;

import java.math.BigDecimal;

/** 题目（教师保存 / 导入校对共用） */
@Data
public class QuestionDTO {
    private Long id;
    private Integer qNo;
    /** SINGLE / MULTI / JUDGE / BLANK / SHORT */
    private String qType;
    private String stem;
    private String optionsJson;
    private String answerJson;
    private String analysis;
    private BigDecimal fullScore;
    private String groupName;
    private Integer sortNo;
}
`);

w('dto/PaperSaveRequest.java', `
package ${P}.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 新建 / 保存试卷（草稿） */
@Data
public class PaperSaveRequest {
    private Long id;
    @NotNull(message = "教学班不能为空")
    private Long teachingClassId;
    /** QUIZ / HOMEWORK / EXAM */
    @NotBlank(message = "类型不能为空")
    private String paperType;
    @NotBlank(message = "名称不能为空")
    private String paperName;
    private Long lessonId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer durationMinutes;
    private BigDecimal fullScore;
    private Long schemeItemId;
    private Integer allowViewPaper;
    private Integer showScore;
    /** NEVER / AFTER_MARK */
    private String showAnswer;
    private String remark;
    private List<QuestionDTO> questions;
}
`);

w('dto/AnswerSubmitDTO.java', `
package ${P}.dto;

import lombok.Data;

/** 学生某题作答（answerJson 结构随题型而定） */
@Data
public class AnswerSubmitDTO {
    private Long questionId;
    private String answerJson;
}
`);

w('dto/PaperSubmitRequest.java', `
package ${P}.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/** 交卷：一次性提交全部作答（作业与测验不允许迟交、不允许教师代交） */
@Data
public class PaperSubmitRequest {
    private List<AnswerSubmitDTO> answers;
    /** 客户端记录的进入时间，用于算用时（仅参考，超时以服务端为准） */
    private LocalDateTime startedAt;
}
`);

w('dto/GradeItemDTO.java', `
package ${P}.dto;

import lombok.Data;

import java.math.BigDecimal;

/** 教师对某题的打分 */
@Data
public class GradeItemDTO {
    private Long answerId;
    private BigDecimal score;
    private String comment;
}
`);

w('dto/GradeRequest.java', `
package ${P}.dto;

import lombok.Data;

import java.util.List;

/** 提交主观题批改 */
@Data
public class GradeRequest {
    private List<GradeItemDTO> items;
    /** 整卷批注 */
    private String remark;
}
`);

// ================================================================ vo
w('vo/PaperVO.java', `
package ${P}.vo;

import ${P}.dto.QuestionDTO;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 试卷视图（教师端） */
@Data
public class PaperVO {
    private Long id;
    private Long teachingClassId;
    private String paperType;
    private String paperName;
    private Long lessonId;
    private Integer weekNo;
    private Integer sessionNo;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer durationMinutes;
    private BigDecimal fullScore;
    private BigDecimal questionScore;
    private Long schemeItemId;
    private String schemeItemName;
    private Integer allowViewPaper;
    private Integer showScore;
    private String showAnswer;
    private String status;
    private String remark;
    private Integer questionCount;
    private Integer subjectiveCount;
    private Integer submitCount;
    private Integer markedCount;
    private Integer absentCount;
    private Integer totalStudents;
    private List<QuestionDTO> questions;
}
`);

w('vo/StudentPaperVO.java', `
package ${P}.vo;

import ${P}.dto.QuestionDTO;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 学生端试卷（作答用，不含答案） */
@Data
public class StudentPaperVO {
    private Long id;
    private String paperName;
    private String paperType;
    private String courseName;
    private String teacherName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer durationMinutes;
    private BigDecimal fullScore;
    private Integer remainingSeconds;
    private String myStatus;
    private BigDecimal myScore;
    private Boolean canAnswer;
    private String reason;
    private List<QuestionDTO> questions;
}
`);

w('vo/SubmissionAnswerVO.java', `
package ${P}.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 单题作答与判分 */
@Data
public class SubmissionAnswerVO {
    private Long id;
    private Long questionId;
    private Integer qNo;
    private String qType;
    private String stem;
    private String optionsJson;
    private BigDecimal fullScore;
    private String answerJson;
    private String stdAnswerJson;
    private String analysis;
    private BigDecimal score;
    private Integer isCorrect;
    private String comment;
    private String markedByType;
    private Long markedBy;
    private String markedByName;
    private LocalDateTime markedAt;
}
`);

w('vo/SubmissionVO.java', `
package ${P}.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 提交记录（教师批阅 / 学生看结果） */
@Data
public class SubmissionVO {
    private Long id;
    private Long paperId;
    private String paperName;
    private String paperType;
    private Long studentId;
    private String studentNo;
    private String studentName;
    private String submitStatus;
    private String markedStatus;
    private LocalDateTime startTime;
    private LocalDateTime submitTime;
    private Integer durationSeconds;
    private BigDecimal objectiveScore;
    private BigDecimal subjectiveScore;
    private BigDecimal totalScore;
    private BigDecimal fullScore;
    private Integer pendingCount;
    private Integer isAbsent;
    private String remark;
    private String markedByName;
    private List<SubmissionAnswerVO> answers;
}
`);

w('vo/PaperImportVO.java', `
package ${P}.vo;

import ${P}.dto.QuestionDTO;
import lombok.Data;

import java.util.List;

/** 导入结果：题目先落成草稿供教师校对，不直接入库 */
@Data
public class PaperImportVO {
    private String fileName;
    private Integer parsedCount;
    private Integer warningCount;
    private List<String> warnings;
    private List<QuestionDTO> questions;
}
`);



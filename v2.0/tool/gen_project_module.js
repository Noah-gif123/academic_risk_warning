/* gen_project_module.js — 第一阶段 1-8 项目与贡献度（项目发布 / 组队 / 文件提交 / 教师评分 / 组长一份不可改贡献表） */
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

// ================================================================ util：扁平对象数组解析
w('util/JsonObjects.java', `
package ${P}.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 只解析「扁平对象数组」：如 [{"studentId":1,"ratio":0.3}]。
 * 项目/组队/贡献度的 JSON 都是这个形态，没必要引入完整解析器。
 */
public final class JsonObjects {

    private JsonObjects() {
    }

    public static List<Map<String, String>> flatObjects(String json, String arrayKey) {
        if (json == null) {
            return new ArrayList<>();
        }
        int k = indexOfKey(json, arrayKey);
        if (k < 0) {
            return new ArrayList<>();
        }
        int lb = json.indexOf('[', k);
        if (lb < 0) {
            return new ArrayList<>();
        }
        return parseFrom(json, lb);
    }

    /** 解析裸数组：[{"studentId":1,"ratio":0.3}] */
    public static List<Map<String, String>> arrayObjects(String json) {
        if (json == null) {
            return new ArrayList<>();
        }
        int lb = json.indexOf('[');
        if (lb < 0) {
            return new ArrayList<>();
        }
        return parseFrom(json, lb);
    }

    private static List<Map<String, String>> parseFrom(String json, int lb) {
        List<Map<String, String>> out = new ArrayList<>();
        int i = lb + 1;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '{') {
                Map<String, String> obj = new LinkedHashMap<>();
                int j = i + 1;
                while (j < json.length() && json.charAt(j) != '}') {
                    if (json.charAt(j) == '"') {
                        StringBuilder key = new StringBuilder();
                        j = readString(json, j, key);
                        int colon = json.indexOf(':', j);
                        if (colon < 0) {
                            break;
                        }
                        j = colon + 1;
                        while (j < json.length() && Character.isWhitespace(json.charAt(j))) {
                            j++;
                        }
                        String value;
                        if (j < json.length() && json.charAt(j) == '"') {
                            StringBuilder val = new StringBuilder();
                            j = readString(json, j, val);
                            value = val.toString();
                        } else {
                            int start = j;
                            while (j < json.length() && json.charAt(j) != ',' && json.charAt(j) != '}') {
                                j++;
                            }
                            value = json.substring(start, j).trim();
                        }
                        obj.put(key.toString(), value);
                    } else {
                        j++;
                    }
                }
                out.add(obj);
                i = j + 1;
            } else if (c == ']') {
                break;
            } else {
                i++;
            }
        }
        return out;
    }

    /** 读取一个带转义的字符串，返回结束后的位置 */
    private static int readString(String json, int quoteIndex, StringBuilder sb) {
        int j = quoteIndex + 1;
        while (j < json.length()) {
            char ch = json.charAt(j);
            if (ch == '\\\\') {
                char nx = j + 1 < json.length() ? json.charAt(j + 1) : ' ';
                if (nx == 'n') {
                    sb.append('\\n');
                } else if (nx == 'r') {
                    sb.append('\\r');
                } else if (nx == 't') {
                    sb.append('\\t');
                } else if (nx == 'u' && j + 5 < json.length()) {
                    sb.append((char) Integer.parseInt(json.substring(j + 2, j + 6), 16));
                    j += 4;
                } else {
                    sb.append(nx);
                }
                j += 2;
                continue;
            }
            if (ch == '"') {
                return j + 1;
            }
            sb.append(ch);
            j++;
        }
        return j;
    }

    private static int indexOfKey(String json, String key) {
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
w('dto/ProjectSaveRequest.java', `
package ${P}.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 新建 / 保存项目任务 */
@Data
public class ProjectSaveRequest {
    private Long id;
    @NotNull(message = "教学班不能为空")
    private Long teachingClassId;
    @NotBlank(message = "项目名称不能为空")
    private String projectName;
    private String description;
    private Long lessonId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    /** 1=团队项目（需组长提交贡献表） */
    private Integer isTeam;
    private BigDecimal fullScore;
    /** 允许的提交形式，逗号分隔：WORD,PDF,CODE,PPT,ZIP,LINK */
    private String allowFormats;
    private Long schemeItemId;
}
`);

w('dto/TeamSaveRequest.java', `
package ${P}.dto;

import lombok.Data;

import java.util.List;

/** 组队：本人为组长，提交成员名单 */
@Data
public class TeamSaveRequest {
    private String teamName;
    private List<Long> memberIds;
}
`);

w('dto/ProjectSubmitRequest.java', `
package ${P}.dto;

import lombok.Data;

import java.util.List;

/** 项目提交：文件（资料库 fileId）+ 仓库链接 + 说明 */
@Data
public class ProjectSubmitRequest {
    private List<Long> fileIds;
    private String repoUrl;
    private String remark;
}
`);

w('dto/ContributionItemDTO.java', `
package ${P}.dto;

import lombok.Data;

import java.math.BigDecimal;

/** 贡献表一行：某成员占多少比例（0~1，合计必须 1） */
@Data
public class ContributionItemDTO {
    private Long studentId;
    private BigDecimal ratio;
    private String note;
}
`);

w('dto/ContributionSubmitRequest.java', `
package ${P}.dto;

import lombok.Data;

import java.util.List;

/** 组长提交贡献表（提交后锁定，教师也不能改） */
@Data
public class ContributionSubmitRequest {
    private List<ContributionItemDTO> items;
    private String remark;
}
`);

w('dto/ProjectScoreRequest.java', `
package ${P}.dto;

import lombok.Data;

import java.math.BigDecimal;

/** 教师给项目总分（团队项目为整组分） */
@Data
public class ProjectScoreRequest {
    private BigDecimal score;
    private String comment;
}
`);

// ================================================================ vo
w('vo/ProjectMemberVO.java', `
package ${P}.vo;

import lombok.Data;

import java.math.BigDecimal;

/** 组员（含组长标记、贡献占比与折算后的个人得分） */
@Data
public class ProjectMemberVO {
    private Long studentId;
    private String studentNo;
    private String studentName;
    private Boolean leader;
    private BigDecimal ratio;
    private BigDecimal personalScore;
    private String note;
}
`);

w('vo/ProjectTeamVO.java', `
package ${P}.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 项目组：成员 + 贡献表状态 */
@Data
public class ProjectTeamVO {
    private Long id;
    private String teamName;
    private Long leaderStudentId;
    private String leaderStudentNo;
    private String leaderStudentName;
    private Integer memberCount;
    private List<ProjectMemberVO> members;
    private Boolean contributionLocked;
    private BigDecimal totalRatio;
    private LocalDateTime contributionTime;
    private String contributionRemark;
}
`);

w('vo/ProjectSubmissionVO.java', `
package ${P}.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** 项目提交与批阅 */
@Data
public class ProjectSubmissionVO {
    private Long id;
    private Long projectId;
    private Long teamId;
    private String teamName;
    private Long studentId;
    private String studentNo;
    private String studentName;
    private LocalDateTime submitTime;
    private List<Map<String, String>> files;
    private String repoUrl;
    private String remark;
    private String submitStatus;
    private BigDecimal projectScore;
    private String comment;
    private String markedByName;
    private LocalDateTime markedAt;
    /** 团队项目：各成员的占比与个人得分 */
    private List<ProjectMemberVO> members;
}
`);

w('vo/ProjectVO.java', `
package ${P}.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 项目任务（教师端） */
@Data
public class ProjectVO {
    private Long id;
    private Long teachingClassId;
    private String projectName;
    private String description;
    private Long lessonId;
    private Integer weekNo;
    private Integer sessionNo;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer isTeam;
    private BigDecimal fullScore;
    private String allowFormats;
    private Long schemeItemId;
    private String schemeItemName;
    private String status;
    private Integer teamCount;
    private Integer submitCount;
    private Integer markedCount;
    private Integer totalStudents;
    private Integer missingContributionCount;
    private List<ProjectTeamVO> teams;
    private List<ProjectSubmissionVO> submissions;
}
`);

w('vo/StudentProjectVO.java', `
package ${P}.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 项目任务（学生端） */
@Data
public class StudentProjectVO {
    private Long id;
    private Long teachingClassId;
    private String courseName;
    private String teacherName;
    private String projectName;
    private String description;
    private Integer isTeam;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal fullScore;
    private String allowFormats;
    private String status;
    private Boolean canSubmit;
    private String reason;
    private Boolean canCreateTeam;
    private ProjectTeamVO myTeam;
    private ProjectSubmissionVO mySubmission;
    private Boolean isLeader;
    private Boolean contributionSubmitted;
    private List<ProjectMemberVO> teamMembers;
}
`);

console.log('项目模块：util / dto / vo 生成完成');

package com.example.academic_risk_warning.controller;

import com.example.academic_risk_warning.config.WarningSystemProperties;
import com.example.academic_risk_warning.entity.CourseKnowledgePoint;
import com.example.academic_risk_warning.entity.Exercise;
import com.example.academic_risk_warning.entity.ExerciseDraft;
import com.example.academic_risk_warning.entity.ExerciseRecommendation;
import com.example.academic_risk_warning.entity.ExerciseSubQuestion;
import com.example.academic_risk_warning.entity.Student;
import com.example.academic_risk_warning.service.AlertQueryService;
import com.example.academic_risk_warning.service.ExerciseService;
import com.example.academic_risk_warning.service.StudentLoginService;
import com.example.academic_risk_warning.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 练习推荐系统接口
 */
@Validated
@RestController
@RequestMapping("/api/exercise")
public class ExerciseController {

    private final ExerciseService exerciseService;
    private final TokenService tokenService;
    private final WarningSystemProperties warningProperties;
    private final StudentLoginService studentLoginService;
    private final AlertQueryService alertQueryService;

    /** 允许上传的文件扩展名 */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".pdf", ".doc", ".docx", ".jpg", ".jpeg", ".png", ".gif");

    public ExerciseController(ExerciseService exerciseService, TokenService tokenService,
                              WarningSystemProperties warningProperties,
                              StudentLoginService studentLoginService,
                              AlertQueryService alertQueryService) {
        this.exerciseService = exerciseService;
        this.tokenService = tokenService;
        this.warningProperties = warningProperties;
        this.studentLoginService = studentLoginService;
        this.alertQueryService = alertQueryService;
    }

    // ==================== 知识点管理（教师端） ====================

    /** GET /exercise/knowledge/tree?courseId=1 */
    @GetMapping("/knowledge/tree")
    public Map<String, Object> getKnowledgeTree(@RequestParam Long courseId, HttpServletRequest request) {
        if (resolveTeacherId(request) == null) return err("未登录");
        try {
            return ok("data", exerciseService.getKnowledgeTree(courseId));
        } catch (Exception e) { return err(e.getMessage()); }
    }

    /** POST /exercise/knowledge/add */
    @PostMapping("/knowledge/add")
    public Map<String, Object> addKnowledge(@Valid @RequestBody KpAddRequest req, HttpServletRequest request) {
        if (resolveTeacherId(request) == null) return err("未登录");
        try {
            CourseKnowledgePoint kp = exerciseService.addKnowledgePoint(req.getCourseId(), req.getParentId(), req.getName());
            return ok("msg", "添加成功");
        } catch (Exception e) { return err(e.getMessage()); }
    }

    /** DELETE /exercise/knowledge/{id} */
    @DeleteMapping("/knowledge/{id}")
    public Map<String, Object> deleteKnowledge(@PathVariable Long id, HttpServletRequest request) {
        if (resolveTeacherId(request) == null) return err("未登录");
        try {
            exerciseService.deleteKnowledgePoint(id);
            return ok("msg", "已删除");
        } catch (Exception e) { return err(e.getMessage()); }
    }

    /** PUT /exercise/knowledge/{id}/desc — 更新知识点介绍 */
    @PutMapping("/knowledge/{id}/desc")
    public Map<String, Object> updateKnowledgeDesc(@PathVariable Long id, @RequestBody Map<String, String> body, HttpServletRequest request) {
        if (resolveTeacherId(request) == null) return err("未登录");
        try {
            exerciseService.updateKnowledgeDesc(id, body.get("description"));
            return ok("msg", "保存成功");
        } catch (Exception e) { return err(e.getMessage()); }
    }

    // ==================== 题库管理（教师端） ====================

    /** GET /exercise/list?courseId=1&keyword=&difficulty=&kpId=&page=1&size=10 */
    @GetMapping("/list")
    public Map<String, Object> listExercises(@RequestParam(required = false) Long courseId,
                                              @RequestParam(required = false) String keyword,
                                              @RequestParam(required = false) String difficulty,
                                              @RequestParam(required = false) Long kpId,
                                              @RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "10") int size,
                                              HttpServletRequest request) {
        Long teacherId = resolveTeacherId(request);
        if (teacherId == null) return err("未登录");
        try {
            return ok("data", exerciseService.listExercises(teacherId, courseId,
                    keyword, difficulty, kpId, page, size));
        } catch (Exception e) { return err(e.getMessage()); }
    }

    /** POST /exercise/add — 支持文件附件（PDF/Word/图片）+ 题目类型与标准答案 */
    @PostMapping("/add")
    public Map<String, Object> addExercise(
            @RequestParam @NotNull Long courseId,
            @RequestParam @NotEmpty List<Long> kpIds,
            @RequestParam @NotBlank String title,
            @RequestParam(required = false, defaultValue = "") String description,
            @RequestParam @NotBlank String difficulty,
            @RequestParam(required = false) String questionType,
            @RequestParam(required = false) String answer,
            @RequestParam(required = false) MultipartFile file,
            HttpServletRequest request) {
        Long teacherId = resolveTeacherId(request);
        if (teacherId == null) return err("未登录");
        try {
            String fileUrl = null;
            String fileType = null;
            if (file != null && !file.isEmpty()) {
                validateFile(file);
                // 保存文件
                File uploadDirFile = new File(warningProperties.getUploadDir());
                if (!uploadDirFile.exists()) uploadDirFile.mkdirs();
                String originalName = file.getOriginalFilename();
                String ext = "";
                if (originalName != null && originalName.contains(".")) {
                    ext = originalName.substring(originalName.lastIndexOf("."));
                }
                String savedName = UUID.randomUUID().toString() + ext;
                Path targetPath = Paths.get(warningProperties.getUploadDir(), savedName);
                Files.write(targetPath, file.getBytes());
                fileUrl = "/uploads/exercises/" + savedName;
                fileType = file.getContentType();
            }
            Exercise e = exerciseService.addExercise(teacherId, courseId,
                    kpIds, title, description, difficulty, questionType, answer, fileUrl, fileType);
            return ok("msg", "添加成功");
        } catch (IOException e) {
            return err("文件保存失败: " + e.getMessage());
        } catch (Exception e) {
            return err(e.getMessage());
        }
    }

    /** DELETE /exercise/{id} */
    @DeleteMapping("/{id}")
    public Map<String, Object> deleteExercise(@PathVariable Long id, HttpServletRequest request) {
        Long teacherId = resolveTeacherId(request);
        if (teacherId == null) return err("未登录");
        try {
            exerciseService.deleteExercise(id, teacherId);
            return ok("msg", "已删除");
        } catch (Exception e) { return err(e.getMessage()); }
    }

    /** PUT /exercise/{id} — 编辑练习题（含题目类型与标准答案） */
    @PutMapping("/{id}")
    public Map<String, Object> updateExercise(@PathVariable Long id,
            @RequestParam Long courseId,
            @RequestParam @NotEmpty List<Long> kpIds,
            @RequestParam @NotBlank String title,
            @RequestParam(required = false, defaultValue = "") String description,
            @RequestParam @NotBlank String difficulty,
            @RequestParam(required = false) String questionType,
            @RequestParam(required = false) String answer,
            @RequestParam(required = false) MultipartFile file,
            HttpServletRequest request) {
        Long teacherId = resolveTeacherId(request);
        if (teacherId == null) return err("未登录");
        try {
            String fileUrl = null;
            String fileType = null;
            if (file != null && !file.isEmpty()) {
                validateFile(file);
                File uploadDirFile = new File(warningProperties.getUploadDir());
                if (!uploadDirFile.exists()) uploadDirFile.mkdirs();
                String originalName = file.getOriginalFilename();
                String ext = "";
                if (originalName != null && originalName.contains(".")) {
                    ext = originalName.substring(originalName.lastIndexOf("."));
                }
                String savedName = UUID.randomUUID().toString() + ext;
                Path targetPath = Paths.get(warningProperties.getUploadDir(), savedName);
                Files.write(targetPath, file.getBytes());
                fileUrl = "/uploads/exercises/" + savedName;
                fileType = file.getContentType();
            }
            Exercise e = exerciseService.updateExercise(id, teacherId, kpIds, title, description,
                    difficulty, questionType, answer, fileUrl, fileType);
            return ok("msg", "更新成功");
        } catch (IOException e) {
            return err("文件保存失败: " + e.getMessage());
        } catch (Exception e) {
            return err(e.getMessage());
        }
    }

    // ==================== 薄弱点 + 匹配推荐（教师端） ====================

    /** GET /exercise/student-weak-points?studentId=1&courseId=1 */
    @GetMapping("/student-weak-points")
    public Map<String, Object> getStudentWeakPoints(@RequestParam Long studentId,
                                                     @RequestParam Long courseId,
                                                     HttpServletRequest request) {
        if (resolveTeacherId(request) == null) return err("未登录");
        return ok("data", exerciseService.getStudentWeakPoints(studentId, courseId));
    }

    /** GET /exercise/match?courseId=1&kpIds=3,7,15 */
    @GetMapping("/match")
    public Map<String, Object> matchExercises(@RequestParam Long courseId,
                                               @RequestParam List<Long> kpIds,
                                               HttpServletRequest request) {
        if (resolveTeacherId(request) == null) return err("未登录");
        return ok("data", exerciseService.matchExercises(courseId, kpIds));
    }

    /** POST /exercise/generate-weak-points — 根据知识掌握数据自动生成薄弱点 */
    @PostMapping("/generate-weak-points")
    public Map<String, Object> generateWeakPoints(@RequestParam Long studentId,
                                                   @RequestParam Long courseId,
                                                   @RequestParam(required = false) Long alertId,
                                                   HttpServletRequest request) {
        if (resolveTeacherId(request) == null) return err("未登录");
        try {
            int count = exerciseService.generateWeakPoints(studentId, courseId, alertId);
            return ok("msg", "已生成 " + count + " 个薄弱知识点记录");
        } catch (Exception e) { return err(e.getMessage()); }
    }

    /** POST /exercise/recommend */
    @PostMapping("/recommend")
    public Map<String, Object> recommend(@RequestBody RecommendRequest req, HttpServletRequest request) {
        Long teacherId = resolveTeacherId(request);
        if (teacherId == null) return err("未登录");
        try {
            ExerciseRecommendation rec = exerciseService.recommend(
                    teacherId, req.getExerciseId(), req.getStudentId(), req.getAlertId());
            return ok("msg", "推荐成功");
        } catch (Exception e) { return err(e.getMessage()); }
    }

    /** GET /exercise/recommend/list?alertId=829&page=1&size=20 */
    @GetMapping("/recommend/list")
    public Map<String, Object> getRecommendationsByAlert(@RequestParam Long alertId,
                                                          @RequestParam(defaultValue = "1") int page,
                                                          @RequestParam(defaultValue = "20") int size,
                                                          HttpServletRequest request) {
        if (resolveTeacherId(request) == null) return err("未登录");
        return ok("data", exerciseService.getRecommendationsByAlert(alertId, page, size));
    }

    /** GET /exercise/available-for-alert?alertId=829 — 根据预警获取可选练习题列表 */
    @GetMapping("/available-for-alert")
    public Map<String, Object> getAvailableExercisesForAlert(@RequestParam Long alertId,
                                                              HttpServletRequest request) {
        if (resolveTeacherId(request) == null) return err("未登录");
        try {
            return ok("data", alertQueryService.getAvailableExercisesForAlert(alertId));
        } catch (Exception e) { return err(e.getMessage()); }
    }

    // ==================== 子题目管理（教师端） ====================

    /** POST /exercise/{exerciseId}/sub-questions — 批量为习题添加子题目 */
    @PostMapping("/{exerciseId}/sub-questions")
    public Map<String, Object> addSubQuestions(@PathVariable Long exerciseId,
                                                @RequestBody List<Map<String, Object>> subQuestions,
                                                HttpServletRequest request) {
        Long teacherId = resolveTeacherId(request);
        if (teacherId == null) return err("未登录");
        try {
            List<ExerciseSubQuestion> list = exerciseService.addSubQuestions(exerciseId, teacherId, subQuestions);
            return ok("data", list.stream().map(q -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", q.getId()); m.put("seq", q.getSeq()); m.put("title", q.getTitle());
                m.put("kpId", q.getKpId()); m.put("score", q.getScore());
                return m;
            }).toList());
        } catch (Exception e) { return err(e.getMessage()); }
    }

    /** GET /exercise/{exerciseId}/sub-questions — 获取习题的子题目列表 */
    @GetMapping("/{exerciseId}/sub-questions")
    public Map<String, Object> getSubQuestions(@PathVariable Long exerciseId, HttpServletRequest request) {
        // 教师或学生均可访问
        try {
            return ok("data", exerciseService.getSubQuestionsWithNames(exerciseId));
        } catch (Exception e) { return err(e.getMessage()); }
    }

    // ==================== 学生端 ====================

    /** GET /exercise/my-recommends?page=1&size=20 (学生查看推荐列表) */
    @GetMapping("/my-recommends")
    public Map<String, Object> getMyRecommends(@RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "20") int size,
                                                HttpServletRequest request) {
        Long studentId = resolveStudentId(request);
        if (studentId == null) return err("未登录");
        return ok("data", exerciseService.getStudentRecommendations(studentId, page, size));
    }

    /** POST /exercise/submit — 学生提交作答（支持子题分题作答） */
    @PostMapping("/submit")
    public Map<String, Object> submitAnswer(@RequestBody SubmitRequest req, HttpServletRequest request) {
        Long studentId = resolveStudentId(request);
        if (studentId == null) return err("未登录");
        try {
            if (req.getSubAnswers() != null && !req.getSubAnswers().isEmpty()) {
                // 子题分题作答
                exerciseService.submitAnswer(req.getRecommendId(), req.getSubAnswers());
            } else {
                // 无子题，简化版
                exerciseService.submitAnswerSimple(req.getRecommendId(), req.getAnswer());
            }
            return ok("msg", "提交成功");
        } catch (Exception e) { return err(e.getMessage()); }
    }

    /** GET /exercise/recommend/{recommendId}/sub-answers — 获取推荐的子题作答记录 */
    @GetMapping("/recommend/{recommendId}/sub-answers")
    public Map<String, Object> getSubAnswers(@PathVariable Long recommendId, HttpServletRequest request) {
        try {
            return ok("data", exerciseService.getSubAnswersWithDetails(recommendId));
        } catch (Exception e) { return err(e.getMessage()); }
    }

    /** POST /exercise/grade — 教师评分（支持子题分题评分） */
    @PostMapping("/grade")
    public Map<String, Object> gradeFeedback(@RequestBody GradeRequest req, HttpServletRequest request) {
        if (resolveTeacherId(request) == null) return err("未登录");
        try {
            if (req.getSubScores() != null && !req.getSubScores().isEmpty()) {
                // 子题分题评分
                exerciseService.gradeFeedback(req.getRecommendId(), req.getSubScores(), req.getFeedback());
            } else {
                // 无子题，简化版
                exerciseService.gradeFeedbackSimple(req.getRecommendId(), req.getScore(), req.getFeedback());
            }
            return ok("msg", "评分完成");
        } catch (Exception e) { return err(e.getMessage()); }
    }

    /** GET /exercise/pending-grading?courseId=1&page=1&size=20 — 教师获取待批改的练习列表（ANSWERED 状态） */
    @GetMapping("/pending-grading")
    public Map<String, Object> getPendingGrading(@RequestParam Long courseId,
                                                  @RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "20") int size,
                                                  HttpServletRequest request) {
        if (resolveTeacherId(request) == null) return err("未登录");
        try {
            return ok("data", exerciseService.getPendingGrading(courseId, page, size));
        } catch (Exception e) { return err(e.getMessage()); }
    }

    // ==================== 鉴权辅助 ====================

    private Long resolveTeacherId(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth == null || auth.isBlank()) return null;
        if (auth.startsWith("Bearer ")) auth = auth.substring(7).trim();
        return tokenService.validateAndGetTeacherId(auth);
    }

    private Long resolveStudentId(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth == null || auth.isBlank()) return null;
        if (auth.startsWith("Bearer ")) auth = auth.substring(7).trim();
        Long userId = tokenService.validateAndGetStudentId(auth);
        if (userId == null) return null;
        // 校验Token对应的用户确实是学生（而非教师）
        Student student = studentLoginService.getStudentById(userId);
        return student != null ? student.getId() : null;
    }

    // ==================== 文件校验 ====================

    /**
     * 校验上传文件的类型和大小
     */
    private void validateFile(MultipartFile file) {
        long maxSize = warningProperties.getMaxFileSizeMB() * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("文件大小不能超过 " + warningProperties.getMaxFileSizeMB() + "MB");
        }
        // 类型校验
        String originalName = file.getOriginalFilename();
        if (originalName != null && originalName.contains(".")) {
            String ext = originalName.substring(originalName.lastIndexOf(".")).toLowerCase();
            if (!ALLOWED_EXTENSIONS.contains(ext)) {
                throw new IllegalArgumentException("不支持的文件类型: " + ext + "，仅支持 " + ALLOWED_EXTENSIONS);
            }
        }
    }

    // ==================== 作答草稿 ====================

    /** POST /exercise/draft/save — 保存作答草稿 */
    @PostMapping("/draft/save")
    public Map<String, Object> saveDraft(@RequestBody DraftSaveRequest req, HttpServletRequest request) {
        Long studentId = resolveStudentId(request);
        if (studentId == null) return err("未登录");
        try {
            exerciseService.saveDraft(req.getRecommendId(), studentId,
                    req.getAnswer(), req.getSubAnswers());
            return ok("msg", "草稿已保存");
        } catch (Exception e) { return err(e.getMessage()); }
    }

    /** GET /exercise/draft/{recommendId} — 获取作答草稿 */
    @GetMapping("/draft/{recommendId}")
    public Map<String, Object> getDraft(@PathVariable Long recommendId, HttpServletRequest request) {
        Long studentId = resolveStudentId(request);
        if (studentId == null) return err("未登录");
        try {
            ExerciseDraft draft = exerciseService.getDraft(recommendId, studentId);
            if (draft == null) return ok("data", null);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("id", draft.getId());
            data.put("recommendId", draft.getRecommendId());
            data.put("answer", draft.getAnswer());
            data.put("subAnswers", draft.getSubAnswers());
            data.put("updateTime", draft.getUpdateTime());
            return ok("data", data);
        } catch (Exception e) { return err(e.getMessage()); }
    }

    /** DELETE /exercise/draft/{recommendId} — 删除作答草稿 */
    @DeleteMapping("/draft/{recommendId}")
    public Map<String, Object> deleteDraft(@PathVariable Long recommendId, HttpServletRequest request) {
        Long studentId = resolveStudentId(request);
        if (studentId == null) return err("未登录");
        try {
            exerciseService.deleteDraft(recommendId, studentId);
            return ok("msg", "草稿已清除");
        } catch (Exception e) { return err(e.getMessage()); }
    }

    // ==================== 响应 ====================

    private Map<String, Object> ok(String key, Object data) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("success", true);
        r.put("message", "操作成功");
        r.put(key, data);
        return r;
    }

    private Map<String, Object> err(String msg) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("success", false);
        r.put("message", msg);
        return r;
    }

    // ==================== 请求体 ====================

    public static class KpAddRequest {
        @NotNull private Long courseId;
        private Long parentId;
        @NotBlank private String name;
        public Long getCourseId() { return courseId; } public void setCourseId(Long v) { courseId = v; }
        public Long getParentId() { return parentId; } public void setParentId(Long v) { parentId = v; }
        public String getName() { return name; } public void setName(String v) { name = v; }
    }

    public static class ExerciseAddRequest {
        @NotNull private Long courseId;
        @NotEmpty private List<Long> kpIds;
        @NotBlank private String title;
        private String description;
        @NotBlank private String difficulty;
        public Long getCourseId() { return courseId; } public void setCourseId(Long v) { courseId = v; }
        public List<Long> getKpIds() { return kpIds; } public void setKpIds(List<Long> v) { kpIds = v; }
        public String getTitle() { return title; } public void setTitle(String v) { title = v; }
        public String getDescription() { return description; } public void setDescription(String v) { description = v; }
        public String getDifficulty() { return difficulty; } public void setDifficulty(String v) { difficulty = v; }
    }

    public static class RecommendRequest {
        @NotNull private Long exerciseId;
        @NotNull private Long studentId;
        private Long alertId;
        public Long getExerciseId() { return exerciseId; } public void setExerciseId(Long v) { exerciseId = v; }
        public Long getStudentId() { return studentId; } public void setStudentId(Long v) { studentId = v; }
        public Long getAlertId() { return alertId; } public void setAlertId(Long v) { alertId = v; }
    }

    public static class SubmitRequest {
        @NotNull private Long recommendId;
        private String answer;
        private Map<Long, String> subAnswers;  // subQuestionId → answer
        public Long getRecommendId() { return recommendId; } public void setRecommendId(Long v) { recommendId = v; }
        public String getAnswer() { return answer; } public void setAnswer(String v) { answer = v; }
        public Map<Long, String> getSubAnswers() { return subAnswers; } public void setSubAnswers(Map<Long, String> v) { subAnswers = v; }
    }

    public static class GradeRequest {
        @NotNull private Long recommendId;
        @Min(0) @Max(100) private int score;
        private String feedback;
        private Map<Long, Integer> subScores;  // subQuestionId → score
        public Long getRecommendId() { return recommendId; } public void setRecommendId(Long v) { recommendId = v; }
        public int getScore() { return score; } public void setScore(int v) { score = v; }
        public String getFeedback() { return feedback; } public void setFeedback(String v) { feedback = v; }
        public Map<Long, Integer> getSubScores() { return subScores; } public void setSubScores(Map<Long, Integer> v) { subScores = v; }
    }

    public static class DraftSaveRequest {
        @NotNull private Long recommendId;
        private String answer;
        private String subAnswers;  // JSON字符串: Map<subQuestionId, answer>
        public Long getRecommendId() { return recommendId; } public void setRecommendId(Long v) { recommendId = v; }
        public String getAnswer() { return answer; } public void setAnswer(String v) { answer = v; }
        public String getSubAnswers() { return subAnswers; } public void setSubAnswers(String v) { subAnswers = v; }
    }
}

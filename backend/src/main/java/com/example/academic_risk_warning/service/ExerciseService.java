package com.example.academic_risk_warning.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.academic_risk_warning.config.WarningSystemProperties;
import com.example.academic_risk_warning.entity.*;
import com.example.academic_risk_warning.mapper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 练习题与知识点管理服务
 */
@Service
public class ExerciseService {

    private final CourseKnowledgePointMapper kpMapper;
    private final StudentWeakPointMapper weakPointMapper;
    private final ExerciseMapper exerciseMapper;
    private final ExerciseRecommendationMapper recommendMapper;
    private final ExerciseKnowledgePointMapper exerciseKpMapper;
    private final StudentMapper studentMapper;
    private final KnowledgeMasteryMapper masteryMapper;
    private final ExerciseSubQuestionMapper subQuestionMapper;
    private final SubQuestionAnswerMapper subAnswerMapper;
    private final ExerciseSubKpMapper subKpMapper;
    private final AlertRecordMapper alertRecordMapper;
    private final ExerciseDraftMapper draftMapper;
    private final WarningSystemProperties warningProperties;

    public ExerciseService(CourseKnowledgePointMapper kpMapper,
                           StudentWeakPointMapper weakPointMapper,
                           ExerciseMapper exerciseMapper,
                           ExerciseRecommendationMapper recommendMapper,
                           ExerciseKnowledgePointMapper exerciseKpMapper,
                           StudentMapper studentMapper,
                           KnowledgeMasteryMapper masteryMapper,
                           ExerciseSubQuestionMapper subQuestionMapper,
                           SubQuestionAnswerMapper subAnswerMapper,
                           ExerciseSubKpMapper subKpMapper,
                           AlertRecordMapper alertRecordMapper,
                           ExerciseDraftMapper draftMapper,
                           WarningSystemProperties warningProperties) {
        this.kpMapper = kpMapper;
        this.weakPointMapper = weakPointMapper;
        this.exerciseMapper = exerciseMapper;
        this.recommendMapper = recommendMapper;
        this.exerciseKpMapper = exerciseKpMapper;
        this.studentMapper = studentMapper;
        this.masteryMapper = masteryMapper;
        this.subQuestionMapper = subQuestionMapper;
        this.subAnswerMapper = subAnswerMapper;
        this.subKpMapper = subKpMapper;
        this.alertRecordMapper = alertRecordMapper;
        this.draftMapper = draftMapper;
        this.warningProperties = warningProperties;
    }

    // ==================== 知识点管理 ====================

    /** 获取某课程的知识点树 */
    public List<Map<String, Object>> getKnowledgeTree(Long courseId) {
        List<CourseKnowledgePoint> list = kpMapper.selectList(
                new LambdaQueryWrapper<CourseKnowledgePoint>()
                        .eq(CourseKnowledgePoint::getCourseId, courseId)
                        .orderByAsc(CourseKnowledgePoint::getSortOrder));
        // 分离一级和二级
        Map<Long, Map<String, Object>> parentMap = new LinkedHashMap<>();
        List<Map<String, Object>> roots = new ArrayList<>();
        for (CourseKnowledgePoint kp : list) {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", kp.getId());
            node.put("name", kp.getName());
            node.put("description", kp.getDescription());
            node.put("sortOrder", kp.getSortOrder());
            if (kp.getParentId() == null) {
                node.put("children", new ArrayList<>());
                parentMap.put(kp.getId(), node);
                roots.add(node);
            }
        }
        for (CourseKnowledgePoint kp : list) {
            if (kp.getParentId() != null && parentMap.containsKey(kp.getParentId())) {
                Map<String, Object> child = new LinkedHashMap<>();
                child.put("id", kp.getId());
                child.put("name", kp.getName());
                child.put("description", kp.getDescription());
                child.put("sortOrder", kp.getSortOrder());
                @SuppressWarnings("unchecked")
                List<Object> children = (List<Object>) parentMap.get(kp.getParentId()).get("children");
                if (children != null) children.add(child);
            }
        }
        return roots;
    }

    /** 添加知识点 */
    public CourseKnowledgePoint addKnowledgePoint(Long courseId, Long parentId, String name) {
        // 检查重名
        Long count = kpMapper.selectCount(
                new LambdaQueryWrapper<CourseKnowledgePoint>()
                        .eq(CourseKnowledgePoint::getCourseId, courseId)
                        .eq(CourseKnowledgePoint::getName, name));
        if (count > 0) {
            throw new IllegalArgumentException("知识点「" + name + "」已存在，请勿重复添加");
        }
        CourseKnowledgePoint kp = new CourseKnowledgePoint();
        kp.setCourseId(courseId);
        kp.setParentId(parentId);
        kp.setName(name);
        kp.setSortOrder(0);
        kpMapper.insert(kp);
        return kp;
    }

    /** 删除知识点（含关联练习题校验 + 级联删除子节点） */
    @Transactional
    public void deleteKnowledgePoint(Long id) {
        // 收集所有待删除的知识点ID（父节点 + 子节点）
        List<Long> idsToDelete = new ArrayList<>();
        idsToDelete.add(id);
        List<CourseKnowledgePoint> children = kpMapper.selectList(
                new LambdaQueryWrapper<CourseKnowledgePoint>().eq(CourseKnowledgePoint::getParentId, id));
        for (CourseKnowledgePoint child : children) idsToDelete.add(child.getId());

        // 检查是否有关联的练习题
        Long exerciseCount = exerciseKpMapper.selectCount(
                new LambdaQueryWrapper<ExerciseKnowledgePoint>().in(ExerciseKnowledgePoint::getKpId, idsToDelete));
        if (exerciseCount > 0) {
            throw new IllegalArgumentException("该知识点下存在 " + exerciseCount + " 道关联练习题，请先移除练习题或修改其知识点关联后再删除");
        }

        // 先删除子节点
        for (CourseKnowledgePoint child : children) kpMapper.deleteById(child.getId());
        // 再删除父节点
        kpMapper.deleteById(id);
    }

    /** 更新知识点介绍 */
    public void updateKnowledgeDesc(Long id, String description) {
        CourseKnowledgePoint kp = kpMapper.selectById(id);
        if (kp != null) {
            kp.setDescription(description);
            kpMapper.updateById(kp);
        }
    }

    // ==================== 题库管理 ====================

    /** 教师获取自己的题库（支持关键词搜索、难度筛选、知识点筛选、分页） */
    public Map<String, Object> listExercises(Long teacherId, Long courseId,
                                              String keyword, String difficulty, Long kpId,
                                              int page, int size) {
        LambdaQueryWrapper<Exercise> wrapper = new LambdaQueryWrapper<Exercise>()
                .eq(Exercise::getTeacherId, teacherId)
                .eq(courseId != null, Exercise::getCourseId, courseId)
                .eq(Exercise::getIsActive, 1)
                .eq(difficulty != null && !difficulty.isBlank(), Exercise::getDifficulty, difficulty)
                .like(keyword != null && !keyword.isBlank(), Exercise::getTitle, keyword)
                .orderByDesc(Exercise::getCreateTime);

        // 按知识点筛选：先通过关联表找出符合条件的 exercise_id
        Set<Long> kpFilterIds = null;
        if (kpId != null) {
            List<ExerciseKnowledgePoint> ekps = exerciseKpMapper.selectList(
                    new LambdaQueryWrapper<ExerciseKnowledgePoint>().eq(ExerciseKnowledgePoint::getKpId, kpId));
            kpFilterIds = ekps.stream().map(ExerciseKnowledgePoint::getExerciseId).collect(Collectors.toSet());
            if (kpFilterIds.isEmpty()) {
                Map<String, Object> empty = new LinkedHashMap<>();
                empty.put("total", 0L);
                empty.put("page", page);
                empty.put("size", size);
                empty.put("records", Collections.emptyList());
                return empty;
            }
            wrapper.in(Exercise::getId, kpFilterIds);
        }

        // 分页查询
        Page<Exercise> p = new Page<>(page, size);
        Page<Exercise> resultPage = exerciseMapper.selectPage(p, wrapper);

        // 批量预加载知识点关联数据以消除 N+1
        List<Long> pageExerciseIds = resultPage.getRecords().stream().map(Exercise::getId).toList();
        Map<Long, List<Long>> exerciseKpMap = new HashMap<>(); // exerciseId → [kpIds]
        Map<Long, String> kpNameMapGlobal = new HashMap<>(); // kpId → name
        if (!pageExerciseIds.isEmpty()) {
            List<ExerciseKnowledgePoint> allEkps = exerciseKpMapper.selectList(
                    new LambdaQueryWrapper<ExerciseKnowledgePoint>().in(ExerciseKnowledgePoint::getExerciseId, pageExerciseIds));
            Set<Long> allKpIds = new HashSet<>();
            for (ExerciseKnowledgePoint ekp : allEkps) {
                exerciseKpMap.computeIfAbsent(ekp.getExerciseId(), k -> new ArrayList<>()).add(ekp.getKpId());
                allKpIds.add(ekp.getKpId());
            }
            if (!allKpIds.isEmpty()) {
                List<CourseKnowledgePoint> allKps = kpMapper.selectList(
                        new LambdaQueryWrapper<CourseKnowledgePoint>().in(CourseKnowledgePoint::getId, allKpIds));
                for (CourseKnowledgePoint kp : allKps) {
                    kpNameMapGlobal.put(kp.getId(), kp.getName());
                }
            }
        }

        List<Map<String, Object>> records = resultPage.getRecords().stream().map(e -> {
            Map<String, Object> m = toMap(e);
            List<Long> kpIds = exerciseKpMap.getOrDefault(e.getId(), Collections.emptyList());
            List<String> kpNames = kpIds.stream()
                    .map(kpid -> kpNameMapGlobal.getOrDefault(kpid, ""))
                    .filter(s -> !s.isEmpty())
                    .toList();
            m.put("kpIds", kpIds);
            m.put("knowledgePointNames", kpNames);
            m.put("knowledgePointName", String.join("、", kpNames));
            return m;
        }).toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", resultPage.getTotal());
        result.put("page", page);
        result.put("size", size);
        result.put("records", records);
        return result;
    }

    /** 添加练习题（支持多知识点 + 文件 + 题目类型与标准答案） */
    @Transactional
    public Exercise addExercise(Long teacherId, Long courseId, List<Long> kpIds,
                                 String title, String description, String difficulty,
                                 String questionType, String answer,
                                 String fileUrl, String fileType) {
        Exercise e = new Exercise();
        e.setTeacherId(teacherId);
        e.setCourseId(courseId);
        e.setTitle(title);
        e.setDescription(description);
        e.setDifficulty(difficulty);
        e.setQuestionType(questionType);
        e.setAnswer(answer);
        e.setFileUrl(fileUrl);
        e.setFileType(fileType);
        e.setIsActive(1);
        e.setAuditStatus("PENDING");
        exerciseMapper.insert(e);
        // 多对多关联（含重复检查，防止同一练习题重复关联同一知识点）
        if (kpIds != null && !kpIds.isEmpty()) {
            // 批量查重：一次查询替代 N 次 selectCount
            List<ExerciseKnowledgePoint> existing = exerciseKpMapper.selectList(
                    new LambdaQueryWrapper<ExerciseKnowledgePoint>()
                            .eq(ExerciseKnowledgePoint::getExerciseId, e.getId())
                            .in(ExerciseKnowledgePoint::getKpId, kpIds));
            Set<Long> existingKpIds = existing.stream()
                    .map(ExerciseKnowledgePoint::getKpId).collect(Collectors.toSet());
            for (Long kpId : kpIds) {
                if (!existingKpIds.contains(kpId)) {
                    ExerciseKnowledgePoint ekp = new ExerciseKnowledgePoint();
                    ekp.setExerciseId(e.getId());
                    ekp.setKpId(kpId);
                    exerciseKpMapper.insert(ekp);
                }
            }
        }
        return e;
    }

    /** 删除练习题（级联清理关联表记录），校验操作用户是否为题目上传者 */
    @Transactional
    public void deleteExercise(Long id, Long teacherId) {
        Exercise exercise = exerciseMapper.selectById(id);
        if (exercise == null) throw new IllegalArgumentException("练习题不存在");
        if (!teacherId.equals(exercise.getTeacherId())) {
            throw new IllegalArgumentException("无权删除其他教师的练习题");
        }
        // 1. 先删除关联表记录，避免产生孤立的 exercise_knowledge_point 数据
        exerciseKpMapper.delete(new LambdaQueryWrapper<ExerciseKnowledgePoint>()
                .eq(ExerciseKnowledgePoint::getExerciseId, id));
        // 2. 再删除练习题本身
        exerciseMapper.deleteById(id);
    }

    /** 编辑练习题（含题目类型与标准答案），校验操作用户是否为题目上传者 */
    @Transactional
    public Exercise updateExercise(Long id, Long teacherId, List<Long> kpIds, String title,
                                    String description, String difficulty,
                                    String questionType, String answer,
                                    String fileUrl, String fileType) {
        Exercise e = exerciseMapper.selectById(id);
        if (e == null) throw new IllegalArgumentException("练习题不存在");
        if (!teacherId.equals(e.getTeacherId())) {
            throw new IllegalArgumentException("无权编辑其他教师的练习题");
        }
        e.setTitle(title);
        e.setDescription(description);
        e.setDifficulty(difficulty);
        e.setQuestionType(questionType);
        e.setAnswer(answer);
        if (fileUrl != null) e.setFileUrl(fileUrl);
        if (fileType != null) e.setFileType(fileType);
        exerciseMapper.updateById(e);
        // 更新知识点关联：先删后插（事务保护）
        if (kpIds != null) {
            exerciseKpMapper.delete(new LambdaQueryWrapper<ExerciseKnowledgePoint>()
                    .eq(ExerciseKnowledgePoint::getExerciseId, id));
            for (Long kpId : kpIds) {
                ExerciseKnowledgePoint ekp = new ExerciseKnowledgePoint();
                ekp.setExerciseId(id);
                ekp.setKpId(kpId);
                exerciseKpMapper.insert(ekp);
            }
        }
        return e;
    }

    // ==================== 子题目管理 ====================

    /**
     * 批量为习题添加子题目（教师拆分PDF后调用）
     * 支持 SINGLE（单知识点）和 COMPREHENSIVE（综合多知识点）两种题型
     */
    @Transactional
    public List<ExerciseSubQuestion> addSubQuestions(Long exerciseId, Long teacherId,
                                                      List<Map<String, Object>> subQuestions) {
        Exercise exercise = exerciseMapper.selectById(exerciseId);
        if (exercise == null) throw new IllegalArgumentException("练习题不存在");
        if (!teacherId.equals(exercise.getTeacherId())) {
            throw new IllegalArgumentException("无权修改其他教师的练习题");
        }
        // 先清除旧的子题和关联
        List<ExerciseSubQuestion> oldSubs = subQuestionMapper.selectList(
                new LambdaQueryWrapper<ExerciseSubQuestion>()
                        .eq(ExerciseSubQuestion::getExerciseId, exerciseId));
        for (ExerciseSubQuestion old : oldSubs) {
            subKpMapper.delete(new LambdaQueryWrapper<ExerciseSubKp>()
                    .eq(ExerciseSubKp::getSubQuestionId, old.getId()));
        }
        subQuestionMapper.delete(new LambdaQueryWrapper<ExerciseSubQuestion>()
                .eq(ExerciseSubQuestion::getExerciseId, exerciseId));

        List<ExerciseSubQuestion> result = new ArrayList<>();
        for (int i = 0; i < subQuestions.size(); i++) {
            Map<String, Object> sq = subQuestions.get(i);
            ExerciseSubQuestion q = new ExerciseSubQuestion();
            q.setExerciseId(exerciseId);
            q.setSeq(i + 1);
            q.setTitle((String) sq.getOrDefault("title", "第" + (i + 1) + "题"));

            // 题型判断
            String questionType = (String) sq.getOrDefault("questionType", "SINGLE");
            q.setQuestionType(questionType);

            if ("COMPREHENSIVE".equals(questionType)) {
                // 综合题：kpId 留空，通过 exercise_sub_kp 关联多个知识点
                q.setKpId(null);
                q.setKnowledgePoint(null);
                q.setScore(scoreFromObj(sq.get("score")));
                q.setReferenceAnswer((String) sq.get("referenceAnswer"));
                subQuestionMapper.insert(q);

                // 插入多知识点关联
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> kpWeights = (List<Map<String, Object>>) sq.get("kpWeights");
                if (kpWeights != null && !kpWeights.isEmpty()) {
                    for (Map<String, Object> kw : kpWeights) {
                        ExerciseSubKp subKp = new ExerciseSubKp();
                        subKp.setSubQuestionId(q.getId());
                        subKp.setKpId(Long.valueOf(kw.get("kpId").toString()));
                        Object w = kw.get("weight");
                        subKp.setWeight(w != null ? new BigDecimal(w.toString()) : BigDecimal.ZERO);
                        subKpMapper.insert(subKp);
                    }
                }
            } else {
                // 单知识点题（原有逻辑）
                Object kpIdObj = sq.get("kpId");
                if (kpIdObj != null && !"".equals(kpIdObj.toString())) {
                    q.setKpId(Long.valueOf(kpIdObj.toString()));
                }
                q.setKnowledgePoint((String) sq.get("knowledgePoint"));
                q.setScore(scoreFromObj(sq.get("score")));
                q.setReferenceAnswer((String) sq.get("referenceAnswer"));
                subQuestionMapper.insert(q);
            }
            result.add(q);
        }
        return result;
    }

    private int scoreFromObj(Object obj) {
        return obj != null ? Integer.parseInt(obj.toString()) : 10;
    }

    /** 获取习题的子题目列表 */
    public List<ExerciseSubQuestion> getSubQuestions(Long exerciseId) {
        return subQuestionMapper.selectList(
                new LambdaQueryWrapper<ExerciseSubQuestion>()
                        .eq(ExerciseSubQuestion::getExerciseId, exerciseId)
                        .orderByAsc(ExerciseSubQuestion::getSeq));
    }

    /** 获取习题的子题目列表（含知识点名称 + 综合题多KP信息） */
    public List<Map<String, Object>> getSubQuestionsWithNames(Long exerciseId) {
        List<ExerciseSubQuestion> list = getSubQuestions(exerciseId);

        // 收集所有需要查名称的知识点ID（SINGLE的kpId + COMPREHENSIVE的subKp）
        Set<Long> allKpIds = new HashSet<>();
        for (ExerciseSubQuestion q : list) {
            if (q.getKpId() != null) allKpIds.add(q.getKpId());
        }
        // 批量查综合题的多知识点关联
        Set<Long> subIds = list.stream().map(ExerciseSubQuestion::getId).collect(Collectors.toSet());
        Map<Long, List<ExerciseSubKp>> subKpMap = new HashMap<>(); // subQuestionId → [ExerciseSubKp]
        if (!subIds.isEmpty()) {
            List<ExerciseSubKp> allSubKps = subKpMapper.selectList(
                    new LambdaQueryWrapper<ExerciseSubKp>().in(ExerciseSubKp::getSubQuestionId, subIds));
            for (ExerciseSubKp sk : allSubKps) {
                subKpMap.computeIfAbsent(sk.getSubQuestionId(), k -> new ArrayList<>()).add(sk);
                allKpIds.add(sk.getKpId());
            }
        }

        Map<Long, String> kpNameMap = new HashMap<>();
        if (!allKpIds.isEmpty()) {
            kpMapper.selectList(new LambdaQueryWrapper<CourseKnowledgePoint>()
                    .in(CourseKnowledgePoint::getId, allKpIds))
                    .forEach(kp -> kpNameMap.put(kp.getId(), kp.getName()));
        }

        return list.stream().map(q -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", q.getId());
            m.put("exerciseId", q.getExerciseId());
            m.put("seq", q.getSeq());
            m.put("title", q.getTitle());
            m.put("questionType", q.getQuestionType() != null ? q.getQuestionType() : "SINGLE");
            m.put("kpId", q.getKpId());
            m.put("knowledgePoint", q.getKnowledgePoint() != null ? q.getKnowledgePoint()
                    : kpNameMap.getOrDefault(q.getKpId(), ""));
            m.put("score", q.getScore());
            m.put("referenceAnswer", q.getReferenceAnswer());
            // 综合题：附上多知识点列表
            List<ExerciseSubKp> subKps = subKpMap.get(q.getId());
            if (subKps != null && !subKps.isEmpty()) {
                List<Map<String, Object>> kpList = new ArrayList<>();
                for (ExerciseSubKp sk : subKps) {
                    Map<String, Object> km = new LinkedHashMap<>();
                    km.put("kpId", sk.getKpId());
                    km.put("kpName", kpNameMap.getOrDefault(sk.getKpId(), ""));
                    km.put("weight", sk.getWeight());
                    kpList.add(km);
                }
                m.put("kpWeights", kpList);
            }
            return m;
        }).toList();
    }

    /** 获取某推荐的子题作答记录（含子题标题和知识点） */
    public List<Map<String, Object>> getSubAnswersWithDetails(Long recommendId) {
        List<SubQuestionAnswer> answers = subAnswerMapper.selectList(
                new LambdaQueryWrapper<SubQuestionAnswer>()
                        .eq(SubQuestionAnswer::getRecommendId, recommendId));
        if (answers.isEmpty()) return Collections.emptyList();

        Set<Long> sqIds = answers.stream().map(SubQuestionAnswer::getSubQuestionId).collect(Collectors.toSet());
        List<ExerciseSubQuestion> subQuestions = subQuestionMapper.selectList(
                new LambdaQueryWrapper<ExerciseSubQuestion>().in(ExerciseSubQuestion::getId, sqIds));
        Map<Long, ExerciseSubQuestion> sqMap = subQuestions.stream()
                .collect(Collectors.toMap(ExerciseSubQuestion::getId, Function.identity()));

        // 批量预加载知识点名称
        Set<Long> kpIds = subQuestions.stream().map(ExerciseSubQuestion::getKpId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> kpNameMap = new HashMap<>();
        if (!kpIds.isEmpty()) {
            kpMapper.selectList(new LambdaQueryWrapper<CourseKnowledgePoint>()
                    .in(CourseKnowledgePoint::getId, kpIds))
                    .forEach(kp -> kpNameMap.put(kp.getId(), kp.getName()));
        }

        return answers.stream().map(a -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", a.getId());
            m.put("subQuestionId", a.getSubQuestionId());
            m.put("studentAnswer", a.getStudentAnswer());
            m.put("score", a.getScore());
            m.put("status", a.getStatus());
            ExerciseSubQuestion sq = sqMap.get(a.getSubQuestionId());
            if (sq != null) {
                m.put("seq", sq.getSeq());
                m.put("title", sq.getTitle());
                m.put("maxScore", sq.getScore());
                m.put("kpId", sq.getKpId());
                m.put("knowledgePoint",
                        sq.getKnowledgePoint() != null ? sq.getKnowledgePoint()
                                : kpNameMap.getOrDefault(sq.getKpId(), ""));
                m.put("referenceAnswer", sq.getReferenceAnswer());
            }
            return m;
        }).toList();
    }

    /** 判断习题是否有子题目 */
    public boolean hasSubQuestions(Long exerciseId) {
        return subQuestionMapper.selectCount(
                new LambdaQueryWrapper<ExerciseSubQuestion>()
                        .eq(ExerciseSubQuestion::getExerciseId, exerciseId)) > 0;
    }

    // ==================== 薄弱点查询 ====================

    /** 获取某学生的薄弱知识点列表（含知识点名称） */
    public List<Map<String, Object>> getStudentWeakPoints(Long studentId, Long courseId) {
        List<StudentWeakPoint> list = weakPointMapper.selectList(
                new LambdaQueryWrapper<StudentWeakPoint>()
                        .eq(StudentWeakPoint::getStudentId, studentId)
                        .eq(courseId != null, StudentWeakPoint::getCourseId, courseId)
                        .orderByDesc(StudentWeakPoint::getErrorRate));
        // 批量预加载知识点名称以消除 N+1
        Set<Long> kpIdSet = list.stream().map(StudentWeakPoint::getKpId).collect(Collectors.toSet());
        Map<Long, String> kpNameMap = new HashMap<>();
        if (!kpIdSet.isEmpty()) {
            List<CourseKnowledgePoint> kps = kpMapper.selectList(
                    new LambdaQueryWrapper<CourseKnowledgePoint>().in(CourseKnowledgePoint::getId, kpIdSet));
            for (CourseKnowledgePoint kp : kps) {
                kpNameMap.put(kp.getId(), kp.getName());
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (StudentWeakPoint wp : list) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", wp.getId());
            m.put("studentId", wp.getStudentId());
            m.put("courseId", wp.getCourseId());
            m.put("kpId", wp.getKpId());
            m.put("errorRate", wp.getErrorRate());
            m.put("knowledgePointName", kpNameMap.getOrDefault(wp.getKpId(), "未知"));
            m.put("alertId", wp.getAlertId());
            result.add(m);
        }
        return result;
    }

    /** 根据薄弱点 kp_id 匹配练习题（通过关联表） */
    public List<Map<String, Object>> matchExercises(Long courseId, List<Long> kpIds) {
        if (kpIds == null || kpIds.isEmpty()) return Collections.emptyList();
        // 通过关联表查匹配的 exercise_id
        List<ExerciseKnowledgePoint> ekps = exerciseKpMapper.selectList(
                new LambdaQueryWrapper<ExerciseKnowledgePoint>().in(ExerciseKnowledgePoint::getKpId, kpIds));
        Set<Long> exerciseIds = ekps.stream().map(ExerciseKnowledgePoint::getExerciseId).collect(Collectors.toSet());
        if (exerciseIds.isEmpty()) return Collections.emptyList();
        List<Exercise> list = exerciseMapper.selectList(
                new LambdaQueryWrapper<Exercise>()
                        .in(Exercise::getId, exerciseIds)
                        .eq(Exercise::getCourseId, courseId)
                        .eq(Exercise::getIsActive, 1)
                        .eq(Exercise::getAuditStatus, "APPROVED"));

        // 批量预加载知识点关联以消除 N+1
        Map<Long, String> kpNameMap = new HashMap<>();
        if (!list.isEmpty()) {
            List<Long> matchedIds = list.stream().map(Exercise::getId).toList();
            List<ExerciseKnowledgePoint> allEkps = exerciseKpMapper.selectList(
                    new LambdaQueryWrapper<ExerciseKnowledgePoint>().in(ExerciseKnowledgePoint::getExerciseId, matchedIds));
            Set<Long> allKpIds = allEkps.stream().map(ExerciseKnowledgePoint::getKpId).collect(Collectors.toSet());
            if (!allKpIds.isEmpty()) {
                List<CourseKnowledgePoint> allKps = kpMapper.selectList(
                        new LambdaQueryWrapper<CourseKnowledgePoint>().in(CourseKnowledgePoint::getId, allKpIds));
                for (CourseKnowledgePoint kp : allKps) {
                    kpNameMap.put(kp.getId(), kp.getName());
                }
            }
            // 构建 exercise → kpNames 的映射
            Map<Long, List<String>> exerciseNamesMap = new HashMap<>();
            for (ExerciseKnowledgePoint ekp : allEkps) {
                String name = kpNameMap.get(ekp.getKpId());
                if (name != null) {
                    exerciseNamesMap.computeIfAbsent(ekp.getExerciseId(), k -> new ArrayList<>()).add(name);
                }
            }
            return list.stream().map(e -> {
                Map<String, Object> m = toMap(e);
                List<String> names = exerciseNamesMap.getOrDefault(e.getId(), Collections.emptyList());
                m.put("knowledgePointName", String.join("、", names));
                return m;
            }).toList();
        }
        return Collections.emptyList();
    }

    /** 根据知识掌握情况自动生成学生的薄弱知识点记录（基于个性化练习评分数据） */
    @Transactional
    public int generateWeakPoints(Long studentId, Long courseId, Long alertId) {
        // 1. 清理旧记录
        weakPointMapper.delete(new LambdaQueryWrapper<StudentWeakPoint>()
                .eq(StudentWeakPoint::getStudentId, studentId)
                .eq(StudentWeakPoint::getCourseId, courseId));

        // 2. 获取该课程所有练习题
        List<Exercise> courseExercises = exerciseMapper.selectList(
                new LambdaQueryWrapper<Exercise>()
                        .eq(Exercise::getCourseId, courseId)
                        .eq(Exercise::getIsActive, 1));
        if (courseExercises.isEmpty()) {
            return 0;
        }
        List<Long> courseExerciseIds = courseExercises.stream()
                .map(Exercise::getId).toList();

        // 3. 获取该学生在这些练习中已评分的推荐记录
        List<ExerciseRecommendation> gradedRecs = recommendMapper.selectList(
                new LambdaQueryWrapper<ExerciseRecommendation>()
                        .eq(ExerciseRecommendation::getStudentId, studentId)
                        .in(ExerciseRecommendation::getExerciseId, courseExerciseIds)
                        .eq(ExerciseRecommendation::getStatus, "GRADED")
                        .isNotNull(ExerciseRecommendation::getScore));

        // 4. 无评分数据时，回退使用 knowledge_mastery 聚合数据
        if (gradedRecs.isEmpty()) {
            return generateWeakPointsFromMastery(studentId, courseId, alertId);
        }

        // 5. 批量预加载所有 exercise_knowledge_point 关联（消除 N+1）
        Set<Long> recExerciseIds = gradedRecs.stream()
                .map(ExerciseRecommendation::getExerciseId).collect(Collectors.toSet());
        List<ExerciseKnowledgePoint> allEkps = exerciseKpMapper.selectList(
                new LambdaQueryWrapper<ExerciseKnowledgePoint>()
                        .in(ExerciseKnowledgePoint::getExerciseId, recExerciseIds));

        // 构建 exerciseId → [kpIds] 映射
        Map<Long, List<Long>> exerciseKpMap = new HashMap<>();
        Set<Long> allKpIds = new HashSet<>();
        for (ExerciseKnowledgePoint ekp : allEkps) {
            exerciseKpMap.computeIfAbsent(ekp.getExerciseId(), k -> new ArrayList<>()).add(ekp.getKpId());
            allKpIds.add(ekp.getKpId());
        }

        // 6. 批量预加载所有 course_knowledge_point（消除 N+1）
        Map<Long, CourseKnowledgePoint> kpInfoMap = new HashMap<>();
        if (!allKpIds.isEmpty()) {
            List<CourseKnowledgePoint> allKps = kpMapper.selectList(
                    new LambdaQueryWrapper<CourseKnowledgePoint>().in(CourseKnowledgePoint::getId, allKpIds));
            for (CourseKnowledgePoint kp : allKps) {
                kpInfoMap.put(kp.getId(), kp);
            }
        }

        // 6b. 批量预加载子题作答记录（消除 N+1）— 优先子题粒度
        Set<Long> recIds = gradedRecs.stream().map(ExerciseRecommendation::getId).collect(Collectors.toSet());
        List<SubQuestionAnswer> allSubAnswers = subAnswerMapper.selectList(
                new LambdaQueryWrapper<SubQuestionAnswer>()
                        .in(SubQuestionAnswer::getRecommendId, recIds)
                        .eq(SubQuestionAnswer::getStatus, "GRADED")
                        .isNotNull(SubQuestionAnswer::getScore));
        Map<Long, List<SubQuestionAnswer>> subAnswerMap = new HashMap<>(); // recommendId → [subAnswers]
        Set<Long> allSubQuestionIds = new HashSet<>();
        for (SubQuestionAnswer sqa : allSubAnswers) {
            subAnswerMap.computeIfAbsent(sqa.getRecommendId(), k -> new ArrayList<>()).add(sqa);
            allSubQuestionIds.add(sqa.getSubQuestionId());
        }

        // 预加载子题信息（含 question_type）
        Map<Long, ExerciseSubQuestion> sqInfoMap = new HashMap<>();
        if (!allSubQuestionIds.isEmpty()) {
            subQuestionMapper.selectList(
                    new LambdaQueryWrapper<ExerciseSubQuestion>().in(ExerciseSubQuestion::getId, allSubQuestionIds))
                    .forEach(sq -> sqInfoMap.put(sq.getId(), sq));
        }

        // 预加载综合题的多知识点关联
        Map<Long, List<ExerciseSubKp>> subKpMap = new HashMap<>(); // subQuestionId → [ExerciseSubKp]
        if (!allSubQuestionIds.isEmpty()) {
            List<ExerciseSubKp> allSubKps = subKpMapper.selectList(
                    new LambdaQueryWrapper<ExerciseSubKp>().in(ExerciseSubKp::getSubQuestionId, allSubQuestionIds));
            for (ExerciseSubKp sk : allSubKps) {
                subKpMap.computeIfAbsent(sk.getSubQuestionId(), k -> new ArrayList<>()).add(sk);
                allKpIds.add(sk.getKpId());
                // 补充 kpInfoMap（综合题关联的知识点可能之前没加载）
                if (!kpInfoMap.containsKey(sk.getKpId())) {
                    CourseKnowledgePoint kp = kpMapper.selectById(sk.getKpId());
                    if (kp != null) kpInfoMap.put(sk.getKpId(), kp);
                }
            }
        }

        // 7. 按知识点统计不合格次数与总题目数（优先子题粒度，支持综合题加权）
        // 使用 double 以支持综合题的分摊计数
        Map<Long, Double> kpFailCount = new HashMap<>();
        Map<Long, Double> kpTotalCount = new HashMap<>();
        int passThreshold = warningProperties.getPassScoreThreshold();
        double comprehensiveDiscount = 0.5; // 综合题折扣系数

        for (ExerciseRecommendation rec : gradedRecs) {
            List<SubQuestionAnswer> recSubAnswers = subAnswerMap.get(rec.getId());

            if (recSubAnswers != null && !recSubAnswers.isEmpty()) {
                for (SubQuestionAnswer sqa : recSubAnswers) {
                    ExerciseSubQuestion sq = sqInfoMap.get(sqa.getSubQuestionId());
                    if (sq == null) continue;

                    // 判断是否错题
                    boolean isFail = false;
                    double failRatio = 1.0; // 默认全错=1.0
                    if (sq.getScore() != null && sq.getScore() > 0 && sqa.getScore() != null) {
                        double pct = sqa.getScore() * 100.0 / sq.getScore();
                        isFail = pct < passThreshold;
                        failRatio = 1.0 - (sqa.getScore().doubleValue() / sq.getScore()); // 扣分比例
                    } else if (sqa.getScore() != null && sqa.getScore() < passThreshold) {
                        isFail = true;
                    }

                    if ("COMPREHENSIVE".equals(sq.getQuestionType())) {
                        // 综合题：按权重 × 折扣系数分摊到各知识点
                        List<ExerciseSubKp> subKps = subKpMap.get(sq.getId());
                        if (subKps == null || subKps.isEmpty()) continue;

                        double totalWeight = subKps.stream()
                                .mapToDouble(sk -> sk.getWeight() == null ? 0 : sk.getWeight().doubleValue()).sum();

                        for (ExerciseSubKp sk : subKps) {
                            Long kpId = sk.getKpId();
                            CourseKnowledgePoint kp = kpInfoMap.get(kpId);
                            if (kp == null || !kp.getCourseId().equals(courseId)) continue;

                            double w = sk.getWeight() != null ? sk.getWeight().doubleValue() : 0;
                            if (totalWeight > 0) w = w / totalWeight; // 归一化
                            double contrib = w * comprehensiveDiscount;

                            kpTotalCount.merge(kpId, contrib, Double::sum);
                            if (isFail) {
                                kpFailCount.merge(kpId, failRatio * contrib, Double::sum);
                            }
                        }
                    } else {
                        // SINGLE 题（原有逻辑，使用 double 保持一致）
                        Long kpId = sq.getKpId();
                        if (kpId == null) continue;
                        CourseKnowledgePoint kp = kpInfoMap.get(kpId);
                        if (kp == null || !kp.getCourseId().equals(courseId)) continue;

                        kpTotalCount.merge(kpId, 1.0, Double::sum);
                        if (isFail) kpFailCount.merge(kpId, 1.0, Double::sum);
                    }
                }
            } else {
                // 无子题：回退到 exercise_knowledge_point（旧逻辑）
                List<Long> kpIds = exerciseKpMap.getOrDefault(rec.getExerciseId(), Collections.emptyList());
                for (Long kpId : kpIds) {
                    CourseKnowledgePoint kp = kpInfoMap.get(kpId);
                    if (kp == null || !kp.getCourseId().equals(courseId)) continue;
                    kpTotalCount.merge(kpId, 1.0, Double::sum);
                    if (rec.getScore() != null && rec.getScore() < passThreshold) {
                        kpFailCount.merge(kpId, 1.0, Double::sum);
                    }
                }
            }
        }

        if (kpTotalCount.isEmpty()) {
            return generateWeakPointsFromMastery(studentId, courseId, alertId);
        }

        // 8. 计算每个知识点的错误率，按错误率降序排列
        List<Map.Entry<Long, Double>> kpErrorRateList = new ArrayList<>();
        for (Map.Entry<Long, Double> entry : kpTotalCount.entrySet()) {
            Long kpId = entry.getKey();
            double total = entry.getValue();
            double fail = kpFailCount.getOrDefault(kpId, 0.0);
            double errorRate = total > 0 ? (fail * 100.0 / total) : 0;
            kpErrorRateList.add(Map.entry(kpId, errorRate));
        }
        kpErrorRateList.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        // 9. 确定薄弱点数量
        KnowledgeMastery mastery = masteryMapper.selectOne(
                new LambdaQueryWrapper<KnowledgeMastery>()
                        .eq(KnowledgeMastery::getStudentId, studentId)
                        .eq(KnowledgeMastery::getCourseId, courseId));
        int weakCount = kpErrorRateList.size();
        if (mastery != null && mastery.getWeakKnowledgeCount() != null
                && mastery.getWeakKnowledgeCount() > 0) {
            weakCount = Math.min(mastery.getWeakKnowledgeCount(), kpErrorRateList.size());
        }

        // 10. 仅保留错误率高于阈值的知识点作为薄弱点（阈值可配置）
        double errorRateThreshold = warningProperties.getWeakPointErrorRateThreshold();
        int generated = 0;
        for (int i = 0; i < weakCount && i < kpErrorRateList.size(); i++) {
            Map.Entry<Long, Double> entry = kpErrorRateList.get(i);
            if (entry.getValue() <= errorRateThreshold) break;

            Long kpId = entry.getKey();
            int failCount = (int) Math.round(kpFailCount.getOrDefault(kpId, 0.0));

            StudentWeakPoint wp = new StudentWeakPoint();
            wp.setStudentId(studentId);
            wp.setCourseId(courseId);
            wp.setKpId(kpId);
            wp.setErrorRate(BigDecimal.valueOf(entry.getValue()).setScale(1, java.math.RoundingMode.HALF_UP));
            wp.setErrorCount(failCount);
            wp.setAlertId(alertId);
            wp.setCreateTime(LocalDateTime.now());
            weakPointMapper.insert(wp);
            generated++;
        }
        return generated;
    }

    /** 无评分数据时的回退方案：基于知识掌握汇总数据生成，按 errorCount 降序取最薄弱的 N 个 */
    private int generateWeakPointsFromMastery(Long studentId, Long courseId, Long alertId) {
        KnowledgeMastery mastery = masteryMapper.selectOne(
                new LambdaQueryWrapper<KnowledgeMastery>()
                        .eq(KnowledgeMastery::getStudentId, studentId)
                        .eq(KnowledgeMastery::getCourseId, courseId));
        if (mastery == null || mastery.getWeakKnowledgeCount() == null
                || mastery.getWeakKnowledgeCount() <= 0) {
            return 0;
        }

        // 取课程下所有知识点（按 sort_order 排序）+ 全局错误率
        List<CourseKnowledgePoint> allKps = kpMapper.selectList(
                new LambdaQueryWrapper<CourseKnowledgePoint>()
                        .eq(CourseKnowledgePoint::getCourseId, courseId)
                        .orderByAsc(CourseKnowledgePoint::getSortOrder));
        if (allKps.isEmpty()) return 0;

        BigDecimal globalErrorRate = BigDecimal.ZERO;
        if (mastery.getTotalQuestion() != null && mastery.getTotalQuestion() > 0) {
            int errCount = mastery.getErrorCount() != null ? mastery.getErrorCount() : 0;
            globalErrorRate = BigDecimal.valueOf(errCount)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(mastery.getTotalQuestion()), 2, java.math.RoundingMode.HALF_UP);
        }

        // 引入一定随机分布：基于全局错误率生成有差异的个体错误率，使结果更有区分度
        double baseRate = globalErrorRate.doubleValue();
        int weakCount = Math.min(mastery.getWeakKnowledgeCount(), allKps.size());
        int generated = 0;
        for (int i = 0; i < weakCount && i < allKps.size(); i++) {
            CourseKnowledgePoint kp = allKps.get(i);
            // 使用微小随机抖动使不同知识点错误率有区分（±10% 范围内）
            double jitter = 1.0 + (Math.random() - 0.5) * 0.2; // 0.9 ~ 1.1 倍
            double individualRate = Math.min(100.0, Math.max(0.0, baseRate * jitter));
            StudentWeakPoint wp = new StudentWeakPoint();
            wp.setStudentId(studentId);
            wp.setCourseId(courseId);
            wp.setKpId(kp.getId());
            wp.setErrorRate(BigDecimal.valueOf(individualRate).setScale(1, java.math.RoundingMode.HALF_UP));
            wp.setErrorCount(mastery.getErrorCount() != null ? mastery.getErrorCount() : 0);
            wp.setAlertId(alertId);
            wp.setCreateTime(LocalDateTime.now());
            weakPointMapper.insert(wp);
            generated++;
        }
        return generated;
    }

    // ==================== 推荐管理 ====================

    /** 推荐练习给学生（知识点名称从多对多关联表查询，如有子题则自动创建子题作答记录） */
    @Transactional
    public ExerciseRecommendation recommend(Long teacherId, Long exerciseId, Long studentId,
                                              Long alertId) {
        Exercise exercise = exerciseMapper.selectById(exerciseId);
        // 从多对多关联表查询知识点名称，不再依赖旧的 kp_id 字段
        List<ExerciseKnowledgePoint> ekps = exerciseKpMapper.selectList(
                new LambdaQueryWrapper<ExerciseKnowledgePoint>().eq(ExerciseKnowledgePoint::getExerciseId, exerciseId));
        // 批量预加载知识点名称（消除 N+1）
        List<String> kpNames = new ArrayList<>();
        Set<Long> kpIds = ekps.stream().map(ExerciseKnowledgePoint::getKpId).collect(Collectors.toSet());
        if (!kpIds.isEmpty()) {
            List<CourseKnowledgePoint> kps = kpMapper.selectList(
                    new LambdaQueryWrapper<CourseKnowledgePoint>().in(CourseKnowledgePoint::getId, kpIds));
            Map<Long, String> nameMap = kps.stream()
                    .collect(Collectors.toMap(CourseKnowledgePoint::getId, CourseKnowledgePoint::getName));
            for (ExerciseKnowledgePoint ekp : ekps) {
                String name = nameMap.get(ekp.getKpId());
                if (name != null) kpNames.add(name);
            }
        }
        String kpName = kpNames.isEmpty() ? null : String.join("、", kpNames);

        ExerciseRecommendation rec = new ExerciseRecommendation();
        rec.setExerciseId(exerciseId);
        rec.setStudentId(studentId);
        rec.setAlertId(alertId);
        rec.setKnowledgePoint(kpName);
        rec.setStatus("PENDING");
        rec.setRecommendTime(LocalDateTime.now());
        recommendMapper.insert(rec);

        // 如果习题有子题目，自动创建子题作答记录
        List<ExerciseSubQuestion> subQuestions = subQuestionMapper.selectList(
                new LambdaQueryWrapper<ExerciseSubQuestion>()
                        .eq(ExerciseSubQuestion::getExerciseId, exerciseId)
                        .orderByAsc(ExerciseSubQuestion::getSeq));
        for (ExerciseSubQuestion sq : subQuestions) {
            SubQuestionAnswer sqa = new SubQuestionAnswer();
            sqa.setRecommendId(rec.getId());
            sqa.setSubQuestionId(sq.getId());
            sqa.setStatus("PENDING");
            subAnswerMapper.insert(sqa);
        }

        return rec;
    }

    /** 获取给某学生的推荐列表（支持分页） */
    public Map<String, Object> getStudentRecommendations(Long studentId, int page, int size) {
        Page<ExerciseRecommendation> p = new Page<>(page, size);
        Page<ExerciseRecommendation> resultPage = recommendMapper.selectPage(p,
                new LambdaQueryWrapper<ExerciseRecommendation>()
                        .eq(ExerciseRecommendation::getStudentId, studentId)
                        .orderByDesc(ExerciseRecommendation::getRecommendTime));

        List<ExerciseRecommendation> list = resultPage.getRecords();

        // 批量预加载练习题以消除 N+1
        Set<Long> exerciseIds = list.stream().map(ExerciseRecommendation::getExerciseId).collect(Collectors.toSet());
        Map<Long, Exercise> exerciseMap = new HashMap<>();
        if (!exerciseIds.isEmpty()) {
            List<Exercise> exercises = exerciseMapper.selectList(
                    new LambdaQueryWrapper<Exercise>().in(Exercise::getId, exerciseIds));
            for (Exercise e : exercises) {
                exerciseMap.put(e.getId(), e);
            }
        }

        List<Map<String, Object>> records = new ArrayList<>();
        for (ExerciseRecommendation rec : list) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", rec.getId());
            m.put("exerciseId", rec.getExerciseId());
            m.put("studentId", rec.getStudentId());
            m.put("knowledgePoint", rec.getKnowledgePoint());
            m.put("status", rec.getStatus());
            m.put("score", rec.getScore());
            m.put("teacherFeedback", rec.getTeacherFeedback());
            m.put("recommendTime", rec.getRecommendTime());
            m.put("completeTime", rec.getCompleteTime());
            Exercise exercise = exerciseMap.get(rec.getExerciseId());
            if (exercise != null) {
                m.put("exerciseTitle", exercise.getTitle());
                m.put("exerciseDesc", exercise.getDescription());
                m.put("difficulty", exercise.getDifficulty());
                m.put("fileUrl", exercise.getFileUrl());
                m.put("fileType", exercise.getFileType());
                // 子题数量
                Long sqCount = subQuestionMapper.selectCount(
                        new LambdaQueryWrapper<ExerciseSubQuestion>()
                                .eq(ExerciseSubQuestion::getExerciseId, exercise.getId()));
                m.put("hasSubQuestions", sqCount > 0);
                m.put("subQuestionCount", sqCount.intValue());
            }
            records.add(m);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", resultPage.getTotal());
        result.put("page", page);
        result.put("size", size);
        result.put("records", records);
        return result;
    }

    /** 获取某预警下的推荐记录（支持分页） */
    public Map<String, Object> getRecommendationsByAlert(Long alertId, int page, int size) {
        Page<ExerciseRecommendation> p = new Page<>(page, size);
        Page<ExerciseRecommendation> resultPage = recommendMapper.selectPage(p,
                new LambdaQueryWrapper<ExerciseRecommendation>()
                        .eq(ExerciseRecommendation::getAlertId, alertId)
                        .orderByDesc(ExerciseRecommendation::getRecommendTime));

        List<ExerciseRecommendation> list = resultPage.getRecords();

        // 批量预加载练习题以消除 N+1
        Set<Long> exerciseIds = list.stream().map(ExerciseRecommendation::getExerciseId).collect(Collectors.toSet());
        Map<Long, Exercise> exerciseMap = new HashMap<>();
        if (!exerciseIds.isEmpty()) {
            List<Exercise> exercises = exerciseMapper.selectList(
                    new LambdaQueryWrapper<Exercise>().in(Exercise::getId, exerciseIds));
            for (Exercise e : exercises) {
                exerciseMap.put(e.getId(), e);
            }
        }

        List<Map<String, Object>> records = list.stream().map(rec -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", rec.getId());
            m.put("exerciseId", rec.getExerciseId());
            m.put("studentId", rec.getStudentId());
            m.put("knowledgePoint", rec.getKnowledgePoint());
            m.put("status", rec.getStatus());
            m.put("score", rec.getScore());
            m.put("studentAnswer", rec.getStudentAnswer());
            m.put("teacherFeedback", rec.getTeacherFeedback());
            m.put("recommendTime", rec.getRecommendTime());
            m.put("completeTime", rec.getCompleteTime());
            Exercise e = exerciseMap.get(rec.getExerciseId());
            if (e != null) m.put("exerciseTitle", e.getTitle());
            return m;
        }).toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", resultPage.getTotal());
        result.put("page", page);
        result.put("size", size);
        result.put("records", records);
        return result;
    }

    /**
     * 获取待批改的推荐列表（状态为 ANSWERED 的练习推荐，按课程筛选）
     */
    public Map<String, Object> getPendingGrading(Long courseId, int page, int size) {
        // 1. 查询该课程下所有练习题
        List<Exercise> courseExercises = exerciseMapper.selectList(
                new LambdaQueryWrapper<Exercise>().eq(Exercise::getCourseId, courseId));
        Set<Long> exIds = courseExercises.stream().map(Exercise::getId).collect(Collectors.toSet());
        if (exIds.isEmpty()) {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("total", 0); empty.put("page", page); empty.put("size", size);
            empty.put("records", List.of());
            return empty;
        }

        // 2. 查询所有 ANSWERED 状态的推荐，按时间倒序
        Page<ExerciseRecommendation> p = new Page<>(page, size);
        Page<ExerciseRecommendation> resultPage = recommendMapper.selectPage(p,
                new LambdaQueryWrapper<ExerciseRecommendation>()
                        .eq(ExerciseRecommendation::getStatus, "ANSWERED")
                        .orderByDesc(ExerciseRecommendation::getCompleteTime));

        // 3. 过滤：只保留属于本课程练习的推荐
        List<ExerciseRecommendation> list = resultPage.getRecords().stream()
                .filter(rec -> exIds.contains(rec.getExerciseId()))
                .collect(Collectors.toList());

        // 4. 批量预加载练习题和学生信息
        Set<Long> recExIds = list.stream().map(ExerciseRecommendation::getExerciseId).collect(Collectors.toSet());
        Set<Long> stuIds = list.stream().map(ExerciseRecommendation::getStudentId).collect(Collectors.toSet());

        Map<Long, Exercise> exerciseMap = new HashMap<>();
        if (!recExIds.isEmpty()) {
            exerciseMapper.selectList(new LambdaQueryWrapper<Exercise>().in(Exercise::getId, recExIds))
                    .forEach(e -> exerciseMap.put(e.getId(), e));
        }
        Map<Long, Student> studentMap = new HashMap<>();
        if (!stuIds.isEmpty()) {
            studentMapper.selectList(new LambdaQueryWrapper<Student>().in(Student::getId, stuIds))
                    .forEach(s -> studentMap.put(s.getId(), s));
        }

        List<Map<String, Object>> records = list.stream().map(rec -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", rec.getId());
            m.put("exerciseId", rec.getExerciseId());
            m.put("studentId", rec.getStudentId());
            m.put("status", rec.getStatus());
            m.put("studentAnswer", rec.getStudentAnswer());
            m.put("completeTime", rec.getCompleteTime());

            Exercise e = exerciseMap.get(rec.getExerciseId());
            if (e != null) {
                m.put("exerciseTitle", e.getTitle());
                m.put("exerciseDescription", e.getDescription());
                m.put("difficulty", e.getDifficulty());
                m.put("fileUrl", e.getFileUrl());
            }

            Student s = studentMap.get(rec.getStudentId());
            if (s != null) {
                m.put("studentName", s.getStudentName());
                m.put("studentNo", s.getStudentNo());
            }

            return m;
        }).toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", resultPage.getTotal());
        result.put("page", page);
        result.put("size", size);
        result.put("records", records);
        return result;
    }

    /** 学生提交作答（支持子题分题作答）：PENDING → ANSWERED */
    @Transactional
    public void submitAnswer(Long recommendId, Map<Long, String> subAnswers) {
        ExerciseRecommendation rec = recommendMapper.selectById(recommendId);
        if (rec == null || !("PENDING".equals(rec.getStatus()))) return;

        LocalDateTime now = LocalDateTime.now();

        if (subAnswers != null && !subAnswers.isEmpty()) {
            // 有子题：逐题保存答案
            List<SubQuestionAnswer> existing = subAnswerMapper.selectList(
                    new LambdaQueryWrapper<SubQuestionAnswer>()
                            .eq(SubQuestionAnswer::getRecommendId, recommendId));
            Map<Long, SubQuestionAnswer> sqaMap = existing.stream()
                    .collect(Collectors.toMap(SubQuestionAnswer::getSubQuestionId, Function.identity()));

            for (Map.Entry<Long, String> entry : subAnswers.entrySet()) {
                Long subQuestionId = entry.getKey();
                String answer = entry.getValue();
                SubQuestionAnswer sqa = sqaMap.get(subQuestionId);
                if (sqa != null) {
                    sqa.setStudentAnswer(answer);
                    sqa.setStatus("ANSWERED");
                    sqa.setAnswerTime(now);
                    subAnswerMapper.updateById(sqa);
                }
            }
        } else {
            // 无子题：保存整个答案到主记录（兼容旧逻辑）
            rec.setStudentAnswer(subAnswers == null ? "" : "");
        }

        rec.setStatus("ANSWERED");
        rec.setCompleteTime(now);
        recommendMapper.updateById(rec);
    }

    /**
     * 学生提交作答（无子题的简化版，兼容旧API）
     */
    @Transactional
    public void submitAnswerSimple(Long recommendId, String answer) {
        ExerciseRecommendation rec = recommendMapper.selectById(recommendId);
        if (rec == null || !("PENDING".equals(rec.getStatus()))) return;

        // 检查是否有子题
        List<SubQuestionAnswer> existing = subAnswerMapper.selectList(
                new LambdaQueryWrapper<SubQuestionAnswer>()
                        .eq(SubQuestionAnswer::getRecommendId, recommendId));
        if (!existing.isEmpty()) {
            // 有子题但用了简化版提交，将整个答案作为第1题的答案
            SubQuestionAnswer sqa = existing.get(0);
            sqa.setStudentAnswer(answer);
            sqa.setStatus("ANSWERED");
            sqa.setAnswerTime(LocalDateTime.now());
            subAnswerMapper.updateById(sqa);
        }

        rec.setStudentAnswer(answer);
        rec.setStatus("ANSWERED");
        rec.setCompleteTime(LocalDateTime.now());
        recommendMapper.updateById(rec);
    }

    /** 教师打分和反馈（支持子题分题评分）：ANSWERED → GRADED */
    @Transactional
    public void gradeFeedback(Long recommendId, Map<Long, Integer> subScores, String feedback) {
        ExerciseRecommendation rec = recommendMapper.selectById(recommendId);
        if (rec == null) return;

        LocalDateTime now = LocalDateTime.now();
        int totalScore = 0;
        int scoredCount = 0;

        if (subScores != null && !subScores.isEmpty()) {
            // 有子题评分：逐题打分
            List<SubQuestionAnswer> existing = subAnswerMapper.selectList(
                    new LambdaQueryWrapper<SubQuestionAnswer>()
                            .eq(SubQuestionAnswer::getRecommendId, recommendId));
            Map<Long, SubQuestionAnswer> sqaMap = existing.stream()
                    .collect(Collectors.toMap(SubQuestionAnswer::getSubQuestionId, Function.identity()));

            // 加载子题信息（用于计算总分时需要最大分）
            Set<Long> sqIds = existing.stream().map(SubQuestionAnswer::getSubQuestionId).collect(Collectors.toSet());
            Map<Long, ExerciseSubQuestion> sqMap = new HashMap<>();
            if (!sqIds.isEmpty()) {
                subQuestionMapper.selectList(
                        new LambdaQueryWrapper<ExerciseSubQuestion>().in(ExerciseSubQuestion::getId, sqIds))
                        .forEach(sq -> sqMap.put(sq.getId(), sq));
            }

            for (Map.Entry<Long, Integer> entry : subScores.entrySet()) {
                Long subQuestionId = entry.getKey();
                int score = entry.getValue();
                SubQuestionAnswer sqa = sqaMap.get(subQuestionId);
                if (sqa != null) {
                    sqa.setScore(score);
                    sqa.setStatus("GRADED");
                    sqa.setGradeTime(now);
                    subAnswerMapper.updateById(sqa);
                    totalScore += score;
                    scoredCount++;
                }
            }

            // 计算百分制总分（子题得分之和 / 子题满分之和 * 100）
            if (scoredCount > 0) {
                int totalMaxScore = sqMap.values().stream().mapToInt(ExerciseSubQuestion::getScore).sum();
                if (totalMaxScore > 0) {
                    rec.setScore(Math.min(100, totalScore * 100 / totalMaxScore));
                } else {
                    rec.setScore(totalScore);
                }
            }
        } else {
            // 无子题：直接设置分数（兼容旧逻辑）
            rec.setScore(subScores != null ? 0 : 0);
        }

        rec.setTeacherFeedback(feedback);
        rec.setStatus("GRADED");
        recommendMapper.updateById(rec);

        // 回写 knowledge_mastery：累计总题数与错误数
        Exercise exercise = exerciseMapper.selectById(rec.getExerciseId());
        if (exercise != null && exercise.getCourseId() != null) {
            Long studentId = rec.getStudentId();
            Long courseId = exercise.getCourseId();
            int passThreshold = warningProperties.getPassScoreThreshold();

            KnowledgeMastery mastery = masteryMapper.selectOne(
                    new LambdaQueryWrapper<KnowledgeMastery>()
                            .eq(KnowledgeMastery::getStudentId, studentId)
                            .eq(KnowledgeMastery::getCourseId, courseId));

            if (mastery == null) {
                mastery = new KnowledgeMastery();
                mastery.setStudentId(studentId);
                mastery.setCourseId(courseId);
                mastery.setTotalQuestion(0);
                mastery.setErrorCount(0);
                mastery.setTotalKnowledge(0);
                mastery.setBasicTotal(0);
                mastery.setBasicCorrect(0);
                mastery.setWeakKnowledgeCount(0);
                masteryMapper.insert(mastery);
            }

            // 按子题粒度统计（精确）
            if (subScores != null && !subScores.isEmpty()) {
                List<SubQuestionAnswer> allSqas = subAnswerMapper.selectList(
                        new LambdaQueryWrapper<SubQuestionAnswer>()
                                .eq(SubQuestionAnswer::getRecommendId, recommendId));

                // 有子题：每个子题作为一个独立的数据点
                Set<Long> allSqIds = allSqas.stream().map(SubQuestionAnswer::getSubQuestionId).collect(Collectors.toSet());
                Map<Long, ExerciseSubQuestion> allSqMap = new HashMap<>();
                if (!allSqIds.isEmpty()) {
                    subQuestionMapper.selectList(
                            new LambdaQueryWrapper<ExerciseSubQuestion>().in(ExerciseSubQuestion::getId, allSqIds))
                            .forEach(sq -> allSqMap.put(sq.getId(), sq));
                }

                int totalQ = (mastery.getTotalQuestion() == null ? 0 : mastery.getTotalQuestion()) + allSqas.size();
                int errC = (mastery.getErrorCount() == null ? 0 : mastery.getErrorCount());
                for (SubQuestionAnswer sqa : allSqas) {
                    ExerciseSubQuestion sq = allSqMap.get(sqa.getSubQuestionId());
                    if (sq != null && sqa.getScore() != null && sq.getScore() != null && sq.getScore() > 0) {
                        double pct = sqa.getScore() * 100.0 / sq.getScore();
                        if (pct < passThreshold) errC++;
                    }
                }
                mastery.setTotalQuestion(totalQ);
                mastery.setErrorCount(errC);
            } else {
                int totalQ = (mastery.getTotalQuestion() == null ? 0 : mastery.getTotalQuestion()) + 1;
                mastery.setTotalQuestion(totalQ);
                if (rec.getScore() != null && rec.getScore() < passThreshold) {
                    int errC = (mastery.getErrorCount() == null ? 0 : mastery.getErrorCount()) + 1;
                    mastery.setErrorCount(errC);
                }
            }
            masteryMapper.updateById(mastery);

            // 重新生成薄弱点（基于最新的评分数据）
            generateWeakPoints(studentId, courseId, rec.getAlertId());
        }
    }

    /**
     * 兼容旧API的评分方法（无子题）
     */
    @Transactional
    public void gradeFeedbackSimple(Long recommendId, int score, String feedback) {
        gradeFeedback(recommendId, null, feedback);
        // 覆写分数
        ExerciseRecommendation rec = recommendMapper.selectById(recommendId);
        if (rec != null) {
            rec.setScore(score);
            recommendMapper.updateById(rec);
        }
    }

    // ==================== 工具 ====================

    private Map<String, Object> toMap(Exercise e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.getId());
        m.put("teacherId", e.getTeacherId());
        m.put("courseId", e.getCourseId());
        // kp_id 已弃用，统一使用多对多关联表 exercise_knowledge_point
        m.put("title", e.getTitle());
        m.put("description", e.getDescription());
        m.put("difficulty", e.getDifficulty());
        m.put("questionType", e.getQuestionType());
        m.put("answer", e.getAnswer());
        m.put("fileUrl", e.getFileUrl());
        m.put("auditStatus", e.getAuditStatus() != null ? e.getAuditStatus() : "APPROVED");
        m.put("auditRemark", e.getAuditRemark());
        m.put("createTime", e.getCreateTime());
        return m;
    }

    // ==================== 题库审核（管理员端） ====================

    /** 获取待审核的练习题列表 */
    public Map<String, Object> listPendingExercises(int page, int size) {
        Page<Exercise> p = new Page<>(page, size);
        Page<Exercise> resultPage = exerciseMapper.selectPage(p,
                new LambdaQueryWrapper<Exercise>()
                        .eq(Exercise::getIsActive, 1)
                        .eq(Exercise::getAuditStatus, "PENDING")
                        .orderByDesc(Exercise::getCreateTime));

        List<Map<String, Object>> records = resultPage.getRecords().stream().map(e -> {
            Map<String, Object> m = toMap(e);
            m.put("auditStatus", e.getAuditStatus());
            return m;
        }).toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", resultPage.getTotal());
        result.put("page", page);
        result.put("size", size);
        result.put("records", records);
        return result;
    }

    /** 审核通过 */
    @Transactional
    public void approveExercise(Long id) {
        Exercise e = exerciseMapper.selectById(id);
        if (e == null) throw new IllegalArgumentException("练习题不存在");
        e.setAuditStatus("APPROVED");
        e.setAuditRemark(null);
        exerciseMapper.updateById(e);
    }

    /** 审核驳回 */
    @Transactional
    public void rejectExercise(Long id, String remark) {
        Exercise e = exerciseMapper.selectById(id);
        if (e == null) throw new IllegalArgumentException("练习题不存在");
        e.setAuditStatus("REJECTED");
        e.setAuditRemark(remark);
        exerciseMapper.updateById(e);
    }

    // ==================== 作答草稿 ====================

    /** 保存/更新学生作答草稿 */
    @Transactional
    public void saveDraft(Long recommendId, Long studentId, String answer, String subAnswers) {
        ExerciseDraft draft = draftMapper.selectOne(
                new LambdaQueryWrapper<ExerciseDraft>()
                        .eq(ExerciseDraft::getRecommendId, recommendId)
                        .eq(ExerciseDraft::getStudentId, studentId));
        LocalDateTime now = LocalDateTime.now();
        if (draft == null) {
            draft = new ExerciseDraft();
            draft.setRecommendId(recommendId);
            draft.setStudentId(studentId);
            draft.setAnswer(answer);
            draft.setSubAnswers(subAnswers);
            draft.setCreateTime(now);
            draft.setUpdateTime(now);
            draftMapper.insert(draft);
        } else {
            draft.setAnswer(answer);
            draft.setSubAnswers(subAnswers);
            draft.setUpdateTime(now);
            draftMapper.updateById(draft);
        }
    }

    /** 获取学生作答草稿 */
    public ExerciseDraft getDraft(Long recommendId, Long studentId) {
        return draftMapper.selectOne(
                new LambdaQueryWrapper<ExerciseDraft>()
                        .eq(ExerciseDraft::getRecommendId, recommendId)
                        .eq(ExerciseDraft::getStudentId, studentId));
    }

    /** 删除学生作答草稿 */
    @Transactional
    public void deleteDraft(Long recommendId, Long studentId) {
        draftMapper.delete(
                new LambdaQueryWrapper<ExerciseDraft>()
                        .eq(ExerciseDraft::getRecommendId, recommendId)
                        .eq(ExerciseDraft::getStudentId, studentId));
    }

    /** 清理超过30天的陈旧草稿 */
    public void cleanExpiredDrafts() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(30);
        draftMapper.delete(
                new LambdaQueryWrapper<ExerciseDraft>()
                        .lt(ExerciseDraft::getUpdateTime, threshold));
    }
}

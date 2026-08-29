package com.example.academic_risk_warning.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.academic_risk_warning.entity.*;
import com.example.academic_risk_warning.mapper.*;
import com.example.academic_risk_warning.vo.*;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 按教师 ID 查询所负责班级学生的全套学情（对齐 study_warning_system 库表结构）
 */
@Service
public class StudentQueryService {

    private final TeacherClassMapper teacherClassMapper;
    private final StudentMapper studentMapper;
    private final CourseMapper courseMapper;
    private final ScoreInfoMapper scoreInfoMapper;
    private final HomeworkInfoMapper homeworkInfoMapper;
    private final ClassPerformanceMapper classPerformanceMapper;
    private final HistoryRiskMapper historyRiskMapper;
    private final KnowledgeMasteryMapper knowledgeMasteryMapper;
    private final StudentCourseMapper studentCourseMapper;
    private final AlertRecordMapper alertRecordMapper;
    private final StudentWeakPointMapper studentWeakPointMapper;
    private final CourseKnowledgePointMapper courseKpMapper;

    public StudentQueryService(TeacherClassMapper teacherClassMapper,
                               StudentMapper studentMapper,
                               CourseMapper courseMapper,
                               ScoreInfoMapper scoreInfoMapper,
                               HomeworkInfoMapper homeworkInfoMapper,
                               ClassPerformanceMapper classPerformanceMapper,
                               HistoryRiskMapper historyRiskMapper,
                               KnowledgeMasteryMapper knowledgeMasteryMapper,
                               StudentCourseMapper studentCourseMapper,
                               AlertRecordMapper alertRecordMapper,
                               StudentWeakPointMapper studentWeakPointMapper,
                               CourseKnowledgePointMapper courseKpMapper) {
        this.teacherClassMapper = teacherClassMapper;
        this.studentMapper = studentMapper;
        this.courseMapper = courseMapper;
        this.scoreInfoMapper = scoreInfoMapper;
        this.homeworkInfoMapper = homeworkInfoMapper;
        this.classPerformanceMapper = classPerformanceMapper;
        this.historyRiskMapper = historyRiskMapper;
        this.knowledgeMasteryMapper = knowledgeMasteryMapper;
        this.studentCourseMapper = studentCourseMapper;
        this.alertRecordMapper = alertRecordMapper;
        this.studentWeakPointMapper = studentWeakPointMapper;
        this.courseKpMapper = courseKpMapper;
    }

    /**
     * 根据 teacher_id：teacher_class.class_name → student → 各学情子表
     * 通过 student_course 关联表确定学生应修课程，只返回关联课程的数据
     * 
     * 分配规则：
     * 1. 如果 teacher_class 有范围（student_id_start/end）→ 只返回范围内的学生
     * 2. 如果 teacher_class 无范围但同班级有多个教师 → 平均分配
     * 3. 如果 teacher_class 无范围且单教师 → 返回该班级所有学生
     */
    public List<StudentFullVO> listStudentsByTeacherId(Long teacherId) {
        if (teacherId == null) {
            throw new IllegalArgumentException("教师ID不能为空");
        }

        // 1. 获取当前教师的所有班级分配
        List<TeacherClass> myAssignments = teacherClassMapper.selectList(
                new LambdaQueryWrapper<TeacherClass>()
                        .eq(TeacherClass::getTeacherId, teacherId)
        );

        if (myAssignments.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 分离有范围和无范围的分配
        List<TeacherClass> rangedAssignments = new ArrayList<>();
        List<TeacherClass> unRangedAssignments = new ArrayList<>();
        
        for (TeacherClass tc : myAssignments) {
            if (tc.getStudentIdStart() != null && tc.getStudentIdEnd() != null) {
                rangedAssignments.add(tc);
            } else {
                unRangedAssignments.add(tc);
            }
        }

        // 3. 收集所有涉及班级的名称
        List<String> allClassNames = myAssignments.stream()
                .map(TeacherClass::getClassName)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (allClassNames.isEmpty()) {
            return Collections.emptyList();
        }

        // 4. 查询所有涉及班级的学生
        List<Student> students = studentMapper.selectList(
                new LambdaQueryWrapper<Student>()
                        .in(Student::getClassName, allClassNames)
                        .orderByAsc(Student::getClassName, Student::getStudentNo)
        );

        if (students.isEmpty()) {
            return Collections.emptyList();
        }

        // 5. 构建有范围班级的范围映射
        Map<String, long[]> classRangeMap = new HashMap<>();
        for (TeacherClass tc : rangedAssignments) {
            classRangeMap.put(tc.getClassName(), 
                    new long[]{tc.getStudentIdStart(), tc.getStudentIdEnd()});
        }

        // 6. 构建无范围班级的教师列表映射（用于平均分配）
        Map<String, List<TeacherClass>> unRangedClassTeachers = new HashMap<>();
        for (TeacherClass tc : unRangedAssignments) {
            unRangedClassTeachers.computeIfAbsent(tc.getClassName(), k -> new ArrayList<>()).add(tc);
        }

        // 7. 最终结果列表（存储筛选后的学生）
        List<Student> filteredStudents = new ArrayList<>();

        // 8. 按班级处理
        Map<String, List<Student>> studentsByClass = students.stream()
                .collect(Collectors.groupingBy(Student::getClassName));

        for (Map.Entry<String, List<Student>> entry : studentsByClass.entrySet()) {
            String className = entry.getKey();
            List<Student> classStudents = entry.getValue();

            // 有范围的班级：按范围过滤
            if (classRangeMap.containsKey(className)) {
                long[] range = classRangeMap.get(className);
                for (Student s : classStudents) {
                    if (isInRange(s, range[0], range[1])) {
                        filteredStudents.add(s);
                    }
                }
            }
            // 无范围的班级：平均分配
            else if (unRangedClassTeachers.containsKey(className)) {
                List<TeacherClass> teachersForClass = unRangedClassTeachers.get(className);
                
                if (teachersForClass.size() == 1) {
                    // 单教师：该班级所有学生
                    filteredStudents.addAll(classStudents);
                } else {
                    // 多教师：平均分配
                    int teacherCount = teachersForClass.size();
                    int teacherIndex = -1;
                    for (int i = 0; i < teachersForClass.size(); i++) {
                        if (teachersForClass.get(i).getTeacherId().equals(teacherId)) {
                            teacherIndex = i;
                            break;
                        }
                    }
                    
                    if (teacherIndex >= 0) {
                        int total = classStudents.size();
                        int perTeacher = (total + teacherCount - 1) / teacherCount;
                        int start = teacherIndex * perTeacher;
                        int end = Math.min(start + perTeacher, total);
                        filteredStudents.addAll(classStudents.subList(start, end));
                    }
                }
            }
        }

        students = filteredStudents;


        Map<Long, String> courseNameMap = courseMapper.selectList(null).stream()
                .collect(Collectors.toMap(Course::getId, Course::getCourseName, (a, b) -> a));
        
        // 获取课程Map（用于判断前序课程）
        Map<Long, Course> courseMap = courseMapper.selectList(null).stream()
                .collect(Collectors.toMap(Course::getId, Function.identity(), (a, b) -> a));

        List<Long> studentIds = students.stream().map(Student::getId).toList();

        // 获取每个学生关联的课程ID列表（通过 student_course 表）
        Map<Long, Set<Long>> studentCourseMap = studentCourseMapper.selectList(
                new LambdaQueryWrapper<StudentCourse>().in(StudentCourse::getStudentId, studentIds)
        ).stream()
        .collect(Collectors.groupingBy(
                StudentCourse::getStudentId,
                Collectors.mapping(StudentCourse::getCourseId, Collectors.toSet())
        ));

        // 获取所有成绩数据，只保留学生关联课程的成绩
        List<ScoreInfo> allScores = scoreInfoMapper.selectList(
                new LambdaQueryWrapper<ScoreInfo>().in(ScoreInfo::getStudentId, studentIds)
        );
        Map<Long, List<ScoreInfo>> scoreMap = new HashMap<>();
        for (ScoreInfo score : allScores) {
            Long studentId = score.getStudentId();
            Long courseId = score.getCourseId();
            Set<Long> courses = studentCourseMap.getOrDefault(studentId, Set.of());
            if (courses.contains(courseId)) {
                scoreMap.computeIfAbsent(studentId, k -> new ArrayList<>()).add(score);
            }
        }

        // 获取所有作业数据，只保留学生关联课程的作业
        List<HomeworkInfo> allHomeworks = homeworkInfoMapper.selectList(
                new LambdaQueryWrapper<HomeworkInfo>().in(HomeworkInfo::getStudentId, studentIds)
        );
        Map<Long, List<HomeworkInfo>> homeworkMap = new HashMap<>();
        for (HomeworkInfo hw : allHomeworks) {
            Long studentId = hw.getStudentId();
            Long courseId = hw.getCourseId();
            Set<Long> courses = studentCourseMap.getOrDefault(studentId, Set.of());
            if (courses.contains(courseId)) {
                homeworkMap.computeIfAbsent(studentId, k -> new ArrayList<>()).add(hw);
            }
        }

        // 获取所有课堂表现数据，只保留学生关联课程的课堂表现
        List<ClassPerformance> allPerformances = classPerformanceMapper.selectList(
                new LambdaQueryWrapper<ClassPerformance>().in(ClassPerformance::getStudentId, studentIds)
        );
        Map<Long, List<ClassPerformance>> performanceMap = new HashMap<>();
        for (ClassPerformance cp : allPerformances) {
            Long studentId = cp.getStudentId();
            Long courseId = cp.getCourseId();
            Set<Long> courses = studentCourseMap.getOrDefault(studentId, Set.of());
            if (courses.contains(courseId)) {
                performanceMap.computeIfAbsent(studentId, k -> new ArrayList<>()).add(cp);
            }
        }

        // 获取所有历史风险数据，只保留学生关联课程的历史风险
        List<HistoryRisk> allRisks = historyRiskMapper.selectList(
                new LambdaQueryWrapper<HistoryRisk>().in(HistoryRisk::getStudentId, studentIds)
        );
        Map<Long, List<HistoryRisk>> riskMap = new HashMap<>();
        for (HistoryRisk risk : allRisks) {
            Long studentId = risk.getStudentId();
            Long courseId = risk.getCourseId();
            Set<Long> courses = studentCourseMap.getOrDefault(studentId, Set.of());
            if (courses.contains(courseId)) {
                riskMap.computeIfAbsent(studentId, k -> new ArrayList<>()).add(risk);
            }
        }

        // 获取所有知识点掌握数据，只保留学生关联课程的知识点掌握
        List<KnowledgeMastery> allMasteries = knowledgeMasteryMapper.selectList(
                new LambdaQueryWrapper<KnowledgeMastery>().in(KnowledgeMastery::getStudentId, studentIds)
        );
        Map<Long, List<KnowledgeMastery>> masteryMap = new HashMap<>();
        for (KnowledgeMastery km : allMasteries) {
            Long studentId = km.getStudentId();
            Long courseId = km.getCourseId();
            Set<Long> courses = studentCourseMap.getOrDefault(studentId, Set.of());
            if (courses.contains(courseId)) {
                masteryMap.computeIfAbsent(studentId, k -> new ArrayList<>()).add(km);
            }
        }

        // 获取薄弱知识点明细（student_weak_point），按 errorRate 降序
        List<StudentWeakPoint> allWeakPoints = studentWeakPointMapper.selectList(
                new LambdaQueryWrapper<StudentWeakPoint>().in(StudentWeakPoint::getStudentId, studentIds)
                        .orderByDesc(StudentWeakPoint::getErrorRate));
        // 批量查知识点名称
        Map<Long, String> kpNameMap = new HashMap<>();
        for (StudentWeakPoint wp : allWeakPoints) {
            if (!kpNameMap.containsKey(wp.getKpId())) {
                CourseKnowledgePoint kp = courseKpMapper.selectById(wp.getKpId());
                kpNameMap.put(wp.getKpId(), kp != null ? kp.getName() : "未知知识点");
            }
        }
        // 按学生分组，同时构建 per-student per-course 薄弱点计数
        Map<Long, List<StudentWeakPoint>> weakPointMap = new HashMap<>();
        Map<Long, Map<Long, Integer>> studentWeakCountMap = new HashMap<>(); // studentId -> courseId -> weakCount
        for (StudentWeakPoint wp : allWeakPoints) {
            Long sid2 = wp.getStudentId();
            Long cid2 = wp.getCourseId();
            Set<Long> scourses = studentCourseMap.getOrDefault(sid2, Set.of());
            if (scourses.contains(cid2)) {
                weakPointMap.computeIfAbsent(sid2, k -> new ArrayList<>()).add(wp);
                studentWeakCountMap.computeIfAbsent(sid2, k -> new HashMap<>()).merge(cid2, 1, Integer::sum);
            }
        }

        // 课程知识点总数（动态从 course_knowledge_point 获取，与教师知识点管理实时同步）
        Map<Long, Integer> courseKpCountMap = new HashMap<>();
        List<CourseKnowledgePoint> allKps = courseKpMapper.selectList(null);
        for (CourseKnowledgePoint kp : allKps) {
            courseKpCountMap.merge(kp.getCourseId(), 1, Integer::sum);
        }

        List<StudentFullVO> result = new ArrayList<>();
        for (Student student : students) {
            StudentFullVO vo = new StudentFullVO();
            vo.setBasicInfo(toBasicVO(student));
            
            // 当前学生的 per-course 薄弱点数
            Map<Long, Integer> scWeakCount = studentWeakCountMap.getOrDefault(student.getId(), Map.of());

            // 转换成绩列表并计算综合成绩（传入动态知识点总数和薄弱点数）
            List<ScoreInfoVO> scoreVOList = toScoreVOList(scoreMap.getOrDefault(student.getId(), List.of()), courseNameMap);
            calculateScores(scoreVOList, homeworkMap.getOrDefault(student.getId(), List.of()),
                    performanceMap.getOrDefault(student.getId(), List.of()),
                    masteryMap.getOrDefault(student.getId(), List.of()),
                    riskMap.getOrDefault(student.getId(), List.of()),
                    student.getGrade(), courseMap, courseKpCountMap, scWeakCount);
            vo.setScores(scoreVOList);
            
            vo.setHomeworkList(toHomeworkVOList(homeworkMap.getOrDefault(student.getId(), List.of()), courseNameMap));
            vo.setClassPerformances(toPerformanceVOList(performanceMap.getOrDefault(student.getId(), List.of()), courseNameMap));
            vo.setHistoryRisks(toRiskVOList(riskMap.getOrDefault(student.getId(), List.of()), courseNameMap));
            vo.setKnowledgeMasteries(toMasteryVOList(masteryMap.getOrDefault(student.getId(), List.of()), courseNameMap, courseKpCountMap, scWeakCount));

            // 设置薄弱知识点明细
            List<Map<String, Object>> wpList = new ArrayList<>();
            for (StudentWeakPoint wp : weakPointMap.getOrDefault(student.getId(), List.of())) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("kpId", wp.getKpId());
                m.put("kpName", kpNameMap.getOrDefault(wp.getKpId(), "未知"));
                m.put("courseId", wp.getCourseId());
                m.put("courseName", courseNameMap.getOrDefault(wp.getCourseId(), ""));
                m.put("errorRate", wp.getErrorRate());
                m.put("errorCount", wp.getErrorCount());
                wpList.add(m);
            }
            vo.setWeakPoints(wpList);

            result.add(vo);
        }
        return result;
    }

    /**
     * 获取教师名下的课程列表（通过 teacher_class → student → student_course → course 链路）
     * 返回每门课程的名称、学生数、活跃预警数
     */
    public List<TeacherCourseVO> listTeacherCourses(Long teacherId) {
        if (teacherId == null) {
            throw new IllegalArgumentException("教师ID不能为空");
        }

        // 复用学生筛选逻辑获取 studentIds
        List<Student> students = getTeacherStudents(teacherId);
        if (students.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> studentIds = students.stream().map(Student::getId).toList();

        // 查询所有课程
        Map<Long, Course> courseMap = courseMapper.selectList(null).stream()
                .collect(Collectors.toMap(Course::getId, Function.identity(), (a, b) -> a));

        // 查询 student_course 关联
        List<StudentCourse> studentCourses = studentCourseMapper.selectList(
                new LambdaQueryWrapper<StudentCourse>().in(StudentCourse::getStudentId, studentIds)
        );

        // 按课程分组统计学生数
        Map<Long, Long> courseStudentCount = studentCourses.stream()
                .collect(Collectors.groupingBy(StudentCourse::getCourseId, Collectors.counting()));

        // 查询 ACTIVE 状态的预警（通过 student_id + course_id）
        List<AlertRecord> activeAlerts = alertRecordMapper.selectList(
                new LambdaQueryWrapper<AlertRecord>()
                        .in(AlertRecord::getStudentId, studentIds)
                        .eq(AlertRecord::getStatus, "ACTIVE")
        );

        // 按课程统计预警数
        Map<Long, Long> courseAlertCount = new LinkedHashMap<>();
        for (AlertRecord alert : activeAlerts) {
            if (alert.getCourseId() != null) {
                courseAlertCount.merge(alert.getCourseId(), 1L, Long::sum);
            }
        }

        // 组装返回
        List<TeacherCourseVO> result = new ArrayList<>();
        for (Map.Entry<Long, Long> entry : courseStudentCount.entrySet()) {
            Long courseId = entry.getKey();
            Course course = courseMap.get(courseId);
            if (course == null) continue;

            TeacherCourseVO vo = new TeacherCourseVO();
            vo.setCourseId(courseId);
            vo.setCourseName(course.getCourseName());
            vo.setStudentCount(entry.getValue().intValue());
            vo.setAlertCount(courseAlertCount.getOrDefault(courseId, 0L).intValue());
            result.add(vo);
        }

        return result;
    }

    /**
     * 获取教师负责的所有学生（筛选逻辑与 listStudentsByTeacherId 一致）
     */
    private List<Student> getTeacherStudents(Long teacherId) {
        List<TeacherClass> myAssignments = teacherClassMapper.selectList(
                new LambdaQueryWrapper<TeacherClass>()
                        .eq(TeacherClass::getTeacherId, teacherId)
        );
        if (myAssignments.isEmpty()) {
            return Collections.emptyList();
        }

        List<TeacherClass> rangedAssignments = new ArrayList<>();
        List<TeacherClass> unRangedAssignments = new ArrayList<>();
        for (TeacherClass tc : myAssignments) {
            if (tc.getStudentIdStart() != null && tc.getStudentIdEnd() != null) {
                rangedAssignments.add(tc);
            } else {
                unRangedAssignments.add(tc);
            }
        }

        List<String> allClassNames = myAssignments.stream()
                .map(TeacherClass::getClassName)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (allClassNames.isEmpty()) {
            return Collections.emptyList();
        }

        List<Student> students = studentMapper.selectList(
                new LambdaQueryWrapper<Student>()
                        .in(Student::getClassName, allClassNames)
                        .orderByAsc(Student::getClassName, Student::getStudentNo)
        );
        if (students.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, long[]> classRangeMap = new HashMap<>();
        for (TeacherClass tc : rangedAssignments) {
            classRangeMap.put(tc.getClassName(),
                    new long[]{tc.getStudentIdStart(), tc.getStudentIdEnd()});
        }

        Map<String, List<TeacherClass>> unRangedClassTeachers = new HashMap<>();
        for (TeacherClass tc : unRangedAssignments) {
            unRangedClassTeachers.computeIfAbsent(tc.getClassName(), k -> new ArrayList<>()).add(tc);
        }

        Map<String, List<Student>> studentsByClass = students.stream()
                .collect(Collectors.groupingBy(Student::getClassName));

        List<Student> filteredStudents = new ArrayList<>();
        for (Map.Entry<String, List<Student>> entry : studentsByClass.entrySet()) {
            String className = entry.getKey();
            List<Student> classStudents = entry.getValue();

            if (classRangeMap.containsKey(className)) {
                long[] range = classRangeMap.get(className);
                for (Student s : classStudents) {
                    if (isInRange(s, range[0], range[1])) {
                        filteredStudents.add(s);
                    }
                }
            } else if (unRangedClassTeachers.containsKey(className)) {
                List<TeacherClass> teachersForClass = unRangedClassTeachers.get(className);
                if (teachersForClass.size() == 1) {
                    filteredStudents.addAll(classStudents);
                } else {
                    int teacherCount = teachersForClass.size();
                    int teacherIndex = -1;
                    for (int i = 0; i < teachersForClass.size(); i++) {
                        if (teachersForClass.get(i).getTeacherId().equals(teacherId)) {
                            teacherIndex = i;
                            break;
                        }
                    }
                    if (teacherIndex >= 0) {
                        int total = classStudents.size();
                        int perTeacher = (total + teacherCount - 1) / teacherCount;
                        int start = teacherIndex * perTeacher;
                        int end = Math.min(start + perTeacher, total);
                        filteredStudents.addAll(classStudents.subList(start, end));
                    }
                }
            }
        }

        return filteredStudents;
    }

    /**
     * 计算综合成绩
     * 根据学生年级和课程前序关系判断使用哪种评分体系：
     * - 学生年级为"大一上"：新生版评分体系（学业45%+作业25%+知识点16%+课堂14%）
     * - 学生年级非"大一上"，且课程有前序课程：老生通用版评分体系（学业26%+作业12%+知识点9%+课堂8%+历史风险45%）
     * - 学生年级非"大一上"，但课程无前序课程：新生版评分体系
     */
    private void calculateScores(List<ScoreInfoVO> scores, List<HomeworkInfo> homeworks,
                                 List<ClassPerformance> performances, List<KnowledgeMastery> masteries,
                                 List<HistoryRisk> historyRisks, String studentGrade,
                                 Map<Long, Course> courseMap,
                                 Map<Long, Integer> courseKpCountMap,
                                 Map<Long, Integer> studentCourseWeakCount) {
        if (scores == null || scores.isEmpty()) return;

        for (ScoreInfoVO score : scores) {
            Long courseId = score.getCourseId();

            // 获取课程信息
            Course course = courseMap.get(courseId);
            
            // 动态薄弱点数（从 student_weak_point 表获取）
            int weakCount = studentCourseWeakCount.getOrDefault(courseId, 0);

            // 判断使用哪套体系
            boolean useFreshmanSystem = isFreshmanSystem(studentGrade, course);

            if (useFreshmanSystem) {
                // 新生版评分体系
                score.setCalculatedScore(calculateFreshmanScore(courseId, score, homeworks, performances, masteries, courseKpCountMap, weakCount));
            } else {
                // 老生通用版评分体系
                score.setCalculatedScore(calculateSeniorScore(courseId, score, homeworks, performances, masteries, historyRisks));
            }
        }

        // 计算学生整体综合成绩
        double sum = 0;
        int count = 0;
        for (ScoreInfoVO s : scores) {
            if (s.getCalculatedScore() != null) {
                sum += s.getCalculatedScore();
                count++;
            }
        }
        double overall = (count > 0) ? Math.round(sum / count * 10) / 10.0 : null;
        for (ScoreInfoVO s : scores) {
            s.setOverallScore(overall);
        }
    }

    /**
     * 判断是否使用新生体系
     * 规则：
     * - 学生年级为"大一上"：新生体系
     * - 学生年级非"大一上"，但课程无前序课程：新生体系
     * - 学生年级非"大一上"，且课程有前序课程：老生体系
     */
    private boolean isFreshmanSystem(String studentGrade, Course course) {
        // 学生是大一上 → 新生体系
        if ("大一上".equals(studentGrade)) {
            return true;
        }
        
        // 学生非大一上 → 看课程是否有前序课程
        // 课程无前序课程ID → 新生体系
        if (course == null || course.getPrerequisiteCourseId() == null) {
            return true;
        }
        
        // 课程有前序课程 → 老生体系
        return false;
    }

    /**
     * 大一新生专属评分体系
     * 学业成绩 45%、作业完成 25%、知识点掌握 16%、课堂表现 14%
     */
    private Double calculateFreshmanScore(Long courseId, ScoreInfoVO score,
                                         List<HomeworkInfo> homeworks,
                                         List<ClassPerformance> performances,
                                         List<KnowledgeMastery> masteries,
                                         Map<Long, Integer> courseKpCountMap,
                                         int weakPointCount) {
        double total = 0.0;

        // ========== 学业成绩 45% ==========
        // 子指标：平时 20%、期中 30%、期末 50%
        double academicScore = 0;
        double us = (score.getUsualScore() != null) ? score.getUsualScore() : 0;
        double ms = (score.getMidScore() != null) ? score.getMidScore() : 0;
        double fs = (score.getFinalScore() != null) ? score.getFinalScore() : 0;
        // 如果期末为空，用平时和期中代替
        if (score.getFinalScore() == null) {
            academicScore = us * 0.4 + ms * 0.6;
        } else {
            academicScore = us * 0.20 + ms * 0.30 + fs * 0.50;
        }
        total += academicScore * 0.45;

        // ========== 作业完成 25% ==========
        // 子指标：提交率 50%、按时提交率 30%、作业均分 20%
        HomeworkInfo hw = homeworks.stream()
                .filter(h -> h.getCourseId().equals(courseId))
                .findFirst().orElse(null);
        double homeworkScore = 0;
        if (hw != null && hw.getTotalHomework() != null && hw.getTotalHomework() > 0) {
            double submitRate = hw.getSubmitCount() * 100.0 / hw.getTotalHomework(); // 提交率
            double ontimeRate = 0;
            if (hw.getTotalHomework() > 0 && hw.getLateSubmitCount() != null) {
                ontimeRate = (hw.getSubmitCount() - hw.getLateSubmitCount()) * 100.0 / hw.getTotalHomework(); // 按时提交率
            }
            double avgScoreRate = (hw.getAvgScore() != null) ? hw.getAvgScore() : 0; // 作业均分（已满分百分制）
            homeworkScore = submitRate * 0.50 + ontimeRate * 0.30 + avgScoreRate * 0.20;
        }
        total += homeworkScore * 0.25;

        // ========== 知识点掌握 16% ==========
        // 子指标：正确率 40%、基础知识正确率 30%、薄弱知识点占比 30%
        KnowledgeMastery km = masteries.stream()
                .filter(k -> k.getCourseId().equals(courseId))
                .findFirst().orElse(null);
        double knowledgeScore = 0;
        if (km != null) {
            double correctRate = 0;
            if (km.getTotalQuestion() != null && km.getTotalQuestion() > 0) {
                // B2修复: 正确率 = (总题数 - 错题数) / 总题数
                int errorCount = km.getErrorCount() != null ? km.getErrorCount() : 0;
                int totalCorrect = km.getTotalQuestion() - errorCount;
                correctRate = totalCorrect * 100.0 / km.getTotalQuestion();
            }
            double basicRate = 0;
            if (km.getBasicTotal() != null && km.getBasicTotal() > 0) {
                basicRate = (km.getBasicCorrect() != null ? km.getBasicCorrect() : 0) * 100.0 / km.getBasicTotal();
            }
            double weakRate = 100;
            // 动态总知识点数（来自 course_knowledge_point，与教师知识点管理同步）
            int totalKp = courseKpCountMap.getOrDefault(courseId, 0);
            if (totalKp > 0 && weakPointCount >= 0) {
                weakRate = (1 - weakPointCount * 1.0 / totalKp) * 100;
            }
            knowledgeScore = correctRate * 0.40 + basicRate * 0.30 + weakRate * 0.30;
        }
        total += knowledgeScore * 0.16;

        // ========== 课堂表现 14% ==========
        // 子指标：出勤率 40%、迟到率 20%、随堂测验 40%
        ClassPerformance cp = performances.stream()
                .filter(p -> p.getCourseId().equals(courseId))
                .findFirst().orElse(null);
        double classScore = 0;
        if (cp != null && cp.getTotalClassTimes() != null && cp.getTotalClassTimes() > 0) {
            double attendanceRate = (cp.getTotalClassTimes() - (cp.getAbsentCount() != null ? cp.getAbsentCount() : 0)) * 100.0 / cp.getTotalClassTimes();
            double ontimeRate = 100;
            if (cp.getTotalClassTimes() > 0 && cp.getLateCount() != null) {
                ontimeRate = (cp.getTotalClassTimes() - cp.getLateCount()) * 100.0 / cp.getTotalClassTimes();
            }
            double quizRate = (cp.getQuizScore() != null) ? cp.getQuizScore() : 0;
            classScore = attendanceRate * 0.40 + ontimeRate * 0.20 + quizRate * 0.40;
        }
        total += classScore * 0.14;

        return Math.round(total * 10) / 10.0;
    }

    /**
     * 老生通用版评分体系
     * 学业成绩 26%、作业完成 12%、知识点掌握 9%、课堂表现 8%、历史风险与学习稳定性 45%
     */
    private Double calculateSeniorScore(Long courseId, ScoreInfoVO score,
                                        List<HomeworkInfo> homeworks,
                                        List<ClassPerformance> performances,
                                        List<KnowledgeMastery> masteries,
                                        List<HistoryRisk> historyRisks) {
        double total = 0.0;

        // ========== 学业成绩 26% ==========
        double academicScore = (score.getFinalScore() != null) ? score.getFinalScore() : 0;
        total += academicScore * 0.26;

        // ========== 作业完成 12% ==========
        HomeworkInfo hw = homeworks.stream()
                .filter(h -> h.getCourseId().equals(courseId))
                .findFirst().orElse(null);
        double homeworkScore = 0;
        if (hw != null && hw.getTotalHomework() != null && hw.getTotalHomework() > 0) {
            homeworkScore = (hw.getSubmitCount() * 100.0 / hw.getTotalHomework());
        }
        total += homeworkScore * 0.12;

        // ========== 知识点掌握 9% ==========
        KnowledgeMastery km = masteries.stream()
                .filter(k -> k.getCourseId().equals(courseId))
                .findFirst().orElse(null);
        double knowledgeScore = 0;
        if (km != null && km.getTotalQuestion() != null && km.getTotalQuestion() > 0) {
            int correct = km.getBasicCorrect() != null ? km.getBasicCorrect() : 0;
            int totalQ = km.getTotalQuestion();
            knowledgeScore = (correct * 100.0 / totalQ);
        }
        total += knowledgeScore * 0.09;

        // ========== 课堂表现 8% ==========
        ClassPerformance cp = performances.stream()
                .filter(p -> p.getCourseId().equals(courseId))
                .findFirst().orElse(null);
        double classScore = 0;
        if (cp != null && cp.getTotalClassTimes() != null && cp.getTotalClassTimes() > 0) {
            int present = cp.getTotalClassTimes() - (cp.getAbsentCount() != null ? cp.getAbsentCount() : 0);
            double attendanceRate = present * 100.0 / cp.getTotalClassTimes();
            classScore = attendanceRate;
            if (cp.getQuizScore() != null) {
                classScore = classScore * 0.6 + cp.getQuizScore() * 0.4;
            }
        }
        total += classScore * 0.08;

        // ========== 历史风险与学习稳定性 45% ==========
        HistoryRisk risk = historyRisks.stream()
                .filter(r -> r.getCourseId().equals(courseId))
                .findFirst().orElse(null);
        double riskScore = 100;
        if (risk != null) {
            if (risk.getLastTermFailed() != null) {
                try {
                    int failed = Integer.parseInt(risk.getLastTermFailed());
                    riskScore -= failed * 20;
                } catch (NumberFormatException ignored) {}
            }
            if ("不稳定".equals(risk.getStudyStable())) {
                riskScore -= 15;
            }
            riskScore = Math.max(0, riskScore);
        }
        total += riskScore * 0.45;

        return Math.round(total * 10) / 10.0;
    }

    /**
     * 根据 student_id 列表查询全套学情（学生端自查询用）
     * 通过 student_course 关联表确定学生应修课程，只返回关联课程的数据
     */
    public List<StudentFullVO> listStudentsByIds(List<Long> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Student> students = studentMapper.selectList(
                new LambdaQueryWrapper<Student>()
                        .in(Student::getId, studentIds)
                        .orderByAsc(Student::getClassName, Student::getStudentNo)
        );
        if (students.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, String> courseNameMap = courseMapper.selectList(null).stream()
                .collect(Collectors.toMap(Course::getId, Course::getCourseName, (a, b) -> a));
        
        // 获取课程Map（用于判断前序课程）
        Map<Long, Course> courseMap = courseMapper.selectList(null).stream()
                .collect(Collectors.toMap(Course::getId, Function.identity(), (a, b) -> a));

        // 获取每个学生关联的课程ID列表（通过 student_course 表）
        Map<Long, Set<Long>> studentCourseMap = studentCourseMapper.selectList(
                new LambdaQueryWrapper<StudentCourse>().in(StudentCourse::getStudentId, studentIds)
        ).stream()
        .collect(Collectors.groupingBy(
                StudentCourse::getStudentId,
                Collectors.mapping(StudentCourse::getCourseId, Collectors.toSet())
        ));

        // 获取所有成绩数据，只保留学生关联课程的成绩
        List<ScoreInfo> allScores = scoreInfoMapper.selectList(
                new LambdaQueryWrapper<ScoreInfo>().in(ScoreInfo::getStudentId, studentIds)
        );
        Map<Long, List<ScoreInfo>> scoreMap = new HashMap<>();
        for (ScoreInfo score : allScores) {
            Long studentId = score.getStudentId();
            Long courseId = score.getCourseId();
            Set<Long> courses = studentCourseMap.getOrDefault(studentId, Set.of());
            if (courses.contains(courseId)) {
                scoreMap.computeIfAbsent(studentId, k -> new ArrayList<>()).add(score);
            }
        }

        // 获取所有作业数据，只保留学生关联课程的作业
        List<HomeworkInfo> allHomeworks = homeworkInfoMapper.selectList(
                new LambdaQueryWrapper<HomeworkInfo>().in(HomeworkInfo::getStudentId, studentIds)
        );
        Map<Long, List<HomeworkInfo>> homeworkMap = new HashMap<>();
        for (HomeworkInfo hw : allHomeworks) {
            Long studentId = hw.getStudentId();
            Long courseId = hw.getCourseId();
            Set<Long> courses = studentCourseMap.getOrDefault(studentId, Set.of());
            if (courses.contains(courseId)) {
                homeworkMap.computeIfAbsent(studentId, k -> new ArrayList<>()).add(hw);
            }
        }

        // 获取所有课堂表现数据，只保留学生关联课程的课堂表现
        List<ClassPerformance> allPerformances = classPerformanceMapper.selectList(
                new LambdaQueryWrapper<ClassPerformance>().in(ClassPerformance::getStudentId, studentIds)
        );
        Map<Long, List<ClassPerformance>> performanceMap = new HashMap<>();
        for (ClassPerformance cp : allPerformances) {
            Long studentId = cp.getStudentId();
            Long courseId = cp.getCourseId();
            Set<Long> courses = studentCourseMap.getOrDefault(studentId, Set.of());
            if (courses.contains(courseId)) {
                performanceMap.computeIfAbsent(studentId, k -> new ArrayList<>()).add(cp);
            }
        }

        // 获取所有历史风险数据，只保留学生关联课程的历史风险
        List<HistoryRisk> allRisks = historyRiskMapper.selectList(
                new LambdaQueryWrapper<HistoryRisk>().in(HistoryRisk::getStudentId, studentIds)
        );
        Map<Long, List<HistoryRisk>> riskMap = new HashMap<>();
        for (HistoryRisk risk : allRisks) {
            Long studentId = risk.getStudentId();
            Long courseId = risk.getCourseId();
            Set<Long> courses = studentCourseMap.getOrDefault(studentId, Set.of());
            if (courses.contains(courseId)) {
                riskMap.computeIfAbsent(studentId, k -> new ArrayList<>()).add(risk);
            }
        }

        // 获取所有知识点掌握数据，只保留学生关联课程的知识点掌握
        List<KnowledgeMastery> allMasteries = knowledgeMasteryMapper.selectList(
                new LambdaQueryWrapper<KnowledgeMastery>().in(KnowledgeMastery::getStudentId, studentIds)
        );
        Map<Long, List<KnowledgeMastery>> masteryMap = new HashMap<>();
        for (KnowledgeMastery km : allMasteries) {
            Long studentId = km.getStudentId();
            Long courseId = km.getCourseId();
            Set<Long> courses = studentCourseMap.getOrDefault(studentId, Set.of());
            if (courses.contains(courseId)) {
                masteryMap.computeIfAbsent(studentId, k -> new ArrayList<>()).add(km);
            }
        }

        // 获取薄弱知识点明细（student_weak_point），按 errorRate 降序
        List<StudentWeakPoint> allWeakPoints = studentWeakPointMapper.selectList(
                new LambdaQueryWrapper<StudentWeakPoint>().in(StudentWeakPoint::getStudentId, studentIds)
                        .orderByDesc(StudentWeakPoint::getErrorRate));
        // 批量查知识点名称
        Map<Long, String> kpNameMap = new HashMap<>();
        for (StudentWeakPoint wp : allWeakPoints) {
            if (!kpNameMap.containsKey(wp.getKpId())) {
                CourseKnowledgePoint kp = courseKpMapper.selectById(wp.getKpId());
                kpNameMap.put(wp.getKpId(), kp != null ? kp.getName() : "未知知识点");
            }
        }
        // 按学生分组，同时构建 per-student per-course 薄弱点计数
        Map<Long, List<StudentWeakPoint>> weakPointMap = new HashMap<>();
        Map<Long, Map<Long, Integer>> studentWeakCountMap = new HashMap<>(); // studentId -> courseId -> weakCount
        for (StudentWeakPoint wp : allWeakPoints) {
            Long sid2 = wp.getStudentId();
            Long cid2 = wp.getCourseId();
            Set<Long> scourses = studentCourseMap.getOrDefault(sid2, Set.of());
            if (scourses.contains(cid2)) {
                weakPointMap.computeIfAbsent(sid2, k -> new ArrayList<>()).add(wp);
                studentWeakCountMap.computeIfAbsent(sid2, k -> new HashMap<>()).merge(cid2, 1, Integer::sum);
            }
        }

        // 课程知识点总数（动态从 course_knowledge_point 获取，与教师知识点管理实时同步）
        Map<Long, Integer> courseKpCountMap = new HashMap<>();
        List<CourseKnowledgePoint> allKps = courseKpMapper.selectList(null);
        for (CourseKnowledgePoint kp : allKps) {
            courseKpCountMap.merge(kp.getCourseId(), 1, Integer::sum);
        }

        List<StudentFullVO> result = new ArrayList<>();
        for (Student student : students) {
            StudentFullVO vo = new StudentFullVO();
            vo.setBasicInfo(toBasicVO(student));

            // 当前学生的 per-course 薄弱点数
            Map<Long, Integer> scWeakCount = studentWeakCountMap.getOrDefault(student.getId(), Map.of());

            // 转换成绩列表并计算综合成绩（传入动态知识点总数和薄弱点数）
            List<ScoreInfoVO> scoreVOList = toScoreVOList(scoreMap.getOrDefault(student.getId(), List.of()), courseNameMap);
            calculateScores(scoreVOList, homeworkMap.getOrDefault(student.getId(), List.of()),
                    performanceMap.getOrDefault(student.getId(), List.of()),
                    masteryMap.getOrDefault(student.getId(), List.of()),
                    riskMap.getOrDefault(student.getId(), List.of()),
                    student.getGrade(), courseMap, courseKpCountMap, scWeakCount);
            vo.setScores(scoreVOList);

            vo.setHomeworkList(toHomeworkVOList(homeworkMap.getOrDefault(student.getId(), List.of()), courseNameMap));
            vo.setClassPerformances(toPerformanceVOList(performanceMap.getOrDefault(student.getId(), List.of()), courseNameMap));
            vo.setHistoryRisks(toRiskVOList(riskMap.getOrDefault(student.getId(), List.of()), courseNameMap));
            vo.setKnowledgeMasteries(toMasteryVOList(masteryMap.getOrDefault(student.getId(), List.of()), courseNameMap, courseKpCountMap, scWeakCount));

            // 设置薄弱知识点明细
            List<Map<String, Object>> wpList = new ArrayList<>();
            for (StudentWeakPoint wp : weakPointMap.getOrDefault(student.getId(), List.of())) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("kpId", wp.getKpId());
                m.put("kpName", kpNameMap.getOrDefault(wp.getKpId(), "未知"));
                m.put("courseId", wp.getCourseId());
                m.put("courseName", courseNameMap.getOrDefault(wp.getCourseId(), ""));
                m.put("errorRate", wp.getErrorRate());
                m.put("errorCount", wp.getErrorCount());
                wpList.add(m);
            }
            vo.setWeakPoints(wpList);

            result.add(vo);
        }
        return result;
    }

    private <T> Map<Long, List<T>> groupByStudentId(List<T> list, Function<T, Long> idGetter) {
        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptyMap();
        }
        return list.stream().collect(Collectors.groupingBy(idGetter));
    }

    private String courseName(Map<Long, String> courseNameMap, Long courseId) {
        if (courseId == null) {
            return null;
        }
        return courseNameMap.getOrDefault(courseId, "课程" + courseId);
    }

    private StudentBasicVO toBasicVO(Student student) {
        StudentBasicVO vo = new StudentBasicVO();
        BeanUtils.copyProperties(student, vo);
        return vo;
    }

    private List<ScoreInfoVO> toScoreVOList(List<ScoreInfo> list, Map<Long, String> courseNameMap) {
        return list.stream().map(item -> {
            ScoreInfoVO vo = new ScoreInfoVO();
            BeanUtils.copyProperties(item, vo);
            vo.setCourseName(courseName(courseNameMap, item.getCourseId()));
            return vo;
        }).toList();
    }

    private List<HomeworkInfoVO> toHomeworkVOList(List<HomeworkInfo> list, Map<Long, String> courseNameMap) {
        return list.stream().map(item -> {
            HomeworkInfoVO vo = new HomeworkInfoVO();
            BeanUtils.copyProperties(item, vo);
            vo.setCourseName(courseName(courseNameMap, item.getCourseId()));
            return vo;
        }).toList();
    }

    private List<ClassPerformanceVO> toPerformanceVOList(List<ClassPerformance> list, Map<Long, String> courseNameMap) {
        return list.stream().map(item -> {
            ClassPerformanceVO vo = new ClassPerformanceVO();
            BeanUtils.copyProperties(item, vo);
            vo.setCourseName(courseName(courseNameMap, item.getCourseId()));
            return vo;
        }).toList();
    }

    private List<HistoryRiskVO> toRiskVOList(List<HistoryRisk> list, Map<Long, String> courseNameMap) {
        return list.stream().map(item -> {
            HistoryRiskVO vo = new HistoryRiskVO();
            BeanUtils.copyProperties(item, vo);
            vo.setCourseName(courseName(courseNameMap, item.getCourseId()));
            vo.setGradeLevel(item.getGradeLevel());
            vo.setSummary(buildHistoryRiskSummary(item));
            return vo;
        }).toList();
    }

    /** 构建历史风险汇总描述 */
    private String buildHistoryRiskSummary(HistoryRisk risk) {
        String failed = "是".equals(risk.getLastTermFailed()) ? "有挂科" : "无挂科";
        String stable = switch (risk.getStudyStable()) {
            case "不稳定" -> "，学习不稳定";
            case "一般" -> "，学习一般";
            default -> "，学习稳定";
        };
        return failed + stable;
    }

    private List<KnowledgeMasteryVO> toMasteryVOList(List<KnowledgeMastery> list, Map<Long, String> courseNameMap,
                                                      Map<Long, Integer> courseKpCountMap, Map<Long, Integer> studentCourseWeakCount) {
        return list.stream().map(item -> {
            KnowledgeMasteryVO vo = new KnowledgeMasteryVO();
            BeanUtils.copyProperties(item, vo);
            vo.setCourseName(courseName(courseNameMap, item.getCourseId()));
            // 动态覆盖：总知识点数 = 该课程在 course_knowledge_point 表中的知识点总数（与教师管理同步）
            if (item.getCourseId() != null && courseKpCountMap.containsKey(item.getCourseId())) {
                vo.setTotalKnowledge(courseKpCountMap.get(item.getCourseId()));
            }
            // 动态覆盖：薄弱点数 = student_weak_point 表中该学生该课程的真实薄弱点数
            if (item.getCourseId() != null && studentCourseWeakCount.containsKey(item.getCourseId())) {
                vo.setWeakKnowledgeCount(studentCourseWeakCount.get(item.getCourseId()));
            } else {
                vo.setWeakKnowledgeCount(0);
            }
            return vo;
        }).toList();
    }

    /**
     * 检查学生是否在指定范围内
     * 优先使用学生ID比较，如果不在范围内则尝试用学号比较
     */
    private boolean isInRange(Student student, long start, long end) {
        // 尝试用学生ID比较
        if (student.getId() != null && student.getId() >= start && student.getId() <= end) {
            return true;
        }

        // 尝试用学号比较
        if (student.getStudentNo() != null) {
            try {
                long studentNo = Long.parseLong(student.getStudentNo().trim());
                if (studentNo >= start && studentNo <= end) {
                    return true;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return false;
    }

    /**
     * 检查学生是否在任意范围内
     */
    private boolean isStudentInAnyRange(Student student, List<TeacherClass> rangedClasses) {
        for (TeacherClass tc : rangedClasses) {
            if (tc.getStudentIdStart() != null && tc.getStudentIdEnd() != null) {
                if (isInRange(student, tc.getStudentIdStart(), tc.getStudentIdEnd())) {
                    return true;
                }
            }
        }
        return false;
    }
}

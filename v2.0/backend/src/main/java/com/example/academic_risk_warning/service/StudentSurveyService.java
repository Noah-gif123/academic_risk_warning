package com.example.academic_risk_warning.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.academic_risk_warning.common.BizException;
import com.example.academic_risk_warning.dto.QuestionnaireAnswerRequest;
import com.example.academic_risk_warning.entity.Enrollment;
import com.example.academic_risk_warning.entity.Questionnaire;
import com.example.academic_risk_warning.entity.QuestionnaireAnswer;
import com.example.academic_risk_warning.entity.Student;
import com.example.academic_risk_warning.entity.Teacher;
import com.example.academic_risk_warning.entity.TeachingClass;
import com.example.academic_risk_warning.mapper.EnrollmentMapper;
import com.example.academic_risk_warning.mapper.QuestionnaireAnswerMapper;
import com.example.academic_risk_warning.mapper.QuestionnaireMapper;
import com.example.academic_risk_warning.mapper.StudentMapper;
import com.example.academic_risk_warning.mapper.TeacherMapper;
import com.example.academic_risk_warning.mapper.TeachingClassMapper;
import com.example.academic_risk_warning.util.JsonObjects;
import com.example.academic_risk_warning.vo.QuestionnaireVO;
import com.example.academic_risk_warning.vo.StudentSurveyVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 学生端问卷：只收到「本人行政班」发布的、正在收集中的问卷，**匿名作答**。
 * 匿名做法：作答只写 respondent_token（问卷ID + 学号 + 固定盐的哈希，只用于防重复提交），
 * 库里不写 student_id，教务与教师看不到谁答的。
 */
@Service
@RequiredArgsConstructor
public class StudentSurveyService {

    private static final String SALT = "sws-v2-survey";

    private final QuestionnaireMapper questionnaireMapper;
    private final QuestionnaireAnswerMapper answerMapper;
    private final StudentMapper studentMapper;
    private final EnrollmentMapper enrollmentMapper;
    private final TeachingClassMapper classMapper;
    private final TeacherMapper teacherMapper;

    // ============================================================ 列表
    public List<QuestionnaireVO> myQuestionnaires(Long studentId) {
        Student st = studentMapper.selectById(studentId);
        if (st == null) {
            throw new BizException("学生档案不存在");
        }
        List<Questionnaire> all = questionnaireMapper.selectList(Wrappers.<Questionnaire>lambdaQuery()
                .eq(Questionnaire::getStatus, "COLLECTING")
                .orderByDesc(Questionnaire::getId));
        List<QuestionnaireVO> out = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (Questionnaire q : all) {
            if (!forMyClass(q, st.getAdminClassId())) {
                continue;
            }
            if (q.getCloseTime() != null && now.isAfter(q.getCloseTime())) {
                continue;
            }
            QuestionnaireVO v = new QuestionnaireVO();
            v.setId(q.getId());
            v.setTitle(q.getTitle());
            v.setQuestionnaireType(q.getQuestionnaireType());
            v.setQuestionnaireTypeText("TEACHER_STYLE".equals(q.getQuestionnaireType())
                    ? "教师风格判定" : q.getQuestionnaireType());
            v.setAnonymous(q.getIsAnonymous() != null && q.getIsAnonymous() == 1);
            v.setStatus(q.getStatus());
            v.setStatusText("收集中");
            v.setOpenTime(q.getOpenTime());
            v.setCloseTime(q.getCloseTime());
            v.setAnswerCount(q.getAnswerCount());
            v.setAdminClassId(st.getAdminClassId());
            TeachingClass tc = classMapper.selectOne(Wrappers.<TeachingClass>lambdaQuery()
                    .eq(TeachingClass::getOfferingId, q.getOfferingId()).last("limit 1"));
            if (tc != null) {
                Teacher t = teacherMapper.selectById(tc.getTeacherId());
                v.setCourseName(t == null ? null : ("被评价教师：" + t.getTeacherName()));
            }
            // 我是否已答（用同一个匿名令牌反查，不暴露身份）
            long answered = answerMapper.selectCount(Wrappers.<QuestionnaireAnswer>lambdaQuery()
                    .eq(QuestionnaireAnswer::getQuestionnaireId, q.getId())
                    .eq(QuestionnaireAnswer::getRespondentToken, tokenOf(q.getId(), studentId)));
            v.setStatusText(answered > 0 ? "已完成" : "待作答");
            out.add(v);
        }
        return out;
    }

    // ============================================================ 详情
    public StudentSurveyVO detail(Long questionnaireId, Long studentId) {
        Student st = requireStudent(studentId);
        Questionnaire q = questionnaireMapper.selectById(questionnaireId);
        if (q == null) {
            throw new BizException("问卷不存在");
        }
        if (!forMyClass(q, st.getAdminClassId())) {
            throw new BizException("这份问卷不是发给你的行政班");
        }
        StudentSurveyVO v = new StudentSurveyVO();
        v.setId(q.getId());
        v.setTitle(q.getTitle());
        v.setAnonymous(q.getIsAnonymous() != null && q.getIsAnonymous() == 1);
        v.setCloseTime(q.getCloseTime());
        v.setQuestions(questionsOf(q));
        String token = tokenOf(q.getId(), studentId);
        List<QuestionnaireAnswer> mine = answerMapper.selectList(Wrappers.<QuestionnaireAnswer>lambdaQuery()
                .eq(QuestionnaireAnswer::getQuestionnaireId, q.getId())
                .eq(QuestionnaireAnswer::getRespondentToken, token));
        v.setAnswered(!mine.isEmpty());
        if (!mine.isEmpty()) {
            Map<String, BigDecimal> scores = new LinkedHashMap<>();
            for (QuestionnaireAnswer a : mine) {
                scores.put(a.getItemCode(), a.getScoreValue());
            }
            v.setMyScores(scores);
        }
        return v;
    }

    // ============================================================ 提交
    @Transactional
    public void submit(Long questionnaireId, QuestionnaireAnswerRequest req, Long studentId) {
        Student st = requireStudent(studentId);
        Questionnaire q = questionnaireMapper.selectById(questionnaireId);
        if (q == null) {
            throw new BizException("问卷不存在");
        }
        if (!forMyClass(q, st.getAdminClassId())) {
            throw new BizException("这份问卷不是发给你的行政班");
        }
        if (!"COLLECTING".equals(q.getStatus())) {
            throw new BizException("问卷已结束，不能作答");
        }
        if (q.getCloseTime() != null && LocalDateTime.now().isAfter(q.getCloseTime())) {
            throw new BizException("问卷已截止");
        }
        String token = tokenOf(q.getId(), studentId);
        long answered = answerMapper.selectCount(Wrappers.<QuestionnaireAnswer>lambdaQuery()
                .eq(QuestionnaireAnswer::getQuestionnaireId, q.getId())
                .eq(QuestionnaireAnswer::getRespondentToken, token));
        if (answered > 0) {
            throw new BizException("你已经提交过这份问卷");
        }
        List<StudentSurveyVO.Question> questions = questionsOf(q);
        Map<String, BigDecimal> given = new LinkedHashMap<>();
        if (req.getItems() != null) {
            for (QuestionnaireAnswerRequest.Item it : req.getItems()) {
                if (it.getItemCode() == null) {
                    continue;
                }
                BigDecimal score = it.getScore() == null ? null : it.getScore();
                if (score == null || score.signum() < 0 || score.compareTo(new BigDecimal("5")) > 0) {
                    throw new BizException("评分需在 0~5 之间");
                }
                given.put(it.getItemCode(), score);
            }
        }
        for (StudentSurveyVO.Question question : questions) {
            if (!given.containsKey(question.getItemCode())) {
                throw new BizException("还有题目没有作答：" + question.getText());
            }
        }
        Long targetTeacher = null;
        TeachingClass tc = classMapper.selectOne(Wrappers.<TeachingClass>lambdaQuery()
                .eq(TeachingClass::getOfferingId, q.getOfferingId()).last("limit 1"));
        if (tc != null) {
            targetTeacher = tc.getTeacherId();
        }
        LocalDateTime now = LocalDateTime.now();
        for (Map.Entry<String, BigDecimal> e : given.entrySet()) {
            QuestionnaireAnswer a = new QuestionnaireAnswer();
            a.setQuestionnaireId(q.getId());
            a.setRespondentToken(token);
            // 匿名：不写 student_id / enrollment_id
            a.setTargetTeacherId(targetTeacher);
            a.setItemCode(e.getKey());
            a.setAnswerType("SCORE");
            a.setScoreValue(e.getValue());
            a.setSubmitTime(now);
            answerMapper.insert(a);
        }
        q.setAnswerCount((q.getAnswerCount() == null ? 0 : q.getAnswerCount()) + 1);
        questionnaireMapper.updateById(q);
    }

    // ============================================================ 内部
    private Student requireStudent(Long studentId) {
        Student st = studentMapper.selectById(studentId);
        if (st == null) {
            throw new BizException("学生档案不存在");
        }
        return st;
    }

    /** 问卷是否发给这个行政班 */
    private boolean forMyClass(Questionnaire q, Long adminClassId) {
        if (adminClassId == null) {
            return false;
        }
        String json = q.getCollectorClassIdsJson();
        if (json == null || json.isBlank()) {
            return false;
        }
        for (String part : json.replace("[", "").replace("]", "").split(",")) {
            String t = part.trim().replace("\"", "");
            if (t.isEmpty()) {
                continue;
            }
            try {
                if (Long.parseLong(t) == adminClassId) {
                    return true;
                }
            } catch (NumberFormatException ignored) {
                // 忽略脏数据
            }
        }
        return false;
    }

    /** 题目：优先按发布时写入的 itemCode，老数据回退成 Q1..Qn */
    private List<StudentSurveyVO.Question> questionsOf(Questionnaire q) {
        List<StudentSurveyVO.Question> out = new ArrayList<>();
        List<Map<String, String>> items = JsonObjects.arrayObjects(q.getQuestionJson());
        int i = 0;
        for (Map<String, String> m : items) {
            i++;
            StudentSurveyVO.Question question = new StudentSurveyVO.Question();
            String code = m.get("itemCode");
            question.setItemCode(code == null || code.isBlank() ? ("Q" + i) : code);
            String text = m.get("q");
            question.setText(text == null ? ("第 " + i + " 题") : text);
            String type = m.get("type");
            question.setType(type == null ? "SCALE" : type);
            out.add(question);
        }
        return out;
    }

    /** 匿名去重令牌：问卷 + 学号 + 固定盐的 SHA-256（库中不落学生ID） */
    private String tokenOf(Long questionnaireId, Long studentId) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest((questionnaireId + ":" + studentId + ":" + SALT).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 16; i++) {
                sb.append(String.format("%02x", bytes[i]));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new BizException("问卷令牌生成失败");
        }
    }
}

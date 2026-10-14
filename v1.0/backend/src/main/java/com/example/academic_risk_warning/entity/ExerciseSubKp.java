package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("exercise_sub_kp")
public class ExerciseSubKp {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long subQuestionId;
    private Long kpId;
    private BigDecimal weight;   // 0.0000 ~ 1.0000，综合题各知识点合计=1.0
}

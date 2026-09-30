# -*- coding: utf-8 -*-
"""导出 v2 各表字段，用于核对汇报稿的字段级描述。"""
import io
import re

FILES = ["01_base_layer", "02_org_and_offering", "03_enrollment_and_session",
         "04_attendance", "05_score", "06_engine_outputs",
         "07_alert_and_intervention", "08_workflow_and_audit"]

# 汇报稿中对字段有具体描述的表 -> 关注的关键字段
WATCH = {
    "student": ["account_id", "admin_class_id", "enroll_year", "status", "password"],
    "teacher": ["account_id", "title", "password"],
    "admin": ["account_id", "admin_type", "dept_name", "password"],
    "course": ["course_type", "prerequisite_course_id", "usual_ratio", "total_class_times"],
    "course_knowledge_point": ["chapter", "parent_id"],
    "course_offering": ["weeks_per_term", "times_per_week", "total_sessions",
                        "actual_weeks", "actual_sessions", "has_placement_test", "pass_score"],
    "course_group": ["level", "locked"],
    "score_scheme": ["level", "locked", "confirm_status", "version"],
    "score_scheme_item": ["parent_id", "weight", "data_source", "score_type"],
    "enrollment": ["status"],
    "course_session": ["week_no", "session_no", "status"],
    "attendance_record": ["status", "phase", "source"],
    "student_admission_score": ["rank_percentile", "subject_group"],
    "risk_engine_config": ["channel_a_enabled", "channel_b_enabled", "channel_c_enabled",
                           "signal_weights_json"],
    "teacher_style": ["raw_delta", "delta", "shrink_lambda", "sample_size", "fused"],
    "alert_record": ["engine_version", "channel", "remaining_absences",
                     "roll_call_coverage", "confidence", "is_void"],
    "notification": ["recipient_type", "aggregate_key"],
    "intervention_record": ["stage", "operator_id", "action_type", "effective"],
}

found = {}
for fn in FILES:
    s = io.open(".tmp-resume/sql-v2/%s.sql" % fn, encoding="utf-8").read()
    for m in re.finditer(r"CREATE TABLE `(\w+)` \((.*?)\n\) ENGINE", s, re.S):
        found[m.group(1)] = m.group(2)

print("=" * 76)
print("字段核对：汇报稿描述 vs 建表脚本")
print("=" * 76)
bad = []
for t, cols in WATCH.items():
    body = found.get(t)
    if body is None:
        print("!!! 表不存在: %s" % t)
        bad.append(t)
        continue
    present = set(re.findall(r"^\s*`(\w+)`", body, re.M))
    missing_desc = [c for c in cols if c not in present]
    absent_desc = [c for c in cols if c.startswith("!") ]  # 期望"不存在"
    print("%-24s 字段数%3d  预期字段缺失: %s" % (t, len(present),
                                              ", ".join(missing_desc) if missing_desc else "无"))
    if missing_desc:
        bad.append("%s:%s" % (t, ",".join(missing_desc)))

print("-" * 76)
print("异常项: %d" % len(bad))

# 关键约束核查
print("\nCHECK 约束（禁止考勤类评分项）:")
for fn in FILES:
    s = io.open(".tmp-resume/sql-v2/%s.sql" % fn, encoding="utf-8").read()
    for m in re.finditer(r"CHECK\s*\(([^)]*)\)", s, re.I):
        line = m.group(0).replace("\n", " ")
        if "考勤" in line or "category" in line or "item" in line:
            print("   %s" % line[:110])

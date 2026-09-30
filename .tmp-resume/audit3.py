# -*- coding: utf-8 -*-
"""统计 v2 建表脚本的实际表数，核对汇报稿的分组计数。"""
import io
import re

FILES = [
    ("01", "基础层", "01_base_layer.sql"),
    ("02", "组织与开课", "02_org_and_offering.sql"),
    ("03", "选课与课次", "03_enrollment_and_session.sql"),
    ("04", "考勤", "04_attendance.sql"),
    ("05", "成绩", "05_score.sql"),
    ("06", "引擎产物", "06_engine_outputs.sql"),
    ("07", "预警与干预", "07_alert_and_intervention.sql"),
    ("08", "教务流程与审计", "08_workflow_and_audit.sql"),
]

total = 0
tables = {}
for no, name, fn in FILES:
    s = io.open(".tmp-resume/sql-v2/" + fn, encoding="utf-8").read()
    ts = re.findall(r"CREATE TABLE `(\w+)`", s)
    tables[name] = ts
    total += len(ts)
    print("%s %-14s %2d 张 : %s" % (no, name, len(ts), ", ".join(ts)))

print("-" * 70)
print("合计 %d 张" % total)

# 汇报稿中的分组声明
DOC_CLAIMS = {
    "基础层": 12, "组织与开课": 7, "选课与课次": 2, "考勤": 3,
    "成绩": 4, "引擎产物": 7, "预警与干预": 6, "教务流程与审计": 8,
}
print("\n汇报稿声明 vs 实际：")
bad = 0
for k, v in DOC_CLAIMS.items():
    real = len(tables.get(k, []))
    ok = (real == v)
    if not ok:
        bad += 1
    print("  %s %-14s 声明 %2d  实际 %2d" % ("OK " if ok else "!!!", k, v, real))
print("  声明合计 %d  实际合计 %d" % (sum(DOC_CLAIMS.values()), total))
print("  不一致项: %d" % bad)

# 旧库 38 张核对（根目录脚本）
old = io.open("study_warning_system.sql", encoding="utf-8").read()
old_ts = re.findall(r"CREATE TABLE `(\w+)`", old)
print("\n旧库表数: %d" % len(old_ts))

# 迁移脚本声明的迁移对象
mig = io.open(".tmp-resume/sql-v2/10_migrate_from_v1.sql", encoding="utf-8").read()
print("\n迁移脚本涉及的表:")
for t in old_ts:
    if re.search(r"`?" + t + r"`?", mig) and t in ("student", "teacher", "admin", "course", "course_knowledge_point", "intervention_record"):
        print("   - %s" % t)

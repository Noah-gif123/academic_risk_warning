# -*- coding: utf-8 -*-
import io
import re

s = io.open(".tmp-resume/sql-v2/04_attendance.sql", encoding="utf-8").read()
for t in ["attendance_archive", "attendance_record"]:
    m = re.search(r"CREATE TABLE `" + t + r"` \((.*?)\n\) ENGINE", s, re.S)
    if not m:
        print("!! not found", t)
        continue
    print("=== " + t + " ===")
    for line in m.group(1).split("\n"):
        c = re.match(r"\s*`(\w+)`\s+([\w()\,]+)", line)
        if c:
            cm = re.search(r"COMMENT '(.*?)'", line)
            print("   %-22s %-16s %s" % (c.group(1), c.group(2), cm.group(1) if cm else ""))
    print()

# 归档相关的设计注释
print("=== 04 文件里关于归档/双轨的设计注释 ===")
for line in s.split("\n"):
    if line.strip().startswith("--") and ("归档" in line or "双轨" in line or "平时" in line):
        print("  " + line.strip()[:120])

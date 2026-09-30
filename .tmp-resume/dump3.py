# -*- coding: utf-8 -*-
import io
import re

s = io.open(".tmp-resume/sql-v2/02_org_and_offering.sql", encoding="utf-8").read()
for t in ["score_scheme", "score_scheme_item"]:
    m = re.search(r"CREATE TABLE `" + t + r"` \((.*?)\n\) ENGINE", s, re.S)
    if not m:
        print("!! not found", t)
        continue
    print("=== " + t + " ===")
    for line in m.group(1).split("\n"):
        c = re.match(r"\s*`(\w+)`\s+([\w()\,]+)", line)
        if c:
            cm = re.search(r"COMMENT '(.*?)'", line)
            print("   %-24s %-16s %s" % (c.group(1), c.group(2), cm.group(1) if cm else ""))
    print()

# 汇报稿中关于这两个表的原句
doc = io.open("汇报稿源文.md", encoding="utf-8").read()
print("=== 汇报稿相关原句 ===")
for line in doc.split("\n"):
    if "开课表——" in line or "课程组表——" in line or "成绩构成表——" in line:
        print("  " + line.strip()[:200])

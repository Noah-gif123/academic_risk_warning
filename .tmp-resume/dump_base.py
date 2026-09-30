# -*- coding: utf-8 -*-
"""抽取 v2 基础层中被沿用表的关键字段变化。"""
import io
import re

s = io.open(r".tmp-resume/sql-v2/01_base_layer.sql", encoding="utf-8").read()

for t in ["student", "teacher", "admin", "course", "course_knowledge_point", "term"]:
    m = re.search(r"CREATE TABLE `" + t + r"` \((.*?)\n\) ENGINE", s, re.S)
    if not m:
        print("!! not found:", t)
        continue
    print("=== " + t + " ===")
    for line in m.group(1).split("\n"):
        line = line.strip()
        c = re.match(r"`(\w+)`\s+([\w()\,]+)", line)
        if not c:
            continue
        name = c.group(1)
        if name.upper() in ("PRIMARY", "KEY", "UNIQUE", "INDEX", "CONSTRAINT"):
            continue
        cm = re.search(r"COMMENT\s+'(.*?)'", line)
        print("   %-26s %-18s %s" % (name, c.group(2), cm.group(1) if cm else ""))
    print()

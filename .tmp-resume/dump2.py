# -*- coding: utf-8 -*-
import io
import re

s = io.open(".tmp-resume/sql-v2/02_org_and_offering.sql", encoding="utf-8").read()
for t in ["course_offering", "course_group"]:
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

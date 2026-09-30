# -*- coding: utf-8 -*-
"""给总结稿的章标题加显式标记 @N、，消除“一、”既当章标题又当列表项的歧义。

判据：正文中分隔线只出现在章标题之前，因此“分隔线之后紧随的非空行”即章标题。
脚本可重复执行：会先去掉已有的 @ 标记再重新标记。
"""
import io
import re

SRC = "总结_修订版.md"

text = io.open(SRC, encoding="utf-8").read()
text = re.sub(r"^@(\d+)、", "", text, flags=re.M)      # 幂等：先清除旧标记
lines = text.split("\n")
RULE = re.compile(r"^[─\-=]{4,}$")
NUM = "一二三四五六七八九十"
HEAD = re.compile(r"^([" + NUM + r"]+)、\s*(\S.*?)\s*$")

out = []
idx = 0
count = 0
while idx < len(lines):
    line = lines[idx]
    if RULE.match(line.strip()):
        j = idx + 1
        while j < len(lines) and not lines[j].strip():
            j += 1
        if j < len(lines):
            m = HEAD.match(lines[j].strip())
            if m and len(m.group(1)) == 1:
                num = NUM.index(m.group(1)) + 1
                out.append(line)
                out.extend(lines[idx + 1:j])
                out.append("@%d、%s" % (num, m.group(2)))
                count += 1
                idx = j + 1
                continue
    out.append(line)
    idx += 1

io.open(SRC, "w", encoding="utf-8", newline="\n").write("\n".join(out))
print("标记章标题 %d 条" % count)
for l in out:
    if l.startswith("@"):
        print("   " + l)

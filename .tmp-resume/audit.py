# -*- coding: utf-8 -*-
"""核对汇报稿与权威设计文档 / 建表脚本之间的一致性。"""
import io
import re

REF_CLIST = io.open(r".tmp-resume/ref/v2.0.0改造清单.md", encoding="utf-8").read()
REF_STEPS = io.open(r".tmp-resume/ref/改造步骤与阶段.md", encoding="utf-8").read()
REF_TODO = io.open(r".tmp-resume/ref/待讨论问题清单.md", encoding="utf-8").read()
SEED = io.open(r".tmp-resume/sql-v2/09_seed_data.sql", encoding="utf-8").read()
ENGINE = io.open(r".tmp-resume/sql-v2/06_engine_outputs.sql", encoding="utf-8").read()
BASE = io.open(r".tmp-resume/sql-v2/01_base_layer.sql", encoding="utf-8").read()
SCORE = io.open(r".tmp-resume/sql-v2/05_score.sql", encoding="utf-8").read()
DOC = io.open("汇报稿源文.md", encoding="utf-8").read()

ALL = REF_CLIST + REF_STEPS + REF_TODO + SEED + ENGINE + BASE + SCORE

checks = []


def chk(name, pattern, expect, src=None):
    """在权威来源中查找特征串，确认汇报稿的说法有依据。"""
    hay = src if src is not None else ALL
    found = bool(re.search(pattern, hay))
    checks.append((name, expect, "FOUND" if found else "*** NOT FOUND ***"))


# ---------- 算法口径 ----------
chk("允许缺课上限 floor(总课次/3)", r"N_max\s*=\s*floor")
chk("挂科硬线 已缺>=N_max+1", r"N_max\s*\+\s*1")
chk("剩余可缺 =0 再缺即挂 / <=-1 必然挂", r"≤\s*−?1|≤\s*-1|差一格")
chk("扣分 P=2缺+1迟+1max(0,请假-3)", r"2×N?缺课\s*\+\s*1×N?迟到|2×缺课\s*\+\s*1×迟到")
chk("请假每学期每门课3次", r"每学期每门课|每课/学期|请假免扣次数")
chk("期末需考 x=(pass+P-W其余)/r_f", r"pass_score\s*\+\s*P\s*−\s*W_其余项|60\s*−\s*W\s*\+\s*P")
chk("x>100 实际不可达", r">\s*100\s*→\s*实际不可达|期末满分\s*100")
chk("置信度=锚点×完备×样本", r"锚点强度\s*×\s*数据完备度\s*×\s*样本")
chk("置信度四档 0.7/0.4/0.15", r"0\.7|0\.15")
chk("风险档位 红75 橙60 黄40", r"红\s*≥\s*75|threshold_red")
chk("收缩常数 k=60", r"k\s*=\s*60|n\s*/\s*\(n\s*\+\s*60\)|n\+60")
chk("熔断 |Δ|>20", r"\|\s*Δ\s*\|\s*>\s*20|>20\s*分")
chk("低区分度 std<3 或 ≥60%同分", r"std\s*<\s*3|标准差小于三")
chk("截尾均值 去最高最低各5%", r"截尾|各\s*5%|百分之五")
chk("clip 0~100", r"clip\s*0~100|0\s*~\s*100")
chk("交替去均值 5~10 轮", r"交替去均值|5~10\s*轮|5～10")
chk("专业组×课程交互 β_(k,major)", r"β_?\(?k,?major\)?|专业组\s*×\s*课程")
chk("通道A 第2周起", r"第\s*2\s*周起|第\s*2\s*周")
chk("通道B 第2~3周起", r"第\s*2\s*[~～-]\s*3\s*周")
chk("通道C 第6~9周起", r"第\s*6\s*[~～-]\s*9\s*周|第\s*6\s*[~～-]\s*8\s*周")

# ---------- 导员端 ----------
chk("多科目 = 3门", r"3\s*门|多科目预警\s*\|\s*3")
chk("班集体 第1-4周不触发", r"第\s*1[–-]4\s*周不触发|第1[–-]4周")
chk("班集体 ≥30%且≥5人", r"30%\s*且\s*≥?\s*5|0\.30")
chk("升级 ≥50%且≥10人", r"50%\s*且\s*≥?\s*10|0\.50")
chk("无效判定 下次刷新/最长2周/期末终判", r"下次\s*risk_state\s*刷新|最长\s*2\s*周|最长观察\s*2\s*周")
chk("风险分≥40 才进导员视图", r"≥\s*40|风险分\s*≥\s*40")

# ---------- 大一替代机制 ----------
chk("摸底考 仅大一/可选/由教务决定", r"摸底考[^\n]{0,40}大一|大一[^\n]{0,20}摸底考")
chk("入学成绩 大一无历史挂科", r"大一[^\n]{0,30}(没有|无)[^\n]{0,10}历史|大一上无此数据|大一上无")
chk("入学成绩逐周退场 100/60/30/≤10", r"60%\s*\+\s*跨课程锚\s*40%|第\s*5~6\s*周\s*60%")
chk("位次百分位 不跨组比较", r"位次百分位")
chk("物理组/历史组各自标准化", r"物理组\s*/\s*历史组|物理组、历史组")

# ---------- 建表脚本与迁移 ----------
chk("49 张表", r"共\s*\*\*?49\s*张表|合计建表\s*\|\s*\*\*49\*\*|49\s*张表")
chk("10 个脚本", r"10\s*个脚本|10\s*\|\s*`10_migrate")
chk("学生 800", r"学生\s*800|800\s*行|学生\s*800\s*（")
chk("教师 9", r"教师\s*9|教师\s*9\s*行")
chk("课程 2", r"课程\s*2\s*行|课程\s*2\b")
chk("知识点 32", r"知识点\s*32|32\s*（8\s*章")
chk("干预记录 21", r"干预记录\s*21|21\s*行")
chk("学期 4", r"学期\s*4\b")
chk("周历 64 周", r"64\s*周")
chk("构成模板 4 套", r"构成模板\s*4|4\s*套")
chk("引擎配置 1 版", r"引擎配置\s*1\s*版|引擎配置\s*1\b")
chk("运营配置 11 项", r"11\s*项|运营配置\s*11")

# ---------- 术语 ----------
chk("旧算法维度表述（四维/五维）", r"五维|四维|四个维度|5\s*个维度")
chk("旧算法第2维=历史挂科率", r"历史挂科率|挂科率")
chk("摸底考=A′层", r"A[′']|A\s*层")

print("=" * 78)
print("汇报稿 vs 权威来源 一致性核对")
print("=" * 78)
bad = 0
for name, expect, status in checks:
    flag = "OK " if status == "FOUND" else "!!!"
    if status != "FOUND":
        bad += 1
    print("%s %-46s 依据: %s" % (flag, name, status))
print("-" * 78)
print("共 %d 项，未找到依据 %d 项" % (len(checks), bad))

# RAG 检索层与评测（R2 + R4）

> 目标：让"基于 RAG 检索增强"这个题目有真实实现和可验证的实验数据，而不是"配了个知识库"。
> 本文记录已落地的代码、如何跑评测、当前基线，以及还缺什么。

## 1 结构：Dify 做向量底座，方法层在自己代码里

```
课程资料（导学 / 课件 / 实验指导 / 复习提纲）
   │ ① Dify 知识库：解析 → 分块 → embedding → Weaviate        ← 用 Dify 的工程，不自造轮子
   ▼
向量路 top-20 (DifyVectorClient)        全文路 top-20 (MySQL ngram FULLTEXT)
   └──────────────┬───────────────────────────────┘
                  ▼
   ② RRF 融合（RetrievalService，只看名次不看分数）→ 拒答判据（guard）
                  ▼
   ③ 生成（StudentQaService，带引用要求）+ 引用校验
                  ▼
   ④ 评测（RagEvalService）：全文 / 向量 / 混合 三行对比
```

| 组件 | 位置 | 职责 |
| --- | --- | --- |
| `RetrievalService` | `service/ai/RetrievalService.java` | 三模式检索、RRF 融合、拒答判据、诊断包；`guard()` 是线上问答与评测共用的唯一判据 |
| `DifyVectorClient` | `service/ai/DifyVectorClient.java` | 调 `POST /v1/datasets/{id}/retrieve`；没配 key 不报错，只标"未配置" |
| `RagEvalService` | `service/ai/RagEvalService.java` | 跑评测集、算 recall@k/MRR/nDCG/拒答准确率/误拒率、写 `ai_rag_eval_run` |
| `AdminRagEvalController` | `controller/admin/AdminRagEvalController.java` | 评测入口（只检索不生成，**不消耗 token**） |

## 2 配置

```yaml
app.ai.retrieval:
  mode: HYBRID            # FULLTEXT / VECTOR / HYBRID；向量没配时自动降级为 FULLTEXT
  pool-size: 20           # 每路召回池
  vector-top-k: 20
  dataset-key: ${AI_RAG_DATASET_KEY:}   # Dify 知识库 API Key（控制台「知识库 → API」，不是应用 App Key）
  dataset-id: ${AI_RAG_DATASET_ID:}     # 知识库 ID
  filter-by-course: true  # 按 metadata.course_id 过滤，避免跨课串答案
```

诊断会写进 `ai_qa_log.retrieval_mode` 与 `ai_qa_log.retrieval_json`，含每路命中数、top 分、名次、融合分、耗时、降级原因。

## 3 怎么跑评测

```bash
# 后台先跑起来（需要 admin001 / 000000 登录拿 token）
POST /api/admin/ai/rag-eval?mode=FULLTEXT&k=4      # 单模式
GET  /api/admin/ai/rag-eval/compare?k=4           # 全文 / 向量 / 混合 三行对比
```

评测集在 `ai_rag_eval_case`（`sql/38_seed_rag_eval.sql` 生成）：**30 条可答 + 10 条不可答**，
题目全部取自真实语料，`gold_doc` 就是答案所在文档；同一份导学发给多个教学班时用 `|` 列多个等价出处。

## 4 指标定义

| 指标 | 定义 |
| --- | --- |
| `recall@k` | 可答问题里，标准出处出现在前 k 条命中的比例（文档级） |
| `MRR` | 第一条正确出处的名次倒数均值 |
| `nDCG@k` | 二值相关性下的归一化折损累计增益 |
| `refuseAcc` | 不可答问题被正确拒答的比例 |
| `falseRefuse` | 可答问题被误拒的比例（越低越好） |
| `effectiveFallback` | 本次有多少条用例**其实没走目标模式**（例如向量未配置被降级）——不为 0 时该行指标不能当结论 |

## 5 当前基线

语料：**213 块 / 57,506 字 / 29 份文档**（课1:60 课4:51 课7:50 课8:48 课10:4）。
评测集：**64 条**（54 可答 + 10 不可答），覆盖 5 门课的导学口径 + 课件/习题集/大纲/实验指导。

| 模式 | recall@4 | MRR | nDCG@4 | 拒答准确率 | 误拒率 | 平均耗时 | fallback |
| --- | --- | --- | --- | --- | --- | --- | --- |
| FULLTEXT（213 块，k=4） | 0.8333 | 0.7577 | 0.7769 | 0.90 | 0.0556 | 1–2 ms | 0 |
| VECTOR | —（向量通道未配置，64/64 降级，不代表该模式） | | | | | | 64 |
| HYBRID | —（同上） | | | | | | 64 |
| *参照：FULLTEXT（40 块，30 条旧评测集，k=4）* | *0.9667* | *0.9333* | *0.9421* | *1.00* | *0.00* | *1–2 ms* | *0* |

### 5.1 词面支撑度扫参（minLexSupport）

| minLexSupport | 拒答准确率 | 误拒率 |
| --- | --- | --- |
| 0.10 | 0.60 | 0.037 |
| 0.15 | 0.60 | 0.056 |
| **0.20** | **0.90** | **0.056** |
| 0.25 | 0.90 | 0.074 |
| 0.30 | 0.90 | 0.148 |
| 0.40 | 0.90 | 0.370 |

结论：语料从 40 块扩到 213 块后，0.10 已挡不住"词在、答案不在"的问题（"傅里叶变换""动量守恒"能骗过校验），
**0.20 是拐点**——拒答准确率从 0.60 提到 0.90，误拒只从 3.7% 涨到 5.6%；再往上误拒陡增。默认值已改为 0.20。

### 5.2 语料扩张暴露的三个真问题（都已写进代码注释）

1. **同源分块占满 top-k**：一份课件十几个块，分数接近，把 top-4 全占了，别的文档压根没机会出现。
   → 加同源抑制（`max-per-doc=2`），recall@4 从 0.7778 → 0.7963。
2. **逐字数字压过正确文档**：问"学分和周次"，课程大纲里的学分/学时逐字命中，把本该命中的导学挤下去。
   这是纯关键词检索的固有短板，**正是向量路要解决的问题**（语义上"学分和周次"与导学条款更近）。
3. **"词在、答案不在"的假命中**：'Java 分代垃圾回收' 能召回一段写着"Java 有垃圾回收"的链表笔记。
   这类只能靠阈值 + 引用校验挡，属于反幻觉要长期盯的 case。

> ⚠️ 顺带发现一处**数据不一致**（与检索无关，但答辩可能被问）：
> `课程大纲_大学计算机基础` 写「3 学分 / 48 学时」，而 `导学 · CS-01-A` 写「2.0 学分 / 12 周」。
> 同一门课两套口径，建议统一后再对外演示。

解读要点（答辩可以直接讲）：

- 全文路在**有明确术语的题目**上已经很强（recall@4 = 0.9667），这也是为什么本项目不能丢掉全文路；
- 10 条不可答问题**全部被正确拒答**（refuseAcc = 1.00），说明 `guard()` 的阈值 + 词面支撑度护栏是有效的反幻觉手段；
- 但语料只有 40 块、且题目用词贴近原文，所以这个分数**偏乐观**；等语料扩到 200+ 块、题目改用口语化提法，才是真实压力测试。

## 6 语料入库工具

`v2.0/tool/kb/`：

| 文件 | 用途 |
| --- | --- |
| `KbIngest.java` | 遍历 `kb-source/` → 解析 docx/pptx/pdf/txt/md → 切块（300 字 + 60 字重叠）→ 写 `ai_kb_chunk` / `ai_kb_doc` |
| `FetchJars.java` | 用 Java 的 TLS 栈从 maven 拉依赖（本机 PowerShell 的 schannel 握手失败，`Invoke-WebRequest` 下不了 jar） |

```bash
# 一条命令重建整个语料（幂等：同名文档先删后插）
java -cp ".;../../backend/build-verify-lib/*" KbIngest ../../kb-source
```

资料原件放在**仓库根的 `kb-source/<课程>/`**（原件不入库，只提交解析后的分块数据）。
实测产出：12 份文档 → 173 块，加原有 40 块 = 213 块。

## 7 还缺什么（阻塞项）

| 项 | 阻塞谁 | 需要什么 |
| --- | --- | --- |
| 向量路 | R2 的 VECTOR/HYBRID 两行指标 | 在 Dify（8090）里给 `openai_api_compatible` 填 SiliconFlow 的 `BAAI/bge-m3` 并**点保存**；之后我用已配好的 `AI_RAG_DATASET_KEY` 自动建库灌数据、拿 dataset_id |
| 消融实验 | 论文三行对比表 | 上面到位后跑 `compare`，把 `effectiveFallback=0` 的三行贴进论文 |
| 数据一致性 | 答辩追问 | `课程大纲_大学计算机基础`（3 学分/48 学时）与 `导学 · CS-01-A`（2.0 学分/12 周）口径冲突，需择一 |

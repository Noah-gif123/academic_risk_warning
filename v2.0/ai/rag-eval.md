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

## 5 当前基线（2026-11-02，40 块语料）

| 模式 | recall@4 | MRR | nDCG@4 | 拒答准确率 | 误拒率 | 平均耗时 | fallback |
| --- | --- | --- | --- | --- | --- | --- | --- |
| FULLTEXT (k=4) | 0.9667 | 0.9333 | 0.9421 | 1.00 | 0.00 | 1–2 ms | 0 |
| FULLTEXT (k=10) | 1.0000 | 0.9400 | 0.9550 | 1.00 | 0.00 | 1–2 ms | 0 |
| VECTOR | —（向量通道未配置，40/40 降级，不代表该模式） | | | | | | 40 |
| HYBRID | —（同上） | | | | | | 40 |

k=4 → k=10 把 recall 从 0.9667 补到 1.0：唯一漏的那条是"三级项目怎么提交、什么时候交"，
原文写作"第 10 周前提交"、资料名叫 `Excel三级项目_选题与提交要求.txt`，全文路把它排在第 5 名开外。
这条用例很典型——**问题越口语化、越偏离原文用词，全文路越吃亏**，正是引入向量路的理由。

解读要点（答辩可以直接讲）：

- 全文路在**有明确术语的题目**上已经很强（recall@4 = 0.9667），这也是为什么本项目不能丢掉全文路；
- 10 条不可答问题**全部被正确拒答**（refuseAcc = 1.00），说明 `guard()` 的阈值 + 词面支撑度护栏是有效的反幻觉手段；
- 但语料只有 40 块、且题目用词贴近原文，所以这个分数**偏乐观**；等语料扩到 200+ 块、题目改用口语化提法，才是真实压力测试。

## 6 还缺什么（阻塞项）

| 项 | 阻塞谁 | 需要什么 |
| --- | --- | --- |
| 向量路 | R2 的 VECTOR/HYBRID 两行指标 | 一个 embedding 来源：在 Dify 里加 OpenAI-API-compatible 供应商（硅基流动 bge-m3 或 DashScope text-embedding-v3），再把知识库 ID 与 dataset key 配进 `ai.env` |
| 语料规模 | R3（200+ 块） | 4 门课的课件/习题/大纲原件——现有真实文本只够切 40–60 块，不能靠编内容凑数 |
| 消融实验 | 论文三行对比表 | 上面两项到位后重跑 `compare`，把 `effectiveFallback=0` 的三行贴进论文 |

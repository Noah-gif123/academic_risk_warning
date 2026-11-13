-- 独立测试集：20 题（10 可答 + 10 不可答），不参与阈值调参
-- 可答题复用既有已核验用例的 gold_doc/gold_section，避免手写文档名
delete from ai_rag_eval_case where note='INDEPENDENT_V2';
insert into ai_rag_eval_case(course_id,question,answerable,gold_doc,gold_section,note)
select course_id,'总评成绩里的平时成绩由哪些分项组成',1,gold_doc,gold_section,'INDEPENDENT_V2' from ai_rag_eval_case where id=105;
insert into ai_rag_eval_case(course_id,question,answerable,gold_doc,gold_section,note)
select course_id,'课程总学时和学分分别是多少',1,gold_doc,gold_section,'INDEPENDENT_V2' from ai_rag_eval_case where id=106;
insert into ai_rag_eval_case(course_id,question,answerable,gold_doc,gold_section,note)
select course_id,'VLOOKUP 的第三个参数表示什么',1,gold_doc,gold_section,'INDEPENDENT_V2' from ai_rag_eval_case where id=111;
insert into ai_rag_eval_case(course_id,question,answerable,gold_doc,gold_section,note)
select course_id,'高等数学课堂测试一共几次、每次多少分',1,gold_doc,gold_section,'INDEPENDENT_V2' from ai_rag_eval_case where id=128;
insert into ai_rag_eval_case(course_id,question,answerable,gold_doc,gold_section,note)
select course_id,'函数极限 ε-δ 定义中 δ 与 ε 是什么关系',1,gold_doc,gold_section,'INDEPENDENT_V2' from ai_rag_eval_case where id=131;
insert into ai_rag_eval_case(course_id,question,answerable,gold_doc,gold_section,note)
select course_id,'顺序表按位置查找元素时要注意什么',1,gold_doc,gold_section,'INDEPENDENT_V2' from ai_rag_eval_case where id=141;
insert into ai_rag_eval_case(course_id,question,answerable,gold_doc,gold_section,note)
select course_id,'顺序表和链表各自适合什么场景',1,gold_doc,gold_section,'INDEPENDENT_V2' from ai_rag_eval_case where id=143;
insert into ai_rag_eval_case(course_id,question,answerable,gold_doc,gold_section,note)
select course_id,'数据库原理课程的成绩如何构成',1,gold_doc,gold_section,'INDEPENDENT_V2' from ai_rag_eval_case where id=145;
insert into ai_rag_eval_case(course_id,question,answerable,gold_doc,gold_section,note)
select course_id,'计算机网络课堂测试一共多少分',1,gold_doc,gold_section,'INDEPENDENT_V2' from ai_rag_eval_case where id=148;
insert into ai_rag_eval_case(course_id,question,answerable,gold_doc,gold_section,note)
select course_id,'拥塞控制包含哪些算法',1,gold_doc,gold_section,'INDEPENDENT_V2' from ai_rag_eval_case where id=152;
insert into ai_rag_eval_case(course_id,question,answerable,gold_doc,gold_section,note) values
(1,'量子纠缠的贝尔不等式怎么推导',0,null,null,'INDEPENDENT_V2'),
(1,'Transformer 自注意力公式是什么',0,null,null,'INDEPENDENT_V2'),
(4,'明朝内阁首辅制度如何演变',0,null,null,'INDEPENDENT_V2'),
(4,'Python 的 GIL 是什么',0,null,null,'INDEPENDENT_V2'),
(7,'卷积神经网络感受野怎么计算',0,null,null,'INDEPENDENT_V2'),
(7,'《红楼梦》前八十回作者是谁',0,null,null,'INDEPENDENT_V2'),
(9,'TCP 拥塞窗口和慢启动是什么关系',0,null,null,'INDEPENDENT_V2'),
(9,'操作系统页面置换 LRU 怎么实现',0,null,null,'INDEPENDENT_V2'),
(11,'矩阵特征值的几何意义是什么',0,null,null,'INDEPENDENT_V2'),
(11,'傅里叶变换的频域卷积定理怎么证明',0,null,null,'INDEPENDENT_V2');

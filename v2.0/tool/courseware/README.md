# 演示课件生成器（M11 补强 · 仅用于造数据）

这两份课件不是业务代码，而是**测试数据生成器**：用来验证
「教师上传课件 → 知识库自动入库 → 学生答疑带页码引用」这条链路，不参与后端编译。

## 生成什么

| 文件 | 内容 |
| --- | --- |
| `第2讲_Excel函数与图表.pptx` | 9 页：本讲目标 / 单元格引用 / VLOOKUP / IF 与 SUMIF / 统计函数 / 图表选择 / 实验二布置 / 报错对照表；其中 2 页带讲稿备注 |
| `实验二_成绩统计操作指导.docx` | 6 章：实验目的、数据准备、操作步骤、提交要求、评分标准（表格）、常见问题 |

内容与课程导学（《大学计算机基础（一）》：平时 40% + 期末 60%、课后作业 4×10、三级项目 20）
以及 `sql/29_seed_intervention_demo.sql` 的演示口径保持一致，答辩时可以直接当"课件"讲。

## 怎么跑

需要 POI 的 jar。最省事的取法是直接从后端 fat jar 里解出依赖（不需要联网）：

```powershell
$lib = "$env:TEMP\arw-courseware-libs"
New-Item -ItemType Directory -Force -Path $lib | Out-Null
$tmp = "$env:TEMP\arw-fatjar"; Remove-Item -Recurse -Force $tmp -ErrorAction SilentlyContinue
Add-Type -AssemblyName System.IO.Compression.FileSystem
[IO.Compression.ZipFile]::ExtractToDirectory('..\..\backend\build\libs\academic-risk-warning-2.0.0-SNAPSHOT.jar', $tmp)
Copy-Item "$tmp\BOOT-INF\lib\*.jar" $lib

# 编译 + 运行（输出目录 = v2.0/demo/courseware）
& "D:\Jdk17\Jdk17\bin\javac.exe" -encoding UTF-8 -cp "$lib\*" -d "$env:TEMP\arw-gen" .\GenCourseware.java
& "D:\Jdk17\Jdk17\bin\java.exe" "-Djava.awt.headless=true" -Dfile.encoding=UTF-8 -cp "$lib\*;$env:TEMP\arw-gen" GenCourseware ..\..\demo\courseware
```

> Windows PowerShell 5.1 里 `-D` 开头的 JVM 参数必须单独加引号，否则会被当成类名。

## 放进系统里

1. 把两份文件复制到后端上传目录，文件名按 `sql/32_seed_courseware.sql` 里的 `file_path`：
   - `第2讲_Excel函数与图表.pptx` → `backend/file-store/201/demo_lecture02.pptx`
   - `实验二_成绩统计操作指导.docx` → `backend/file-store/201/demo_exp02.docx`
2. 执行 `sql/32_seed_courseware.sql`（幂等，重建这两行资源记录）。
3. 教师端触发一次入库：`POST /api/teacher/ai/kb/sync?courseId=1`。

也可以在教师端「课程内容 → 资料库」直接上传这两个文件——上传接口会**自动入库**，不需要再点同步。

## 自检（可选）

`TestExtract.java` 直接调 `KbExtractor` 抽文本并打印，用来确认页码与讲稿备注抽取正确：

```powershell
& "D:\Jdk17\Jdk17\bin\javac.exe" -encoding UTF-8 -cp "$lib\*;..\..\backend\build\classes\java\main" -d "$env:TEMP\arw-gen" .\TestExtract.java
& "D:\Jdk17\Jdk17\bin\java.exe" -Dfile.encoding=UTF-8 -cp "$lib\*;..\..\backend\build\classes\java\main;$env:TEMP\arw-gen" TestExtract ..\..\demo\courseware\第2讲_Excel函数与图表.pptx
```

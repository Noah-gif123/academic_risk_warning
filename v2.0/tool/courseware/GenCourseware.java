import org.apache.poi.sl.usermodel.Placeholder;
import org.apache.poi.xslf.usermodel.SlideLayout;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFNotes;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

import java.awt.Dimension;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 生成两份演示课件（PPTX + DOCX），用于验证「教师上传课件 → 知识库入库 → 学生答疑带页码引用」整条链路。
 *
 * <p>注意：这是<b>测试数据生成器</b>，不属于业务代码，放在 tool 下，不参与后端编译。</p>
 *
 * <p>用法：java GenCourseware &lt;输出目录&gt; → 第2讲_Excel函数与图表.pptx / 实验二_成绩统计操作指导.docx</p>
 *
 * <p>编译运行见同目录 README.md（需要 POI 的 jar，可直接从后端 fat jar 的 BOOT-INF/lib 里取）。</p>
 */
public class GenCourseware {

    public static void main(String[] args) throws Exception {
        Path outDir = Path.of(args.length > 0 ? args[0] : ".");
        Files.createDirectories(outDir);
        Path pptx = outDir.resolve("第2讲_Excel函数与图表.pptx");
        Path docx = outDir.resolve("实验二_成绩统计操作指导.docx");
        buildPptx(pptx);
        buildDocx(docx);
        System.out.println("OK " + pptx.toAbsolutePath());
        System.out.println("OK " + docx.toAbsolutePath());
    }

    // ============================================================ PPTX
    private static void buildPptx(Path target) throws Exception {
        try (XMLSlideShow ppt = new XMLSlideShow()) {
            ppt.setPageSize(new Dimension(960, 540));
            var master = ppt.getSlideMasters().get(0);

            // 1 封面
            XSLFSlide cover = ppt.createSlide(master.getLayout(SlideLayout.TITLE));
            setPlaceholder(cover, 0, "第2讲  Excel 函数与图表");
            setPlaceholder(cover, 1, "大学计算机基础（一） · 2025级计算机类201班");

            // 2 本讲目标
            slide(ppt, "本讲目标", new String[]{
                    "会用相对引用、绝对引用和跨表引用，避免公式拖动后结果错位",
                    "能用 VLOOKUP / IF / COUNTIF / SUMIF / RANK 完成成绩统计",
                    "能按数据特点选对图表：柱状图比大小、折线图看趋势、饼图看占比"
            }, null);

            // 3 单元格引用
            slide(ppt, "一、单元格引用（公式里最容易错的地方）", new String[]{
                    "相对引用 A1：公式往右/往下拖动时，行列会跟着变",
                    "绝对引用 $A$1：拖动时行列都不变，按 F4 可快速切换",
                    "混合引用 A$1 或 $A1：只锁定行或只锁定列",
                    "跨工作表引用：表名!A1，例如 成绩表!C2；表名含空格时写成 '期末 成绩'!C2"
            }, null);

            // 4 VLOOKUP
            slide(ppt, "二、VLOOKUP：按学号查姓名、查成绩", new String[]{
                    "语法：=VLOOKUP(查找值, 查找区域, 返回列号, 匹配方式)",
                    "返回列号从查找区域的第 1 列开始数，不是从 A 列开始数",
                    "匹配方式写 0（或 FALSE）表示精确匹配，成绩查询一律用 0",
                    "查找值必须出现在查找区域的第 1 列，否则返回 #N/A",
                    "示例：=VLOOKUP(A2, 成绩表!$A$2:$D$51, 4, 0) 取回该学号的卷面成绩"
            }, "查表之前先用「删除重复值」确认学号唯一；查找区域加 $ 锁定，避免拖动时整体偏移。");

            // 5 IF 与 SUMIF
            slide(ppt, "三、IF 与 SUMIF：判断与条件求和", new String[]{
                    "IF：=IF(条件, 条件成立时的值, 条件不成立时的值)，例 =IF(D2>=60,\"及格\",\"不及格\")",
                    "SUMIF：=SUMIF(条件区域, 条件, 求和区域)，例 =SUMIF(C2:C51,\">=60\",D2:D51)",
                    "条件要加英文双引号，如 \">=60\"、\"计算机类201班\"",
                    "文本条件支持通配符 * 和 ?，例如 \"张*\" 统计姓张的学生"
            }, null);

            // 6 统计函数
            slide(ppt, "四、常用统计函数速查", new String[]{
                    "计数：=COUNTIF(B2:B51,\">=90\") 统计 90 分及以上人数",
                    "多条件计数：=COUNTIFS(B2:B51,\">=80\",B2:B51,\"<90\")",
                    "平均分：=AVERAGE(D2:D51)，及格率：=COUNTIF(D2:D51,\">=60\")/COUNT(D2:D51)",
                    "排名：=RANK(D2,$D$2:$D$51)，第二个参数必须绝对引用",
                    "最高最低：=MAX(D2:D51) / =MIN(D2:D51)"
            }, null);

            // 7 图表选择
            slide(ppt, "五、图表怎么选", new String[]{
                    "柱状图：比较不同类别的大小，例如各分数段人数",
                    "折线图：看随时间变化的趋势，例如每周作业提交率",
                    "饼图：看各部分占比，类别不超过 6 类，例如成绩等级分布",
                    "散点图：看两个变量的相关，例如平时成绩与卷面成绩",
                    "图表规范：要有标题、坐标轴名称、图例，不要直接把数据标签关掉"
            }, null);

            // 8 实验二要求（讲稿备注里放评分标准）
            slide(ppt, "六、实验二布置", new String[]{
                    "数据：成绩表.xlsx（学号、姓名、平时成绩、卷面成绩）",
                    "任务：算出总评（平时 40% + 卷面 60%）、统计 5 个分数段人数、算平均分与排名、做 1 张成绩分布柱状图",
                    "提交：文件命名 学号_姓名_实验二.xlsx，上传到学习平台「实验二」作业",
                    "截止：第 4 周周五 23:59，逾期不补交，计入课后作业 40 分中的第 1 次"
            }, "评分标准：统计结果正确 40 分；必须用函数计算、手工输入结果不得分 30 分；图表与格式规范 20 分；文件命名与按时提交 10 分。");

            // 9 常见报错
            slide(ppt, "七、公式报错对照表", new String[]{
                    "#N/A：查找值不在查找区域第 1 列，或数据带空格、格式不一致",
                    "#REF!：公式引用的单元格或区域已被删除",
                    "#VALUE!：参数类型不对，例如把文本当数字参与计算",
                    "#DIV/0!：除数为 0 或分母为空，可用 IFERROR 包一层",
                    "#NAME?：函数名拼错，或使用了未加英文双引号的文本"
            }, null);

            try (OutputStream os = new FileOutputStream(target.toFile())) {
                ppt.write(os);
            }
        }
    }

    /** 内容页：标题 + 要点（可带讲稿备注） */
    private static XSLFSlide slide(XMLSlideShow ppt, String title, String[] bullets, String notes) {
        var master = ppt.getSlideMasters().get(0);
        XSLFSlide slide = ppt.createSlide(master.getLayout(SlideLayout.TITLE_AND_CONTENT));
        setPlaceholder(slide, 0, title);
        // 正文占位符固定是索引 1；POI 默认模板会把 layout 的提示文字（Click to edit Master text styles…）
        // 一起复制进来，必须整体清空再写，否则会混进抽取结果
        XSLFTextShape body = slide.getPlaceholder(1);
        if (body == null) {
            body = slide.createTextBox();
            body.setAnchor(new java.awt.geom.Rectangle2D.Double(60, 140, 840, 340));
        }
        body.clearText();
        for (String b : bullets) {
            XSLFTextParagraph p = body.addNewTextParagraph();
            p.addNewTextRun().setText(b);
        }
        if (notes != null && !notes.isBlank()) {
            try {
                if (ppt.getNotesMaster() == null) {
                    ppt.createNotesMaster();
                }
                XSLFNotes n = ppt.getNotesSlide(slide);
                if (n != null) {
                    XSLFTextShape nb = n.getPlaceholder(1);
                    if (nb != null) {
                        nb.clearText();
                        nb.addNewTextParagraph().addNewTextRun().setText(notes);
                        System.out.println("  [OK] 讲稿备注已写入：" + title);
                    }
                }
            } catch (Exception e) {
                System.out.println("WARN 备注写入失败：" + e.getMessage());
            }
        }
        return slide;
    }

    private static XSLFTextShape bodyPlaceholder(org.apache.poi.xslf.usermodel.XSLFSheet sheet) {
        for (var sh : sheet.getShapes()) {
            if (sh instanceof XSLFTextShape t) {
                try {
                    if (t.isPlaceholder() && t.getTextType() == Placeholder.BODY) {
                        return t;
                    }
                } catch (Exception ignore) {
                    // 占位符类型可能取不到，忽略
                }
            }
        }
        return null;
    }

    private static void setPlaceholder(XSLFSlide slide, int idx, String text) {
        try {
            XSLFTextShape ph = slide.getPlaceholder(idx);
            if (ph != null) {
                ph.clearText();
                ph.addNewTextParagraph().addNewTextRun().setText(text);
            }
        } catch (Exception e) {
            System.out.println("WARN 占位符 " + idx + " 写入失败：" + e.getMessage());
        }
    }

    // ============================================================ DOCX
    private static void buildDocx(Path target) throws Exception {
        try (XWPFDocument doc = new XWPFDocument(); OutputStream os = new FileOutputStream(target.toFile())) {
            title(doc, "实验二  成绩统计操作指导");
            para(doc, "适用课程：大学计算机基础（一）　适用教学班：2025级计算机类201班　实验学时：2 学时");

            h1(doc, "一、实验目的");
            para(doc, "1. 掌握相对引用与绝对引用在公式拖动中的差别；");
            para(doc, "2. 能用 VLOOKUP、IF、COUNTIF、COUNTIFS、AVERAGE、RANK 完成成绩统计；");
            para(doc, "3. 能根据数据类型选择合适的图表，并给图表补齐标题、坐标轴与图例。");

            h1(doc, "二、数据准备");
            para(doc, "打开成绩表.xlsx，确认「成绩表」工作表包含 4 列：A 列学号、B 列姓名、C 列平时成绩、D 列卷面成绩，"
                    + "数据从第 2 行开始，共 50 条记录。原始数据不要修改，所有结果另建「统计」工作表存放。");

            h1(doc, "三、操作步骤");
            h2(doc, "1. 计算总评");
            para(doc, "在「统计」工作表 E2 输入 =C2*0.4+D2*0.6，向下填充到 E51。总评 = 平时成绩 40% + 卷面成绩 60%，"
                    + "与课程导学的成绩构成一致。保留 1 位小数。");
            h2(doc, "2. 统计各分数段人数");
            para(doc, "按 90 分及以上、80-89、70-79、60-69、60 分以下五个区间统计人数。"
                    + "用 COUNTIF 统计单区间，如 =COUNTIF(E2:E51,\">=90\")；"
                    + "用 COUNTIFS 统计双边界区间，如 =COUNTIFS(E2:E51,\">=80\",E2:E51,\"<90\")。"
                    + "不要手工数，手工输入的人数不计分。");
            h2(doc, "3. 计算平均分、最高分与最低分");
            para(doc, "平均分 =AVERAGE(E2:E51)，最高分 =MAX(E2:E51)，最低分 =MIN(E2:E51)，"
                    + "及格率 =COUNTIF(E2:E51,\">=60\")/COUNT(E2:E51)，设置为百分比保留 1 位小数。");
            h2(doc, "4. 计算排名");
            para(doc, "在排名列输入 =RANK(E2,$E$2:$E$51)，第二个参数必须绝对引用，否则向下填充后区域会错位。");
            h2(doc, "5. 制作图表");
            para(doc, "选中分数段与人数两列，插入柱状图，图表标题写「总评成绩分布」，纵轴标题写「人数」。"
                    + "如需展示趋势再补一张折线图；饼图只用于占比，且类别不要超过 6 类。");
            h2(doc, "6. 数据透视表（选做，加分 5 分）");
            para(doc, "以班级为行、总评为值，做一张数据透视表求各班平均分，并与手工统计结果互相核对。");

            h1(doc, "四、提交要求");
            para(doc, "1. 文件命名：学号_姓名_实验二.xlsx（例如 20250001_张三_实验二.xlsx），命名不符合要求的退回重交；");
            para(doc, "2. 提交位置：学习平台 → 本课程 → 作业 → 「实验二 成绩统计」，只交 1 个 xlsx 文件；");
            para(doc, "3. 截止时间：第 4 周周五 23:59，逾期不补交，本次计入课后作业 40 分中的第 1 次（10 分）。");

            h1(doc, "五、评分标准");
            XWPFTable table = doc.createTable(5, 3);
            setRow(table.getRow(0), "评分项", "分值", "说明");
            setRow(table.getRow(1), "统计结果正确", "40", "总评、5 个分数段人数、平均分、排名结果全部正确");
            setRow(table.getRow(2), "函数使用规范", "30", "必须用函数计算，手工输入结果该项不得分");
            setRow(table.getRow(3), "图表与格式规范", "20", "有标题、坐标轴、图例，数据保留位数统一");
            setRow(table.getRow(4), "命名与按时提交", "10", "文件名符合学号_姓名_实验二.xlsx 且按时提交");

            h1(doc, "六、常见问题");
            para(doc, "1. VLOOKUP 返回 #N/A：查找值不在查找区域第一列，或学号前后有空格，用 TRIM 清理后再查；");
            para(doc, "2. 拖动公式后结果全错：多半是相对引用没改成绝对引用，按 F4 切换成 $A$1；");
            para(doc, "3. 排名出现并列跳号：RANK 遇到相同分数会给相同名次，这是正常的；");
            para(doc, "4. 图表横轴是 1、2、3 而不是分数段：选择数据时把首列也选进去，或手动设置横轴标签。");

            doc.write(os);
        }
    }

    private static void title(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(org.apache.poi.xwpf.usermodel.ParagraphAlignment.CENTER);
        XWPFRun r = p.createRun();
        r.setText(text);
        r.setBold(true);
        r.setFontSize(18);
        r.setColor("1F2937");
    }

    private static void h1(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        p.setStyle("Heading1");
        XWPFRun r = p.createRun();
        r.setText(text);
        r.setBold(true);
        r.setFontSize(14);
        r.setColor("1D4ED8");
    }

    private static void h2(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        p.setStyle("Heading2");
        XWPFRun r = p.createRun();
        r.setText(text);
        r.setBold(true);
        r.setFontSize(12);
        r.setColor("334155");
    }

    private static void para(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        XWPFRun r = p.createRun();
        r.setText(text);
        r.setFontSize(11);
    }

    private static void setRow(XWPFTableRow row, String c1, String c2, String c3) {
        row.getCell(0).setText(c1);
        row.getCell(1).setText(c2);
        row.getCell(2).setText(c3);
    }
}

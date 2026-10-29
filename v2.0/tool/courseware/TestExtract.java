import com.example.academic_risk_warning.service.ai.KbExtractor;

import java.nio.file.Path;

/** 临时自检：直接调用 KbExtractor 抽文本，肉眼确认页码与内容。 */
public class TestExtract {
    public static void main(String[] args) throws Exception {
        for (String p : args) {
            Path path = Path.of(p);
            String ext = path.getFileName().toString().toLowerCase();
            ext = ext.substring(ext.lastIndexOf('.') + 1);
            String text = KbExtractor.extract(path, ext);
            System.out.println("========== " + path.getFileName() + " (" + text.length() + " chars) ==========");
            System.out.println(text);
            System.out.println("---------- 行数=" + text.split("\\n").length);
        }
    }
}

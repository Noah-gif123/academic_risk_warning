import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;

/** 把 PDF 逐页渲染成 PNG（用于检查 docx 排版）。用法：java Pdf2Png <in.pdf> <outDir> [dpi] */
public class Pdf2Png {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("usage: java Pdf2Png <in.pdf> <outDir> [dpi=110]");
            return;
        }
        Path out = Path.of(args[1]);
        java.nio.file.Files.createDirectories(out);
        float dpi = args.length > 2 ? Float.parseFloat(args[2]) : 110f;
        try (PDDocument doc = Loader.loadPDF(new File(args[0]))) {
            PDFRenderer renderer = new PDFRenderer(doc);
            for (int i = 0; i < doc.getNumberOfPages(); i++) {
                BufferedImage img = renderer.renderImageWithDPI(i, dpi, ImageType.RGB);
                File f = out.resolve(String.format("p%02d.png", i + 1)).toFile();
                ImageIO.write(img, "png", f);
                System.out.println("page " + (i + 1) + " -> " + f.getName() + " " + img.getWidth() + "x" + img.getHeight());
            }
            System.out.println("pages=" + doc.getNumberOfPages());
        }
    }
}

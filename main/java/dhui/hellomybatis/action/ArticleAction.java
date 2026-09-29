package dhui.hellomybatis.action;

import com.github.pagehelper.PageInfo;
import dhui.hellomybatis.entity.Article;
import dhui.hellomybatis.service.ArticleService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.UUID;

@Controller
public class    ArticleAction {
    @Autowired
    private ArticleService articleService;

    @Autowired
    private ResourceLoader resourceLoader; // 确保这个字段被正确声明和注入

    // 跳转到添加文章页面
    @GetMapping("/article/add")
    public String toAddArticle() {
        return "article_add";
    }

    @RequestMapping("/article/list")
    public String listArticle(Model model,
                              @RequestParam(value = "pageNum", defaultValue = "1") int pageNum,
                              @RequestParam(value = "pageSize", defaultValue = "2") int pageSize) {
        PageInfo<Article> pageInfo = articleService.getArticleListByPage(pageNum, pageSize);
        model.addAttribute("pageInfo", pageInfo);
        return "article_list";
    }

    private String saveFile(MultipartFile file, HttpServletRequest request) {
        // 获取上传文件的原始名称
        String originalFilename = file.getOriginalFilename();
        // 设置上传文件的保存地址目录
        String dir = "/uploads/";
        String dirPath = request.getServletContext().getRealPath(dir);
        // 设置上传文件的保存地址目录
        File filePath = new File(dirPath);
        // 如果保存文件的地址不存在，就先创建目录
        if (!filePath.exists()) {
            filePath.mkdirs();
        }
        // 使用UUID（通用唯一标识）重新命名上传的文件名称(uuid_原始文件名称)
        String newFilename = UUID.randomUUID() + "_" + originalFilename;
        try {
            file.transferTo(new File(dirPath + newFilename));
            return dir + newFilename;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @PostMapping("/article/upload")
    public String addArticle(Article article,
                             @RequestParam("titlePicFile") MultipartFile titlePicFile,
                             @RequestParam("videoFile") MultipartFile videoFile,
                             HttpServletRequest request) {
        if (!titlePicFile.isEmpty()) {
            article.setTitlepic(saveFile(titlePicFile, request));
        }
        if (!videoFile.isEmpty()) {
            article.setVideo(saveFile(videoFile, request));
        }
        articleService.addArticle(article);
        return "redirect:/article/list";
    }

    @GetMapping("/article/download")
    public ResponseEntity<Resource> fileDownload(HttpServletRequest request,
                                                 @RequestParam String filename) throws Exception {
        // 加载资源（需要拼接文件的绝对路径）
        String filePath = request.getServletContext().getRealPath(filename);
        org.springframework.core.io.Resource resource = resourceLoader.getResource("file:" + filePath);

        // 检查资源是否存在
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        // 设置响应头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

        // 对文件名进行编码，防止中文乱码
        String encodedFilename = encodeFilename(request, filename.substring(filename.lastIndexOf("/") + 1));
        headers.setContentDispositionFormData("attachment", encodedFilename);

        return ResponseEntity.ok()
                .headers(headers)
                .body(resource);
    }

    // 中文文件名编码工具方法
    private String encodeFilename(HttpServletRequest request, String filename) throws Exception {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent.contains("MSIE") || userAgent.contains("Trident")) {
            // IE浏览器编码
            return URLEncoder.encode(filename, "UTF-8");
        } else {
            // 其他浏览器编码
            return new String(filename.getBytes("UTF-8"), "ISO-8859-1");
        }
    }
}
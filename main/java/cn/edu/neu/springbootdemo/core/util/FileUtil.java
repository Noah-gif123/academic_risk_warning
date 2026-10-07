package cn.edu.neu.springbootdemo.core.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.springframework.util.ResourceUtils;
import org.springframework.web.multipart.MultipartFile;

import cn.edu.neu.springbootdemo.core.Constants;

public class FileUtil {
	
	//创建文件
	public static File createFile(String filePath){
		File path = null;
		try {
			path = new File(ResourceUtils.getURL("classpath:").getPath());
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (!path.exists()) {
			path = new File("");
		}
		System.out.println("path:"+path.getPath());
		File upload = new File(path.getAbsolutePath(), filePath.substring(0,filePath.lastIndexOf("/")+1));
		if (!upload.exists()) {
			upload.mkdirs();
		}
		System.out.println("upload:"+upload.getPath());
		return new File(path.getAbsolutePath(), filePath);
	}

	//上传文件
	public static File uploadFile(MultipartFile file, HttpServletRequest request){
		String fileName = file.getOriginalFilename();  // 文件名
        String suffixName = fileName.substring(fileName.lastIndexOf("."));  // 后缀名
        fileName = UUID.randomUUID().toString().replaceAll("-", "") + suffixName; // 新文件名
        String filePath=Constants.UPLOAD_PATH+fileName;   
        File dest = FileUtil.createFile(filePath);
        System.out.println("path---:"+dest.getPath()); 
        try {
            file.transferTo(dest);
        } catch (IOException e) {
            e.printStackTrace();
			return null;
        } 
        return dest;
	}
	
	//上传文件到指定磁盘目录，返回重命名后的文件名（失败返回 null）
	public static String uploadFileToDir(MultipartFile file, String dirPath){
		if (file == null || file.isEmpty()) {
			return null;
		}
		String fileName = file.getOriginalFilename();  // 文件名
		String suffixName = fileName.substring(fileName.lastIndexOf("."));  // 后缀名
		fileName = UUID.randomUUID().toString().replaceAll("-", "") + suffixName; // 新文件名
		File dir = new File(dirPath);
		if (!dir.exists()) {
			dir.mkdirs();
		}
		File dest = new File(dir, fileName);
		try {
			file.transferTo(dest);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		return fileName;
	}

	//删除磁盘目录下的单个文件
	public static boolean deleteFileInDir(String dirPath, String fileName){
		if (fileName == null || fileName.trim().equals("")) {
			return false;
		}
		// 只取文件名，避免传入路径时越权删除其它目录的文件
		String name = fileName.contains("/") ? fileName.substring(fileName.lastIndexOf("/") + 1) : fileName;
		File file = new File(dirPath, name);
		return file.exists() && file.delete();
	}

	//删除文件
	public static boolean deleteFile(String filePath){
		File path = null;
		try {
			path = new File(ResourceUtils.getURL("classpath:").getPath());
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		System.out.println("path:"+path);
		File file=new File(path.getAbsolutePath(), filePath);
		System.out.println("file:"+file);
		return file.delete();
	}
	
	//读取文件内容，作为字符串返回
    public static String readFileAsString(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new FileNotFoundException(filePath);
        } 

        if (file.length() > 1024 * 1024 * 1024) {
            throw new IOException("File is too large");
        } 

        StringBuilder sb = new StringBuilder((int) (file.length()));
        // 创建字节输入流  
        FileInputStream fis = new FileInputStream(filePath);  
        // 创建一个长度为10240的Buffer
        byte[] bbuf = new byte[10240];  
        // 用于保存实际读取的字节数  
        int hasRead = 0;  
        while ( (hasRead = fis.read(bbuf)) > 0 ) {  
            sb.append(new String(bbuf, 0, hasRead));  
        }  
        fis.close();  
        return sb.toString();
    }

    //根据文件路径读取byte[]数组
    public static byte[] readFileByBytes(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new FileNotFoundException(filePath);
        } else {
            ByteArrayOutputStream bos = new ByteArrayOutputStream((int) file.length());
            BufferedInputStream in = null;

            try {
                in = new BufferedInputStream(new FileInputStream(file));
                short bufSize = 1024;
                byte[] buffer = new byte[bufSize];
                int len1;
                while (-1 != (len1 = in.read(buffer, 0, bufSize))) {
                    bos.write(buffer, 0, len1);
                }

                byte[] var7 = bos.toByteArray();
                return var7;
            } finally {
                try {
                    if (in != null) {
                        in.close();
                    }
                } catch (IOException var14) {
                    var14.printStackTrace();
                }

                bos.close();
            }
        }
    }
}

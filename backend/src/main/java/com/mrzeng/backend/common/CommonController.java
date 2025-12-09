package com.mrzeng.backend.common;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.io.IOException;

@RestController
@RequestMapping("/common")
public class CommonController {

    @Value("${file.upload-path:./uploads/}")
    private String uploadPath;

    @PostMapping("/upload")
    public Result<String> upload(MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("文件不能为空");
        }

        // Check size (300KB = 300 * 1024 bytes)
        if (file.getSize() > 300 * 1024) {
            return Result.error("文件大小不能超过300KB");
        }

        // Check type
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals("image/jpeg") && !contentType.equals("image/png"))) {
            return Result.error("只支持JPG或PNG格式");
        }

        // Create directory if not exists
        if (!FileUtil.exist(uploadPath)) {
            FileUtil.mkdir(uploadPath);
        }

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String suffix = FileUtil.getSuffix(originalFilename);
        String fileName = IdUtil.simpleUUID() + "." + suffix;

        try {
            // Save file
            file.transferTo(new File(uploadPath + fileName));
            // Return URL (assuming mapped to /images/)
            String url = "http://localhost:8080/images/" + fileName;
            return Result.success(url);
        } catch (IOException e) {
            e.printStackTrace();
            return Result.error("文件上传失败");
        }
    }
}

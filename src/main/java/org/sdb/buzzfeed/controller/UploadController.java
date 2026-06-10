package org.sdb.buzzfeed.controller;

import lombok.RequiredArgsConstructor;
import org.sdb.buzzfeed.entity.Result;
import org.sdb.buzzfeed.entity.vo.UploadVO;
import org.sdb.buzzfeed.service.MinioService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class UploadController {

    private final MinioService minioService;

    @Value("${minio.endpoint}")
    private String minioEndpoint;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @PostMapping("/upload/image")
    public Result uploadImage(@RequestParam("file") MultipartFile file) {
        return doUpload(file, "images");
    }

    @PostMapping("/upload/video")
    public Result uploadVideo(@RequestParam("file") MultipartFile file) {
        return doUpload(file, "videos");
    }

    private Result doUpload(MultipartFile file, String directory) {
        if (file == null || file.isEmpty()) {
            return Result.error("文件不能为空");
        }
        try {
            MinioService.UploadResult uploadResult = minioService.upload(file, directory);
            String url = minioEndpoint + "/" + bucketName + "/" + uploadResult.getObjectName();
            String coverUrl = uploadResult.getCoverUrl() != null
                    ? minioEndpoint + "/" + bucketName + "/" + uploadResult.getCoverUrl()
                    : null;
            return Result.success(new UploadVO(url, uploadResult.getObjectName(), coverUrl));
        } catch (Exception e) {
            return Result.error("文件上传失败: " + e.getMessage());
        }
    }
}

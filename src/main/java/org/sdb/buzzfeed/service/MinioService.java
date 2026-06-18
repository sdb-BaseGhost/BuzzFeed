package org.sdb.buzzfeed.service;

import io.minio.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioService {

    private final MinioClient minioClient;
    private final VideoTranscodeService transcodeService;

    @Value("${minio.file-url-prefix:/files}")
    private String fileUrlPrefix;

    @Value("${minio.bucket-name}")
    private String bucketName;

    /**
     * 上传结果：objectName + 可选的封面 URL
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class UploadResult {
        private String objectName;
        private String coverUrl;  // 视频封面相对路径，可能为 null
    }

    /**
     * 上传文件到 MinIO
     * <p>
     * 视频文件会自动检测编码，H.265 转码为 H.264，并截取封面帧。
     */
    public UploadResult upload(MultipartFile file, String directory) throws Exception {
        boolean exists = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(bucketName).build()
        );
        if (!exists) {
            minioClient.makeBucket(
                    MakeBucketArgs.builder().bucket(bucketName).build()
            );
        }

        String originalFilename = file.getOriginalFilename();
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String objectName = directory + "/" + dateStr + "/" + UUID.randomUUID() + ext;

        // 视频上传前：检测编码，H.265 自动转码为 H.264，同时截取封面帧
        if ("videos".equals(directory) && ".mp4".equalsIgnoreCase(ext)) {
            return uploadVideoWithTranscode(file, ext, objectName);
        } else {
            try (InputStream is = file.getInputStream()) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucketName)
                                .object(objectName)
                                .stream(is, file.getSize(), -1)
                                .contentType(file.getContentType())
                                .build()
                );
            }
            return new UploadResult(objectName, null);
        }
    }

    /**
     * 视频专用上传：检测编码 → 转码 → 截封面 → 上传
     */
    private UploadResult uploadVideoWithTranscode(MultipartFile file, String ext, String objectName) throws Exception {
        InputStream uploadStream = null;
        long uploadSize;
        File tempInput = null;
        File transcoded = null;
        File coverFile = null;
        String coverObjectName = null;

        try {
            // 写到临时文件以供 ffprobe / ffmpeg 读取
            tempInput = Files.createTempFile("upload_src_", ext).toFile();
            file.transferTo(tempInput);

            String codec = transcodeService.detectVideoCodec(tempInput);
            log.info("视频编码检测: codec={}, file={}", codec, file.getOriginalFilename());

            // 用于截帧的源文件：转码成功则用转码后的，否则用原始的
            File sourceForCover = tempInput;

            if (transcodeService.needsTranscode(codec)) {
                log.info("检测到 H.265 视频，开始转码: {}", file.getOriginalFilename());
                transcoded = transcodeService.transcodeToH264(tempInput);
                if (transcoded != null) {
                    sourceForCover = transcoded;
                }
            }

            // 确定最终上传的视频流
            if (transcoded != null) {
                uploadStream = new FileInputStream(transcoded);
                uploadSize = transcoded.length();
            } else {
                uploadStream = new FileInputStream(tempInput);
                uploadSize = tempInput.length();
            }

            // 上传视频到 MinIO
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(uploadStream, uploadSize, -1)
                            .contentType("video/mp4")
                            .build()
            );

            // 截取封面帧并上传
            coverFile = transcodeService.extractCoverFrame(sourceForCover, 1.0);
            if (coverFile != null) {
                coverObjectName = objectName.replace(".mp4", "_cover.jpg");
                try (InputStream coverStream = new FileInputStream(coverFile)) {
                    minioClient.putObject(
                            PutObjectArgs.builder()
                                    .bucket(bucketName)
                                    .object(coverObjectName)
                                    .stream(coverStream, coverFile.length(), -1)
                                    .contentType("image/jpeg")
                                    .build()
                    );
                }
                log.info("封面已上传: {}", coverObjectName);
            }
        } finally {
            if (uploadStream != null) {
                try { uploadStream.close(); } catch (IOException ignored) {}
            }
            if (tempInput != null) {
                try { tempInput.delete(); } catch (Exception ignored) {}
            }
            if (transcoded != null) {
                try { transcoded.delete(); } catch (Exception ignored) {}
            }
            if (coverFile != null) {
                try { coverFile.delete(); } catch (Exception ignored) {}
            }
        }

        return new UploadResult(objectName, coverObjectName);
    }

    public String getFileUrl(String objectName) {
        return fileUrlPrefix + "/" + objectName;
    }
}

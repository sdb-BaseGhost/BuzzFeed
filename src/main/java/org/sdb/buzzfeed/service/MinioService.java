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

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.bucket-name}")
    private String bucketName;

    public String upload(MultipartFile file, String directory) throws Exception {
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

        // 视频上传前：检测编码，H.265 自动转码为 H.264
        if ("videos".equals(directory) && ".mp4".equalsIgnoreCase(ext)) {
            InputStream uploadStream = null;
            long uploadSize;
            File tempInput = null;
            File transcoded = null;

            try {
                // 写到临时文件以供 ffprobe / ffmpeg 读取
                tempInput = Files.createTempFile("upload_src_", ext).toFile();
                file.transferTo(tempInput);

                String codec = transcodeService.detectVideoCodec(tempInput);
                log.info("视频编码检测: codec={}, file={}", codec, originalFilename);

                if (transcodeService.needsTranscode(codec)) {
                    log.info("检测到 H.265 视频，开始转码: {}", originalFilename);
                    transcoded = transcodeService.transcodeToH264(tempInput);
                }

                if (transcoded != null) {
                    // 转码成功：上传转码后的文件
                    uploadStream = new FileInputStream(transcoded);
                    uploadSize = transcoded.length();
                    // objectName 保持 .mp4 后缀
                } else {
                    // 不需要转码 或 转码失败：上传原始文件
                    uploadStream = new FileInputStream(tempInput);
                    uploadSize = tempInput.length();
                }

                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucketName)
                                .object(objectName)
                                .stream(uploadStream, uploadSize, -1)
                                .contentType("video/mp4")
                                .build()
                );
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
            }
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
        }

        return objectName;
    }

    public String getFileUrl(String objectName) {
        return endpoint + "/" + bucketName + "/" + objectName;
    }
}

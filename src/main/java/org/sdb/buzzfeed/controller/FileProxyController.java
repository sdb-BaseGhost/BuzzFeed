package org.sdb.buzzfeed.controller;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequiredArgsConstructor
public class FileProxyController {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @GetMapping("/files/**")
    public void proxyFile(HttpServletResponse response, jakarta.servlet.http.HttpServletRequest request) {
        String fullPath = request.getRequestURI();
        String objectName = URLDecoder.decode(fullPath.substring("/files/".length()), StandardCharsets.UTF_8);

        try (InputStream is = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build())) {

            String contentType = getContentType(objectName);
            response.setContentType(contentType);
            response.setHeader("Cache-Control", "max-age=86400");

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                response.getOutputStream().write(buffer, 0, bytesRead);
            }
            response.getOutputStream().flush();

        } catch (Exception e) {
            response.setStatus(404);
        }
    }

    private String getContentType(String objectName) {
        String lower = objectName.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".mp4")) return "video/mp4";
        if (lower.endsWith(".webm")) return "video/webm";
        return "application/octet-stream";
    }
}

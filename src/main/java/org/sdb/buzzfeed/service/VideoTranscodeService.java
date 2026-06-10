package org.sdb.buzzfeed.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;

/**
 * 视频转码服务：H.265(HEVC) → H.264(AVC)
 * <p>
 * Chrome / Firefox 不支持 HEVC 硬解，上传时自动转码为浏览器兼容的 H.264
 */
@Slf4j
@Service
public class VideoTranscodeService {

    @Value("${ffmpeg.path:ffmpeg}")
    private String ffmpegPath;

    /**
     * 获取 ffprobe 路径（从 ffmpeg 路径推导）
     */
    private String getFfprobePath() {
        // 如果 ffmpegPath 是完整路径，替换文件名
        if (ffmpegPath.contains("ffmpeg")) {
            return ffmpegPath.replace("ffmpeg", "ffprobe");
        }
        return "ffprobe";
    }

    /**
     * 用 ffprobe 检测视频编码
     *
     * @return 编码名称，如 "h264"、"hevc"；检测失败返回 null
     */
    public String detectVideoCodec(File file) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    getFfprobePath(),
                    "-v", "error",
                    "-select_streams", "v:0",
                    "-show_entries", "stream=codec_name",
                    "-of", "default=noprint_wrappers=1:nokey=1",
                    file.getAbsolutePath()
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            String codec = new String(process.getInputStream().readAllBytes()).trim();
            int exit = process.waitFor();
            if (exit == 0 && !codec.isEmpty()) {
                return codec;
            }
        } catch (Exception e) {
            log.warn("ffprobe 检测编码失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 判断是否需要转码（HEVC / H.265 浏览器不支持）
     */
    public boolean needsTranscode(String codec) {
        if (codec == null) return false;
        String c = codec.toLowerCase();
        return c.contains("hevc") || c.contains("h265") || c.contains("hev1") || c.contains("hvc1");
    }

    /**
     * 从视频中截取封面帧（JPEG）
     *
     * @param video    视频文件
     * @param seekTime 截取时间点（秒），一般取 1.0
     * @return 封面 JPEG 临时文件；失败返回 null
     */
    public File extractCoverFrame(File video, double seekTime) {
        try {
            File cover = Files.createTempFile("cover_", ".jpg").toFile();

            ProcessBuilder pb = new ProcessBuilder(
                    ffmpegPath,
                    "-y",
                    "-ss", String.valueOf(seekTime),
                    "-i", video.getAbsolutePath(),
                    "-frames:v", "1",
                    "-q:v", "2",                // JPEG 质量（2=高质量）
                    "-f", "image2",
                    cover.getAbsolutePath()
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            byte[] logBytes = process.getInputStream().readAllBytes();
            int exit = process.waitFor();

            if (exit == 0 && cover.exists() && cover.length() > 0) {
                log.info("封面截取成功: {} ({}KB)", cover.getName(), cover.length() / 1024);
                return cover;
            } else {
                log.error("封面截取失败 exit={}, output: {}", exit, new String(logBytes));
                cover.delete();
            }
        } catch (Exception e) {
            log.error("封面截取异常: {}", e.getMessage(), e);
        }
        return null;
    }

    /**
     * 将视频转码为 H.264 + AAC 封装的 MP4
     *
     * @param input  原始视频文件
     * @return 转码后的临时文件；如果转码失败返回 null
     */
    public File transcodeToH264(File input) {
        try {
            File output = Files.createTempFile("transcode_", ".mp4").toFile();

            ProcessBuilder pb = new ProcessBuilder(
                    ffmpegPath,
                    "-y",                       // 覆盖输出
                    "-i", input.getAbsolutePath(),
                    "-c:v", "libx264",          // 视频编码 H.264
                    "-preset", "fast",           // 编码速度（fast 平衡质量和速度）
                    "-crf", "23",                // 质量因子（越小质量越高，18-28 是合理范围）
                    "-c:a", "aac",              // 音频编码 AAC
                    "-b:a", "128k",             // 音频码率
                    "-movflags", "+faststart",  // 使 MP4 可渐进式下载（moov 前置）
                    "-pix_fmt", "yuv420p",      // 像素格式兼容性最好
                    output.getAbsolutePath()
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // 消费输出流防止阻塞
            byte[] logBytes = process.getInputStream().readAllBytes();
            int exit = process.waitFor();

            if (exit == 0 && output.exists() && output.length() > 0) {
                log.info("转码成功: {} -> {} ({}KB)",
                        input.getName(), output.getName(), output.length() / 1024);
                return output;
            } else {
                log.error("转码失败 exit={}, ffmpeg output: {}", exit, new String(logBytes));
                output.delete();
            }
        } catch (Exception e) {
            log.error("转码异常: {}", e.getMessage(), e);
        }
        return null;
    }
}

package org.sdb.buzzfeed.entity.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * 发布内容请求参数
 * <p>
 * contentType 取值：
 * <ul>
 *   <li>0 — 纯文本</li>
 *   <li>1 — 图文（需上传 1~3 张图片）</li>
 *   <li>2 — 视频（需上传视频文件）</li>
 * </ul>
 */
@Data
public class CreatePostDTO {

    /** 内容类型：0纯文本 1图文 2视频 */
    @NotNull(message = "contentType 不能为空")
    @Min(value = 0, message = "contentType 取值范围 0~2")
    @Max(value = 2, message = "contentType 取值范围 0~2")
    private Integer contentType;

    /** 标题 */
    @NotBlank(message = "标题不能为空")
    @Size(max = 128, message = "标题最长 128 个字符")
    private String title;

    /** 描述/正文（可选） */
    @Size(max = 500, message = "描述最长 500 个字符")
    private String description;

    /** 可见范围：0私密 1好友 2粉丝 3公开，默认 1 */
    private Integer visibility = 1;

    /** 图文类型：图片文件数组（1~3 张） */
    private MultipartFile[] images;

    /** 视频类型：视频文件 */
    private MultipartFile video;
}

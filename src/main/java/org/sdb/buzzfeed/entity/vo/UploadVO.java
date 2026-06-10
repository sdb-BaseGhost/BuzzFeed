package org.sdb.buzzfeed.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 文件上传返回值
 */
@Data
@AllArgsConstructor
public class UploadVO {
    /** 可直接访问的完整 URL */
    private String url;
    /** MinIO 中的对象名（用于后续关联/清理） */
    private String objectName;
    /** 视频封面图 URL（仅视频上传时有值） */
    private String coverUrl;

    /** 便捷构造：无封面场景 */
    public UploadVO(String url, String objectName) {
        this.url = url;
        this.objectName = objectName;
        this.coverUrl = null;
    }
}

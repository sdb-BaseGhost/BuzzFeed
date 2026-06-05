CREATE TABLE `image_info` (
`id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
`item_id` bigint NOT NULL COMMENT '内容ID',
`image_uri` varchar(1024) NOT NULL COMMENT '图片URI',
`sort_order` int NOT NULL DEFAULT '0' COMMENT '排序序号，小的在前',
`version` int NOT NULL DEFAULT '1' COMMENT '关联的版本号，用于区分线上/草稿',
`status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1-正常 2-删除',
`create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
`update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
PRIMARY KEY (`id`),
KEY `idx_item_version_status_order` (`item_id`,`version`,`status`,`sort_order`),
KEY `idx_image_uri` (`image_uri`(200))
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='内容图片表';

CREATE TABLE `item_info` (
`item_id` bigint NOT NULL COMMENT '内容唯一标识',
`creator_id` bigint NOT NULL COMMENT '创作者ID',
`online_version` int NOT NULL DEFAULT '1' COMMENT '线上内容版本号',
`latest_version` int NOT NULL DEFAULT '1' COMMENT '最新变更版本号',
`visibility` int NOT NULL DEFAULT '0' COMMENT '可见范围 0私密 1好友 2粉丝 3公开',
`status` tinyint NOT NULL DEFAULT '0' COMMENT '状态 0待审核 1正常 2删除 3下架',
`item_type` tinyint NOT NULL DEFAULT '0' COMMENT '类别 0纯文本 1图文 2视频 3长文',
`title` varchar(128) DEFAULT '' COMMENT '内容标题',
`summary` varchar(256) DEFAULT '' COMMENT '内容摘要',
`cover_url` varchar(1024) DEFAULT '' COMMENT '内容封面URL',
`extra` text COMMENT '扩展字段(JSON)',
`create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
`publish_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '内容通过审核发布的时间',
`update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
PRIMARY KEY (`item_id`),
KEY `idx_creator_id` (`creator_id`),
KEY `idx_update_time` (`update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='内容主表';

CREATE TABLE `item_text` (
`id` bigint NOT NULL AUTO_INCREMENT,
`item_id` bigint NOT NULL,
`version` int NOT NULL COMMENT '版本号',
`content` longtext NOT NULL COMMENT '文本内容',
`create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
`update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
PRIMARY KEY (`id`),
UNIQUE KEY `uk_item_version` (`item_id`,`version`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='内容文本版本表';

CREATE TABLE `video_info` (
`id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
`item_id` bigint NOT NULL COMMENT '内容ID',
`sort_order` int NOT NULL DEFAULT '0' COMMENT '排序序号，小的在前',
`version` int NOT NULL DEFAULT '1' COMMENT '关联的版本号，用于区分线上/草稿',
`creator_id` bigint NOT NULL COMMENT '上传用户ID',
`bucket_name` varchar(128) NOT NULL COMMENT 'MinIO bucket名称',
`object_name` varchar(512) NOT NULL COMMENT 'MinIO对象名',
`video_url` varchar(1024) NOT NULL COMMENT '视频访问URL',
`cover_url` varchar(1024) DEFAULT '' COMMENT '视频封面URL',
`duration` int DEFAULT '0' COMMENT '视频时长(秒)',
`file_size` bigint DEFAULT '0' COMMENT '文件大小(Byte)',
`width` int DEFAULT '0' COMMENT '视频宽度',
`height` int DEFAULT '0' COMMENT '视频高度',
`bitrate` int DEFAULT '0' COMMENT '码率',
`fps` int DEFAULT '0' COMMENT '帧率',
`codec` varchar(64) DEFAULT '' COMMENT '编码格式',
`status` tinyint NOT NULL DEFAULT '0' COMMENT '0转码中 1正常 2审核失败 3删除',
`create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
`update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
PRIMARY KEY (`id`),
KEY `idx_creator_id` (`creator_id`),
KEY `idx_creator_status_ctime` (`creator_id`,`status`,`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='视频资源表';
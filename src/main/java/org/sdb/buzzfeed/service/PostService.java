package org.sdb.buzzfeed.service;

import org.sdb.buzzfeed.entity.dto.CreatePostDTO;
import org.sdb.buzzfeed.entity.vo.CreatePostVO;
import org.sdb.buzzfeed.entity.vo.FeedItemVO;

import java.util.List;

public interface PostService {
    CreatePostVO postContent(CreatePostDTO dto);

    /** 获取指定用户的帖子列表 */
    List<FeedItemVO> getUserPosts(Long userId, int limit);
}

package org.sdb.buzzfeed.service;

import org.sdb.buzzfeed.entity.dto.CreatePostDTO;
import org.sdb.buzzfeed.entity.vo.CreatePostVO;

public interface PostService {
    CreatePostVO postContent(CreatePostDTO dto);
}

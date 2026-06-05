package org.sdb.buzzfeed.service;

import org.springframework.web.multipart.MultipartFile;

public interface PostService {
    Object postContent(Integer contentType, String title, String description,
                       Integer visibility, MultipartFile[] images, MultipartFile video);
}

package org.sdb.buzzfeed.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.sdb.buzzfeed.entity.Result;
import org.sdb.buzzfeed.entity.dto.CreatePostDTO;
import org.sdb.buzzfeed.service.PostService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @PostMapping("/post")
    public Result post(@Valid @RequestBody CreatePostDTO dto) {
        return Result.success(postService.postContent(dto));
    }

    /**
     * 获取指定用户的帖子列表
     */
    @GetMapping("/api/post/user/{userId}")
    public Result getUserPosts(@PathVariable Long userId,
                               @RequestParam(defaultValue = "20") int limit) {
        return Result.success(postService.getUserPosts(userId, limit));
    }
}

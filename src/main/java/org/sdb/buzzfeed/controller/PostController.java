package org.sdb.buzzfeed.controller;

import lombok.RequiredArgsConstructor;
import org.sdb.buzzfeed.entity.Content;
import org.sdb.buzzfeed.entity.Result;
import org.sdb.buzzfeed.service.PostService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    /**
     * 内容发布模块
     *
     * @return
     */
    @PostMapping("/post")
    public Result post(@RequestBody Content content){
        return Result.success(postService.postContent(content));
    }

    /**
     * 修改内容模块
     */
    private void revise(@RequestBody Content content){

    }
}

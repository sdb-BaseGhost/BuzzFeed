package org.sdb.buzzfeed.controller;

import lombok.RequiredArgsConstructor;
import org.sdb.buzzfeed.entity.Result;
import org.sdb.buzzfeed.entity.User;
import org.sdb.buzzfeed.entity.vo.SearchUserVO;
import org.sdb.buzzfeed.mapper.UserMapper;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final UserMapper userMapper;

    @GetMapping
    public Result search(@RequestParam("q") String q) {
        if (!StringUtils.hasText(q)) {
            return Result.success(Map.of("users", Collections.emptyList(), "posts", Collections.emptyList()));
        }

        List<User> users = userMapper.searchByKeyword(q.trim());
        List<SearchUserVO> userVOs = users.stream()
                .map(SearchUserVO::from)
                .collect(Collectors.toList());

        return Result.success(Map.of(
                "users", userVOs,
                "posts", Collections.emptyList()
        ));
    }
}

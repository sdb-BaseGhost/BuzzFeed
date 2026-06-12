package org.sdb.buzzfeed.entity.vo;

import lombok.Data;
import org.sdb.buzzfeed.entity.User;

@Data
public class SearchUserVO {
    private Long userId;
    private String username;
    private String displayName;
    private String avatar;
    private String bio;
    private Integer followerNumber;

    public static SearchUserVO from(User user) {
        SearchUserVO vo = new SearchUserVO();
        vo.setUserId(user.getUserId());
        vo.setUsername(user.getUsername());
        vo.setDisplayName(user.getDisplayName());
        vo.setAvatar(user.getAvatar());
        vo.setBio(user.getBio());
        vo.setFollowerNumber(user.getFollowerNumber());
        return vo;
    }
}

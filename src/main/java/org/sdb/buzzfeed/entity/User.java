package org.sdb.buzzfeed.entity;

import lombok.Data;

@Data
public class User {
    private Long userId;
    private String username;
    private String passwordHash;
    private String email;
    private String displayName;
    private String bio;
    private String avatar;
    private Boolean isActive;
    private Integer followerNumber;
    private Integer followsNumber;
    private Integer postCount;
}

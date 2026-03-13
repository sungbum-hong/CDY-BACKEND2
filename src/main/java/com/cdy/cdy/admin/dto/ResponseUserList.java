package com.cdy.cdy.admin.dto;

import com.cdy.cdy.domain.users.entity.Users;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ResponseUserList {
    private Long id;
    private String username;
    private String name;
    private String nickname;
    private String userCategory;
    private String role;

    public static ResponseUserList from(Users u) {
        return ResponseUserList.builder()
                .id(u.getId())
                .username(u.getUsername())
                .name(u.getName())
                .nickname(u.getNickname())
                .userCategory(u.getUserCategory() != null ? u.getUserCategory().getDescription() : null)
                .role(u.getRole().name())
                .build();
    }
}

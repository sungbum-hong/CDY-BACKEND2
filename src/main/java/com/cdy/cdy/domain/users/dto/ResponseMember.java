package com.cdy.cdy.domain.users.dto;

import com.cdy.cdy.domain.users.entity.UserRole;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ResponseMember {
    private Long id;
    private String name;
    private String field;
    private String bio;
    private String avatar;
    private UserRole role;
}

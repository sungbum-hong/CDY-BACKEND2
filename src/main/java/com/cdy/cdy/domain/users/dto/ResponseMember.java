package com.cdy.cdy.domain.users.dto;

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
}

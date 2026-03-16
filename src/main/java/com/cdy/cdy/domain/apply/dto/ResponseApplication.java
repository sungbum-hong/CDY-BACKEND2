package com.cdy.cdy.domain.apply.dto;

import com.cdy.cdy.domain.apply.entity.Application;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ResponseApplication {
    private Long id;
    private String name;
    private String email;
    private String nickname;
    private String field;
    private String status;
    private String createdAt;

    public static ResponseApplication from(Application a) {
        return ResponseApplication.builder()
                .id(a.getId())
                .name(a.getName())
                .email(a.getEmail())
                .nickname(a.getNickname())
                .field(a.getField().getDescription())
                .status(a.getStatus().name())
                .createdAt(a.getCreatedAt() != null ? a.getCreatedAt().toString() : "")
                .build();
    }
}

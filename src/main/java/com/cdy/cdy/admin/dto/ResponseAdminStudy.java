package com.cdy.cdy.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResponseAdminStudy {
    private Long id;
    private String title;
    private LocalDateTime createdAt;
    private String authorName;
    private String authorNickname;
    private String userCategory;
    private String imageKey;
}

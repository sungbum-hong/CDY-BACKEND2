package com.cdy.cdy.domain.contest.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ResponseContest {
    private Long id;
    private String title;
    private String organizer;
    private String deadline;
    private String field;
    private String externalUrl;
    private String imageUrl;
    private LocalDateTime createdAt;
}

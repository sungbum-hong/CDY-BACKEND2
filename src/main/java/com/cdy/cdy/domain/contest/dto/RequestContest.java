package com.cdy.cdy.domain.contest.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RequestContest {
    private String title;
    private String organizer;
    private String deadline;
    private String field;
    private String externalUrl;
    private String imageUrl;
}

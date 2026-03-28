package com.cdy.cdy.domain.project.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class RequestProject {
    private String title;
    private String slogan;
    private String description;
    private String imageUrl;
    private Integer maxParticipants;
    private String deadline;
    private String question;
    private String openTalkUrl;
    private List<String> skills;
    private List<String> positions;
}

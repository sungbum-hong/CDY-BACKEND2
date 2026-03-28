package com.cdy.cdy.domain.project.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ResponseProject {
    private Long id;
    private String title;
    private String slogan;
    private String description;
    private String image;
    private Integer maxParticipants;
    private String deadline;
    private String question;
    private String openTalkUrl;
    private Integer likes;
    private List<String> tags;       // skills
    private List<String> positions;
    private String author;           // 작성자 닉네임
    private String authorUsername;   // 작성자 username (이메일)
    private int participants;
}

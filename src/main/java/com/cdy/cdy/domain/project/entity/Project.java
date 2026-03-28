package com.cdy.cdy.domain.project.entity;

import com.cdy.cdy.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "project")
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Builder
public class Project extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "slogan")
    private String slogan;

    @Column(name = "description", columnDefinition = "LONGTEXT")
    private String description;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "max_participants")
    private Integer maxParticipants;

    @Column(name = "deadline")
    private String deadline;

    @Column(name = "question", columnDefinition = "TEXT")
    private String question;

    @Column(name = "open_talk_url", length = 500)
    private String openTalkUrl;

    @Builder.Default
    @Column(name = "likes")
    private Integer likes = 0;

    @Builder.Default
    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "project_skill", joinColumns = @JoinColumn(name = "project_id"))
    @Column(name = "skill")
    private List<String> skills = new ArrayList<>();

    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "project_position", joinColumns = @JoinColumn(name = "project_id"))
    @Column(name = "position_name")
    private List<String> positions = new ArrayList<>();

    public void update(String title, String slogan, String description, String imageUrl,
                       Integer maxParticipants, String deadline, String question,
                       String openTalkUrl, List<String> skills, List<String> positions) {
        this.title = title;
        this.slogan = slogan;
        this.description = description;
        if (imageUrl != null) this.imageUrl = imageUrl;
        if (maxParticipants != null) this.maxParticipants = maxParticipants;
        this.deadline = deadline;
        this.question = question;
        this.openTalkUrl = openTalkUrl;
        this.skills.clear();
        this.skills.addAll(skills);
        this.positions.clear();
        this.positions.addAll(positions);
    }

    public void delete() { this.isDeleted = true; }
}

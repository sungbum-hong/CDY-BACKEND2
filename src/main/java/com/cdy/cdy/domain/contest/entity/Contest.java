package com.cdy.cdy.domain.contest.entity;

import com.cdy.cdy.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "contests")
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Builder
public class Contest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "organizer", nullable = false)
    private String organizer;

    @Column(name = "deadline", nullable = false)
    private String deadline;

    @Column(name = "field")
    private String field;

    @Column(name = "external_url", length = 1000)
    private String externalUrl;

    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    @Builder.Default
    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    public void delete() { this.isDeleted = true; }
}

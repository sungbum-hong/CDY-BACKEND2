package com.cdy.cdy.domain.study.entity;

import com.cdy.cdy.common.entity.BaseEntity;
import com.cdy.cdy.domain.study.dto.RequestStudy;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "study")
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Builder
public class Study extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false; //DB DEFAULT 조건으로 FALSE 지정.

    public void setIsDeleted() {
        this.isDeleted = true;
    }

    public void update(RequestStudy dto) {
        this.title = dto.getTitle();
        this.content =  dto.getContent();


    }
}

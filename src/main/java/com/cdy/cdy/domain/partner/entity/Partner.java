package com.cdy.cdy.domain.partner.entity;

import com.cdy.cdy.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "partners")
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Builder
public class Partner extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    @Column(name = "link_url", length = 1000)
    private String linkUrl;

    @Builder.Default
    @Column(name = "sort_order")
    private Integer sortOrder = 0;
}

package com.cdy.cdy.domain.partner.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RequestPartner {
    private String name;
    private String imageUrl;
    private String linkUrl;
    private Integer sortOrder;
}

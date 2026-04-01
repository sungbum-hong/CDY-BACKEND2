package com.cdy.cdy.domain.service.dto;

import com.cdy.cdy.domain.service.entity.ServiceCategory;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RequestService {
    private String title;
    private String description;
    private Integer price;
    private ServiceCategory category;
    private String thumbnail;
}

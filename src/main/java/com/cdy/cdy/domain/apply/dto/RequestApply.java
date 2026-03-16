package com.cdy.cdy.domain.apply.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RequestApply {
    private String name;
    private String phone;
    private String email;
    private String field; // "CODING" | "DESIGN" | "EDITING"
}

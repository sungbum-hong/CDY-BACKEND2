package com.cdy.cdy.domain.apply.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RequestApprove {
    private String name;
    private String email;
    private String nickname;
    private String phone;
    private String password;
}

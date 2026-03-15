package com.cdy.cdy.domain.users.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResponseMyProfile {


    @Schema(description = "유저 로그인 아이디")
    private String username;
    @Schema(description = "유저 카테고리 ex 영상편집,코딩,디자인")
    private String userCategory;
    @Schema(description = "한 줄 소개(자기 소개 글)")
    private String description;
    @Schema(description = "닉네임")
    private String nickname;
    @Schema(description = "유저 역할 ex ADMIN, USER")
    private String role;

    @Schema(description = "프로필 이미지 URL")
    private String profileImageUrl;

}

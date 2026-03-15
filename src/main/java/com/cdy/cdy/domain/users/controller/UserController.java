package com.cdy.cdy.domain.users.controller;

import com.cdy.cdy.domain.users.dto.ResponseMyProfile;
import com.cdy.cdy.domain.users.dto.UserRequestDto;
import com.cdy.cdy.domain.users.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "유저프로필 정보 조회",description = """
            로그인 된 유저의 프로필 정보를 조회옵니다.
            """)
    @GetMapping("/getProfileInfo")
    public ResponseEntity<?> getProfileInfo(Authentication authentication) {

        ResponseMyProfile result = userService.getProfileInfo(authentication.getName());
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "유저프로필 변경 로직", description = """
            로그인 된 상태에서 유저가 자신의 프로필을 변경하는 API
            기존의 유저 로그인 아이디와(username)과 dto의 username이 값이 다르면 에러발생
            """)
    @PutMapping("changeMyPage")
    public ResponseEntity<?> changeMyPage(Authentication authentication,
                                          @RequestBody UserRequestDto dto) {

        userService.changeMyPage(authentication.getName(), dto);
        return ResponseEntity.ok("마이프로필 변경 완료.");

    }

    @Operation(summary = "프로필 이미지 URL 저장")
    @PutMapping("/profileImageUrl")
    public ResponseEntity<?> updateProfileImageUrl(Authentication authentication,
                                                   @RequestBody java.util.Map<String, String> body) {
        userService.updateProfileImageUrl(authentication.getName(), body.get("imageUrl"));
        return ResponseEntity.ok("프로필 이미지 URL 저장 완료.");
    }

}

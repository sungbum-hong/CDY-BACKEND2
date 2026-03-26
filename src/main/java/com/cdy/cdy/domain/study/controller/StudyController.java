package com.cdy.cdy.domain.study.controller;

import com.cdy.cdy.domain.study.dto.RequestStudy;
import com.cdy.cdy.domain.study.dto.ResponseStudy;
import com.cdy.cdy.domain.study.dto.ResponseStudyListByUser;
import com.cdy.cdy.domain.study.service.StudyService;
import com.cdy.cdy.domain.users.dto.ResponseMember;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RequestMapping("/api/v1/study")
@RestController
public class StudyController {

    private final StudyService studyService;

    @Operation(summary = "스터디 글 작성", description = """
            스터디글과 스터디글에 포함된 스터디이미지를 작성하는 API
            로그인한 사용자만 접근가능.
            """)
    @PostMapping("/create")
    public ResponseEntity<?> createStudy(Authentication authentication,
                                         @RequestBody RequestStudy dto) {
        studyService.createStudy(authentication.getName(), dto);
        return ResponseEntity.ok("스터디 글이 작성되었습니다.");
    }

    @Operation(summary = "스터디 글 삭제", description = """
            스터디 글 삭제로직, soft_delete 처리 ,
            글의 작성자만 삭제가능
            """)
    @DeleteMapping("/delete/{studyId}")
    public ResponseEntity<?> deleteStudy(Authentication authentication, @PathVariable("studyId") Long studyId) {
        studyService.deleteStudy(authentication.getName(), studyId);
        return ResponseEntity.ok("스터디 글이 삭제 되었습니다.");
    }

    @Operation(summary = "스터디 글 수정", description = """
            스터디 글 수정 로직 , 새로들어온 dto값으로 전체 변경,
            이미지 마찬가지(이미지 list가 비어있을경우 이미지 전체삭제)
            """)
    @PutMapping("/update/{studyId}")
    public ResponseEntity<?> updateStudy(Authentication authentication, @PathVariable("studyId") Long studyId,
                                         @RequestBody RequestStudy dto) {
        studyService.updateStudy(authentication.getName(), dto, studyId);
        return ResponseEntity.ok("스터디 글이 수정 되었습니다.");
    }

    @Operation(summary = "스터디 글 단건 상세조회", description = """
            스터디 ID를 기준으로 해당하는 스터디글 상세조회 로직
            """)
    @GetMapping("/findById/{studyId}")
    public ResponseEntity<ResponseStudy> findById(@PathVariable("studyId") Long studyId) {
        ResponseStudy responseStudy = studyService.findById(studyId);
        return ResponseEntity.ok(responseStudy);
    }

    @Operation(summary = "유저의 스터디 작성 목록 조회", description = """
            로그인한 유저의 작성한 전체 스터디목록 조회
            """)
    @GetMapping("/findByUser")
    public ResponseEntity<?> findByUser(Authentication authentication,
                                        @PageableDefault(page = 0, size = 5, sort = "createdAt", direction = Sort.Direction.DESC
                                        ) Pageable pageable) {
        Page<ResponseStudyListByUser> result = studyService.findByUser(authentication.getName(), pageable);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "특정 유저의 스터디 목록 조회 (공개)", description = """
            userId로 해당 유저의 스터디 목록 조회 (비로그인 공개)
            """)
    @GetMapping("/findByUser/{userId}")
    public ResponseEntity<?> findByUserId(@PathVariable("userId") Long userId,
                                          @PageableDefault(page = 0, size = 1000, sort = "createdAt", direction = Sort.Direction.DESC
                                          ) Pageable pageable) {
        Page<ResponseStudyListByUser> result = studyService.findByUserId(userId, pageable);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "카테고리별 크루 멤버 목록 조회", description = """
            category 파라미터로 coding / design / video 중 하나를 전달하면
            해당 카테고리에 속한 크루 멤버 목록을 반환합니다. (비로그인 공개)
            """)
    @GetMapping("/members")
    public ResponseEntity<List<ResponseMember>> getMembers(@RequestParam String category) {
        return ResponseEntity.ok(studyService.getMembers(category));
    }
}

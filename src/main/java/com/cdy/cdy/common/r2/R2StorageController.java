package com.cdy.cdy.common.r2;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URL;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/image")
@RequiredArgsConstructor
public class R2StorageController {

    private final R2StorageService storageService;

    /** 
     * 1. 업로드 presign URL 발급
     * 프론트에서 파일 이름, contentType 보내면 서버에서 key 생성 후 presign URL 반환
     */
    @Operation(
            summary = "이미지 업로드 presign URL 발급",
            description = "프론트가 먼저 호출해서 presign URL을 받아온 뒤, 해당 URL로 이미지를 직접 PUT 업로드합니다."
    )
    @PostMapping("/presign-put")
    public Map<String, String> getPresignPut(
            @RequestParam String filename,
            @RequestParam String contentType
    ) {
        // key 규칙대로 생성
        String key = storageService.buildKey(filename);
        URL url = storageService.presignPut(key, contentType);

        return Map.of(
                "key", key,   // 업로드 후 DB에 저장할 key
                "url", url.toString() // 프론트가 PUT할 presigned URL
        );
    }

    /**
     * 2. 특정 key에 대한 presign GET URL 발급
     */
    @Operation(
            summary = "이미지 조회 presign URL 발급",
            description = "저장된 key를 기반으로 일정 시간 동안(1시간)만 유효한 조회 URL을 반환합니다."
    )
    @GetMapping("/presign-get")
    public Map<String, String> getPresignGet(
            @RequestParam String key
    ) {
        int seconds = 3600;
        URL url = storageService.presignGet(key, seconds);
        return Map.of("url", url.toString());
    }

    /**
     * 3. 퍼블릭 버킷일 경우 고정 URL 반환 (선택사항)
     */
    @GetMapping("/public-url")
    public Map<String, String> getPublicUrl(@RequestParam String key) {
        String url = storageService.publicUrl(key);
        return Map.of("url", url);
    }

    /**
     * 4. 서버 사이드 업로드 (multipart/form-data → R2 → URL 반환)
     */
    @PostMapping("/upload")
    public Map<String, String> upload(@RequestParam("file") MultipartFile file) {
        String url = storageService.upload(file);
        return Map.of("url", url);
    }
}

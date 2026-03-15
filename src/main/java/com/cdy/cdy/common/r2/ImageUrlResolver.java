package com.cdy.cdy.common.r2;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ImageUrlResolver {

    private final R2StorageService r2StorageService;


    public String toPresignedUrl(String key) {
        if (key == null || key.isBlank()) return null;
        return r2StorageService.publicUrl(key);
    }

}

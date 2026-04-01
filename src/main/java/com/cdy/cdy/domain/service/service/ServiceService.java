package com.cdy.cdy.domain.service.service;

import com.cdy.cdy.common.r2.ImageUrlResolver;
import com.cdy.cdy.domain.service.dto.RequestService;
import com.cdy.cdy.domain.service.dto.ResponseService;
import com.cdy.cdy.domain.service.entity.Service;
import com.cdy.cdy.domain.service.entity.ServiceStatus;
import com.cdy.cdy.domain.service.repository.ServiceRepository;
import com.cdy.cdy.domain.users.entity.Users;
import com.cdy.cdy.domain.users.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class ServiceService {

    private final ServiceRepository serviceRepository;
    private final UserRepository userRepository;
    private final ImageUrlResolver imageUrlResolver;

    @Transactional
    public ResponseService create(String username, RequestService dto) {
        Users user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("유저를 찾을 수 없습니다."));

        Service service = Service.builder()
                .userId(user.getId())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .price(dto.getPrice())
                .category(dto.getCategory())
                .thumbnail(dto.getThumbnail())
                .build();

        serviceRepository.save(service);
        return toResponse(service, user.getNickname());
    }

    public List<ResponseService> findAll() {
        return serviceRepository.findByStatusOrderByCreatedAtDesc(ServiceStatus.ACTIVE)
                .stream()
                .map(s -> {
                    String nickname = userRepository.findById(s.getUserId())
                            .map(Users::getNickname)
                            .orElse(null);
                    return toResponse(s, nickname);
                })
                .toList();
    }

    public ResponseService findById(Long id) {
        Service service = serviceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("서비스를 찾을 수 없습니다."));
        if (service.getStatus() == ServiceStatus.INACTIVE) {
            throw new EntityNotFoundException("비활성화된 서비스입니다.");
        }
        String nickname = userRepository.findById(service.getUserId())
                .map(Users::getNickname)
                .orElse(null);
        return toResponse(service, nickname);
    }

    private ResponseService toResponse(Service s, String nickname) {
        String thumbnailUrl = imageUrlResolver.toPresignedUrl(s.getThumbnail());
        return ResponseService.builder()
                .id(s.getId())
                .userId(s.getUserId())
                .userNickname(nickname)
                .title(s.getTitle())
                .description(s.getDescription())
                .price(s.getPrice())
                .category(s.getCategory())
                .thumbnail(thumbnailUrl != null ? thumbnailUrl : s.getThumbnail())
                .status(s.getStatus())
                .createdAt(s.getCreatedAt())
                .build();
    }
}

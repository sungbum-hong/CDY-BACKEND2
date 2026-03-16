package com.cdy.cdy.domain.apply.service;

import com.cdy.cdy.domain.apply.dto.RequestApply;
import com.cdy.cdy.domain.apply.dto.RequestApprove;
import com.cdy.cdy.domain.apply.dto.ResponseApplication;
import com.cdy.cdy.domain.apply.entity.Application;
import com.cdy.cdy.domain.apply.repository.ApplicationRepository;
import com.cdy.cdy.domain.users.entity.UserCategory;
import com.cdy.cdy.domain.users.entity.Users;
import com.cdy.cdy.domain.users.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void apply(RequestApply dto) {
        Application application = Application.builder()
                .name(dto.getName())
                .email(dto.getEmail())
                .nickname(dto.getNickname())
                .field(UserCategory.valueOf(dto.getField()))
                .createdAt(LocalDateTime.now())
                .build();
        applicationRepository.save(application);
        log.info("[Apply] 신청 저장 - name: {}, email: {}", dto.getName(), dto.getEmail());
    }

    public List<ResponseApplication> getApplications() {
        return applicationRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(ResponseApplication::from)
                .toList();
    }

    @Transactional
    public void approve(Long id, RequestApprove dto) {
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("신청을 찾을 수 없습니다."));

        if (userRepository.existsByUsername(dto.getEmail())) {
            throw new IllegalArgumentException("이미 존재하는 아이디입니다.");
        }

        Users user = Users.builder()
                .username(dto.getEmail())
                .name(dto.getName())
                .nickname(dto.getNickname())
                .phoneNumber(dto.getPhone())
                .password(passwordEncoder.encode(dto.getPassword()))
                .userCategory(application.getField())
                .build();

        userRepository.save(user);
        application.approve();
        log.info("[Apply] 신청 승인 - id: {}, email: {}", id, dto.getEmail());
    }

    @Transactional
    public void reject(Long id) {
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("신청을 찾을 수 없습니다."));
        application.reject();
        log.info("[Apply] 신청 거절 - id: {}", id);
    }
}

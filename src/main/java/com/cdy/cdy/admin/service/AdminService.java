package com.cdy.cdy.admin.service;

import com.cdy.cdy.admin.dto.RequestChangePassword;
import com.cdy.cdy.admin.dto.RequestPromoteAdmin;
import com.cdy.cdy.admin.dto.ResponseUserList;
import com.cdy.cdy.domain.users.dto.UserRequestDto;
import com.cdy.cdy.domain.users.entity.UserRole;
import com.cdy.cdy.domain.users.entity.Users;
import com.cdy.cdy.domain.users.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    public void createUser(String username, UserRequestDto userRequestDto) {
        Users admin = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("[AdminService] 관리자 조회 실패 - username : {}", username);
                    throw new UsernameNotFoundException("존재하지 않는 유저");
                });

        if (!admin.getRole().equals(UserRole.ADMIN)) {
            log.warn("[AdminService] 관리자 권한 실패 - username : {}", username);
            throw new IllegalArgumentException("관리자만 가능.");
        }

        if (userRepository.findByUsername(userRequestDto.getUsername()).isPresent()) {
            log.warn("[AdminService] 유저 아이디 중복 username - {}", userRequestDto.getUsername());
            throw new IllegalArgumentException("이미 존재하는 아이디입니다.");
        }

        Users users = Users.builder()
                .username(userRequestDto.getUsername())
                .name(userRequestDto.getName())
                .phoneNumber(userRequestDto.getPhoneNumber())
                .userCategory(userRequestDto.getUserCategory())
                .nickname(userRequestDto.getNickname())
                .password(passwordEncoder.encode(userRequestDto.getPassword()))
                .build();

        userRepository.save(users);
        log.info("[AdminService] 유저 생성 , createdUsername : {}", username);
    }

    public List<ResponseUserList> getUsers() {
        return userRepository.findAll()
                .stream()
                .filter(u -> !u.getIsDeleted())
                .map(ResponseUserList::from)
                .toList();
    }

    @Transactional
    public void changePassword(Long userId, RequestChangePassword dto) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 유저"));
        user.changePassword(passwordEncoder.encode(dto.getNewPassword()));
        log.info("[AdminService] 비밀번호 변경 - userId : {}", userId);
    }

    @Transactional
    public void changeRole(Long userId, String role) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 유저"));
        UserRole userRole = UserRole.valueOf(role);
        user.changeRole(userRole);
        log.info("[AdminService] 권한 변경 - userId: {}, role: {}", userId, role);
    }

    @Transactional
    public void deleteUser(Long userId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 유저"));
        user.softDelete();
        log.info("[AdminService] 유저 삭제 - userId : {}", userId);
    }

    private static final String BOOTSTRAP_SECRET = "cdy-admin-bootstrap-2026";

    @Transactional
    public void promoteToAdmin(RequestPromoteAdmin dto) {
        if (!BOOTSTRAP_SECRET.equals(dto.getSecretKey())) {
            throw new IllegalArgumentException("잘못된 시크릿 키입니다.");
        }
        Users user = userRepository.findByUsername(dto.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 유저"));
        user.promoteToAdmin();
        log.info("[AdminService] ADMIN 권한 부여 - username : {}", dto.getUsername());
    }
}

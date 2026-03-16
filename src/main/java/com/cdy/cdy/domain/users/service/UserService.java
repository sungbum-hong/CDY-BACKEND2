package com.cdy.cdy.domain.users.service;

import com.cdy.cdy.domain.users.dto.ResponseMyProfile;
import com.cdy.cdy.domain.users.dto.UserRequestDto;
import com.cdy.cdy.domain.users.entity.Users;
import com.cdy.cdy.domain.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    @Transactional(readOnly = true)
    @Override  //로그필터 이후 dto로 들어온 로그인정보를 DB에서 가져온 정보랑 체크하는 메서드
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        Users users = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("유저를 찾을 수 없습니다."));

        if (Boolean.TRUE.equals(users.getIsDeleted())) {
            throw new UsernameNotFoundException("유저를 찾을 수 없습니다.");
        }

        return User.builder()
                .username(username)
                .password(users.getPassword())
                .roles(users.getRole().name())
                .build();

    }

    /**
     * 로그인 된 유저의 프로필정보 조회
     */
    public ResponseMyProfile getProfileInfo(String username) {

        Users users = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("유저를 찾을 수 없습니다."));

        if (Boolean.TRUE.equals(users.getIsDeleted())) {
            throw new UsernameNotFoundException("유저를 찾을 수 없습니다.");
        }

        return ResponseMyProfile.builder()
                .nickname(users.getNickname())
                .userCategory(users.getUserCategory().toString())
                .username(users.getUsername())
                .description(users.getDescription())
                .role(users.getRole().name())
                .profileImageUrl(users.getProfileImageUrl())
                .build();
    }

    /**
     *
     * @param username
     * @param dto
     * 로그인한 유저의 프로필 정보 변경 로직
     */
    @Transactional
    public void updateProfileImageUrl(String username, String imageUrl) {
        Users users = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("유저를 찾을 수 없습니다."));
        users.updateProfileImageUrl(imageUrl);
    }

    @Transactional
    public void agreeTerms(String username) {
        Users users = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("유저를 찾을 수 없습니다."));
        users.agreeTerms();
    }

    public void changeMyPage(String username,UserRequestDto dto) {

        Users users = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("유저를 찾을 수 없습니다."));

        if (!users.getUsername().equals(dto.getUsername())) {
            throw new IllegalArgumentException("본인만 정보를 변경 할 수 있습니다.");
        }

        String encodedPassword = (dto.getPassword() != null && !dto.getPassword().isEmpty())
                ? bCryptPasswordEncoder.encode(dto.getPassword())
                : users.getPassword();
        users.updateProfile(dto, encodedPassword);
        userRepository.save(users);

    }
}
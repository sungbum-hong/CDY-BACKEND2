package com.cdy.cdy.domain.auth;

import com.cdy.cdy.domain.users.dto.UserRequestDto;
import com.cdy.cdy.domain.users.entity.Users;
import com.cdy.cdy.domain.users.repository.UserRepository;
import com.cdy.cdy.domain.users.service.UserService;
import com.cdy.cdy.security.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;


    //회원가입 로직
    public void signUp(UserRequestDto dto) {

        boolean existsByUsername = userRepository.existsByUsername(dto.getUsername());

        if (existsByUsername) {
            throw new IllegalArgumentException("이미 존재하는 아이디입니다.");
        }

        Users users = Users.builder()
                .username(dto.getUsername())
                .password(bCryptPasswordEncoder.encode(dto.getPassword()))
                .nickname(dto.getNickname())
                .isDeleted(false)
                .userCategory(dto.getUserCategory())
                .build();

        userRepository.save(users);

    }






   //로그인 로직
    public String login(UserRequestDto userRequestDto) {


        Users users = userRepository.findByUsername(userRequestDto.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 아이디"));

        if (!bCryptPasswordEncoder.matches(userRequestDto.getPassword(), users.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 틀렸습니다.");
        }

        String jwt = jwtUtil.createJWT(users.getUsername(), users.getRole().toString(), true);

        return jwt;

    }




}

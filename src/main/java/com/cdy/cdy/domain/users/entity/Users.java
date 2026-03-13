package com.cdy.cdy.domain.users.entity;

import com.cdy.cdy.domain.users.dto.UserRequestDto;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@AllArgsConstructor
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Users {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username")
    private String username;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @Column(name = "role", nullable = false)
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private UserRole role = UserRole.USER;

    @Column(name = "profile_image_key")
    private String profileImageKey;

    @Column(name = "password", nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_category")
    private UserCategory userCategory;

    @Column(name = "description")
    private String description;

    @Column(name = "nickname", unique = true)
    private String nickname;

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    public void updateProfile(UserRequestDto dto, String encodedPassword) {
        this.description = dto.getDescription();
        this.userCategory = dto.getUserCategory();
        this.password = encodedPassword;
        this.nickname = dto.getNickname();
    }

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }
}

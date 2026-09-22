package com.cdy.cdy.domain.benefit.entity;

import com.cdy.cdy.domain.users.entity.Users;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 혜택 발급 기록. 같은 혜택을 여러 번 받을 수 있으므로 유니크 제약을 두지 않는다.
 */
@Entity
@Table(name = "benefit_claims")
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Builder
public class BenefitClaim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "benefit_id", nullable = false)
    private Benefit benefit;

    @Column(name = "claimed_at", nullable = false)
    private LocalDateTime claimedAt;
}

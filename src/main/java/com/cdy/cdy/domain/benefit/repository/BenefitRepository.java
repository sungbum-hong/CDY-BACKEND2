package com.cdy.cdy.domain.benefit.repository;

import com.cdy.cdy.domain.benefit.dto.ResponseBenefitStats;
import com.cdy.cdy.domain.benefit.entity.Benefit;
import com.cdy.cdy.domain.benefit.entity.BenefitStatus;
import com.cdy.cdy.domain.partner.entity.PartnerCategory;
import com.cdy.cdy.domain.partner.entity.PartnerStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BenefitRepository extends JpaRepository<Benefit, Long> {

    // partnerStatus IS NULL 조건은 status 컬럼 추가 이전에 생성된 기존 파트너 행을 위한 방어
    @Query("""
            SELECT b
            FROM Benefit b
            JOIN FETCH b.partner p
            WHERE b.status = :status
              AND (p.status IS NULL OR p.status = :partnerStatus)
              AND (b.validFrom IS NULL OR b.validFrom <= :today)
              AND (b.validTo IS NULL OR b.validTo >= :today)
            ORDER BY b.sortOrder ASC, b.id DESC
            """)
    List<Benefit> findActive(@Param("status") BenefitStatus status,
                             @Param("partnerStatus") PartnerStatus partnerStatus,
                             @Param("today") LocalDate today);

    @Query("""
            SELECT b
            FROM Benefit b
            JOIN FETCH b.partner p
            WHERE b.status = :status
              AND (p.status IS NULL OR p.status = :partnerStatus)
              AND p.category = :category
              AND (b.validFrom IS NULL OR b.validFrom <= :today)
              AND (b.validTo IS NULL OR b.validTo >= :today)
            ORDER BY b.sortOrder ASC, b.id DESC
            """)
    List<Benefit> findActiveByCategory(@Param("status") BenefitStatus status,
                                       @Param("partnerStatus") PartnerStatus partnerStatus,
                                       @Param("today") LocalDate today,
                                       @Param("category") PartnerCategory category);

    @Query("""
            SELECT b
            FROM Benefit b
            JOIN FETCH b.partner p
            WHERE b.id = :id
            """)
    Optional<Benefit> findWithPartnerById(@Param("id") Long id);

    boolean existsByPartnerId(Long partnerId);

    /** 어드민 목록. HIDDEN·기간만료 포함 전체 */
    @Query("""
            SELECT b
            FROM Benefit b
            JOIN FETCH b.partner p
            ORDER BY b.sortOrder ASC, b.id DESC
            """)
    List<Benefit> findAllWithPartner();

    @Query("""
            SELECT new com.cdy.cdy.domain.benefit.dto.ResponseBenefitStats(
                b.id, b.title, p.name, COUNT(c.id), MAX(c.claimedAt))
            FROM Benefit b
            JOIN b.partner p
            LEFT JOIN BenefitClaim c ON c.benefit = b
            GROUP BY b.id, b.title, p.name
            ORDER BY COUNT(c.id) DESC, b.id DESC
            """)
    List<ResponseBenefitStats> findClaimStats();
}

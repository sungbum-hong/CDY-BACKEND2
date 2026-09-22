package com.cdy.cdy.domain.benefit.repository;

import com.cdy.cdy.domain.benefit.dto.ResponseMyBenefitClaim;
import com.cdy.cdy.domain.benefit.entity.BenefitClaim;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BenefitClaimRepository extends JpaRepository<BenefitClaim, Long> {

    @Query("""
            SELECT new com.cdy.cdy.domain.benefit.dto.ResponseMyBenefitClaim(
                c.id, b.id, b.title, p.name, c.claimedAt)
            FROM BenefitClaim c
            JOIN c.benefit b
            JOIN b.partner p
            WHERE c.user.id = :userId
            ORDER BY c.claimedAt DESC, c.id DESC
            """)
    List<ResponseMyBenefitClaim> findMyClaims(@Param("userId") Long userId);
}

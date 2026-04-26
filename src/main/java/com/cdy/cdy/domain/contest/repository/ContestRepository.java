package com.cdy.cdy.domain.contest.repository;

import com.cdy.cdy.domain.contest.entity.Contest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ContestRepository extends JpaRepository<Contest, Long> {
    List<Contest> findAllByIsDeletedFalseOrderByCreatedAtDesc();

    // 마감일이 오늘 이후인 공모전만 조회 (deadline은 'YYYY-MM-DD' 형식)
    @Query("SELECT c FROM Contest c WHERE c.isDeleted = false AND c.deadline >= :today ORDER BY c.createdAt DESC")
    List<Contest> findActiveContests(@Param("today") String today);
}

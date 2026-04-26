package com.cdy.cdy.domain.contest.repository;

import com.cdy.cdy.domain.contest.entity.Contest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContestRepository extends JpaRepository<Contest, Long> {
    List<Contest> findAllByIsDeletedFalseOrderByCreatedAtDesc();
}

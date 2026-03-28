package com.cdy.cdy.domain.project.repository;

import com.cdy.cdy.domain.project.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findAllByIsDeletedFalseOrderByCreatedAtDesc();
    List<Project> findAllByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(Long userId);
}

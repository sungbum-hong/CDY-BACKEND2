package com.cdy.cdy.domain.project.service;

import com.cdy.cdy.domain.project.dto.RequestProject;
import com.cdy.cdy.domain.project.dto.ResponseProject;
import com.cdy.cdy.domain.project.entity.Project;
import com.cdy.cdy.domain.project.repository.ProjectRepository;
import com.cdy.cdy.domain.users.entity.Users;
import com.cdy.cdy.domain.users.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    // 전체 프로젝트 목록 조회 (공개)
    public List<ResponseProject> findAll() {
        List<Project> projects = projectRepository.findAllByIsDeletedFalseOrderByCreatedAtDesc();

        // userId 목록으로 유저 정보 한번에 조회
        List<Long> userIds = projects.stream().map(Project::getUserId).distinct().toList();
        Map<Long, Users> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(Users::getId, u -> u));

        return projects.stream().map(p -> {
            Users user = userMap.get(p.getUserId());
            return ResponseProject.builder()
                    .id(p.getId())
                    .title(p.getTitle())
                    .slogan(p.getSlogan())
                    .description(p.getDescription())
                    .image(p.getImageUrl())
                    .maxParticipants(p.getMaxParticipants())
                    .deadline(p.getDeadline())
                    .question(p.getQuestion())
                    .openTalkUrl(p.getOpenTalkUrl())
                    .likes(p.getLikes())
                    .tags(p.getSkills() != null ? p.getSkills() : new ArrayList<>())
                    .positions(p.getPositions() != null ? p.getPositions() : new ArrayList<>())
                    .author(user != null ? user.getNickname() : "")
                    .authorUsername(user != null ? user.getUsername() : "")
                    .participants(1)
                    .build();
        }).toList();
    }

    // 단건 조회
    public ResponseProject findById(Long id) {
        Project p = projectRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 프로젝트"));
        if (p.getIsDeleted()) throw new EntityNotFoundException("삭제된 프로젝트");

        Users user = userRepository.findById(p.getUserId()).orElse(null);
        return ResponseProject.builder()
                .id(p.getId())
                .title(p.getTitle())
                .slogan(p.getSlogan())
                .description(p.getDescription())
                .image(p.getImageUrl())
                .maxParticipants(p.getMaxParticipants())
                .deadline(p.getDeadline())
                .question(p.getQuestion())
                .openTalkUrl(p.getOpenTalkUrl())
                .likes(p.getLikes())
                .tags(p.getSkills() != null ? p.getSkills() : new ArrayList<>())
                .positions(p.getPositions() != null ? p.getPositions() : new ArrayList<>())
                .author(user != null ? user.getNickname() : "")
                .authorUsername(user != null ? user.getUsername() : "")
                .participants(1)
                .build();
    }

    // 프로젝트 생성
    @Transactional
    public void create(String username, RequestProject dto) {
        Users user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 사용자"));

        Project project = Project.builder()
                .userId(user.getId())
                .title(dto.getTitle())
                .slogan(dto.getSlogan())
                .description(dto.getDescription())
                .imageUrl(dto.getImageUrl())
                .maxParticipants(dto.getMaxParticipants())
                .deadline(dto.getDeadline())
                .question(dto.getQuestion())
                .openTalkUrl(dto.getOpenTalkUrl())
                .skills(dto.getSkills() != null ? dto.getSkills() : new ArrayList<>())
                .positions(dto.getPositions() != null ? dto.getPositions() : new ArrayList<>())
                .build();

        projectRepository.save(project);
    }

    // 프로젝트 수정
    @Transactional
    public void update(String username, Long projectId, RequestProject dto) {
        Users user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 사용자"));
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 프로젝트"));
        if (!project.getUserId().equals(user.getId()))
            throw new IllegalArgumentException("작성자만 수정할 수 있습니다.");

        project.update(dto.getTitle(), dto.getSlogan(), dto.getDescription(), dto.getImageUrl(),
                dto.getMaxParticipants(), dto.getDeadline(), dto.getQuestion(), dto.getOpenTalkUrl(),
                dto.getSkills() != null ? dto.getSkills() : new ArrayList<>(),
                dto.getPositions() != null ? dto.getPositions() : new ArrayList<>());
    }
}

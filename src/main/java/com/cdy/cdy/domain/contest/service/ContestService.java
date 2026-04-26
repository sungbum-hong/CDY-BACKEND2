package com.cdy.cdy.domain.contest.service;

import com.cdy.cdy.domain.contest.dto.RequestContest;
import com.cdy.cdy.domain.contest.dto.ResponseContest;
import com.cdy.cdy.domain.contest.entity.Contest;
import com.cdy.cdy.domain.contest.repository.ContestRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ContestService {

    private final ContestRepository contestRepository;

    public List<ResponseContest> findAll() {
        return contestRepository.findAllByIsDeletedFalseOrderByCreatedAtDesc()
                .stream()
                .map(c -> ResponseContest.builder()
                        .id(c.getId())
                        .title(c.getTitle())
                        .organizer(c.getOrganizer())
                        .deadline(c.getDeadline())
                        .field(c.getField())
                        .externalUrl(c.getExternalUrl())
                        .createdAt(c.getCreatedAt())
                        .build())
                .toList();
    }

    @Transactional
    public void create(RequestContest dto) {
        contestRepository.save(Contest.builder()
                .title(dto.getTitle())
                .organizer(dto.getOrganizer())
                .deadline(dto.getDeadline())
                .field(dto.getField())
                .externalUrl(dto.getExternalUrl())
                .build());
    }

    @Transactional
    public void delete(Long id) {
        Contest contest = contestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 공모전"));
        contest.delete();
    }
}

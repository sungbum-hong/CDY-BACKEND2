package com.cdy.cdy.domain.study.service;

import com.cdy.cdy.common.r2.ImageUrlResolver;
import com.cdy.cdy.domain.study.dto.*;
import com.cdy.cdy.domain.study.entity.Study;
import com.cdy.cdy.domain.study.entity.StudyImage;
import com.cdy.cdy.domain.study.repository.StudyImageRepository;
import com.cdy.cdy.domain.study.repository.StudyRepository;
import com.cdy.cdy.domain.study.repository.StudyRepositoryJDBC;
import com.cdy.cdy.domain.users.dto.ResponseMember;
import com.cdy.cdy.domain.users.entity.UserCategory;
import com.cdy.cdy.domain.users.entity.Users;
import com.cdy.cdy.domain.users.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StudyService {

    private final UserRepository userRepository;
    private final StudyRepository studyRepository;
    private final StudyRepositoryJDBC studyRepositoryJDBC;
    private final StudyImageRepository studyImageRepository;
    private final ImageUrlResolver imageUrlResolver;


    //스터디 작성 저장
    @Transactional
    public void createStudy(String username,RequestStudy dto) {

        //로그인 유저 정보 뽑기
        Users users = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("유저를 찾을 수 없습니다."));

        //스터디 dto->entity 변환 후 저장
        Study study = Study.builder()
                .title(dto.getTitle())
                .content(dto.getContent())
                .userId(users.getId())
                .build();

        studyRepository.save(study);

        //스터디 이미지 dto->entity 변환 후 저장
        if (dto.getImageList() != null && !dto.getImageList().isEmpty()) {

            List<StudyImage> studyImageList = dto.getImageList().stream().map((imageDto) ->

                    StudyImage.builder()
                            .studyId(study.getId())
                            .imageKey(imageDto.getImageKey())
                            .sortOrder(imageDto.getSortOrder())
                            .build()
            ).toList();
            studyImageRepository.saveAll(studyImageList);
        }
    }
    //스터디 글 삭제처리(soft delete 처리)
    @Transactional
    public void deleteStudy(String username,Long studyId) {

        Users users = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 사용자"));


        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 스터디"));

        if (study.getUserId() != users.getId()) {
            throw new IllegalArgumentException("작성자만 삭제할 수 있습니다.");
        }
        //삭제 메서드호출(컬럼 is_deleted 상태만 변경)
        study.setIsDeleted();
    }

    @Transactional
    public void updateStudy(String username, RequestStudy dto,Long studyId) {

        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 스터디"));

        Users users = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 사용자"));

        if (!study.getUserId().equals(users.getId())) {
            throw new IllegalArgumentException("작성자만 수정할 수 있습니다..");
        }

        study.update(dto);
        studyRepository.save(study);

        studyImageRepository.deleteAllByStudyId(studyId);
        List<StudyImage> studyImageList = new ArrayList<>();

        for (int i = 0; i < dto.getImageList().size(); i++) {
            RequestStudyImage requestStudyImage = dto.getImageList().get(i);
            StudyImage studyImage = StudyImage.builder()
                    .studyId(studyId)
                    .sortOrder(requestStudyImage.getSortOrder())
                    .imageKey(requestStudyImage.getImageKey())
                    .build();
            studyImageList.add(studyImage);
        }
        studyImageRepository.saveAll(studyImageList);
    }

    public ResponseStudy findById(Long studyId) {

        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 스터디"));

        if (study.getIsDeleted().equals(true)) {
            throw new EntityNotFoundException("삭제된 스터디");
        }

        List<StudyImage> studyImageList = studyImageRepository.findByStudyId(studyId);
        List<ResponseStudyImage> responseStudyImages = studyImageList.stream().map(ent ->

                ResponseStudyImage.builder()
                        .imageUrl(imageUrlResolver.toPresignedUrl(ent.getImageKey()))
                        .sortOrder(ent.getSortOrder())
                        .build()
        ).toList();

        ResponseStudy responseStudy = ResponseStudy.builder()
                .id(studyId)
                .title(study.getTitle())
                .content(study.getContent())
                .studyImageList(responseStudyImages)
                .createdAt(study.getCreatedAt())
                .updatedAt(study.getUpdatedAt())
                .build();

        return responseStudy;
    }

    public Page<ResponseStudyListByUser> findByUser(String username, Pageable pageable) {

        Users users = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 사용자"));

        List<ResponseStudyListByUser> studyListByUsers = studyRepositoryJDBC.findByUser(users.getId(), pageable);

        studyListByUsers.stream().forEach(dto -> {
            dto.setFirstImageUrl(imageUrlResolver.toPresignedUrl(dto.getFirstImageUrl()));
            dto.setUserProfileImageUrl(imageUrlResolver.toPresignedUrl(dto.getUserProfileImageUrl()));
        });

        Long totalCount = studyRepository.findTotalCountByUserId(users.getId());

        return new PageImpl<>(studyListByUsers, pageable, totalCount);
    }

    // 카테고리별 크루 멤버 목록 조회 (비로그인 공개)
    public List<ResponseMember> getMembers(String category) {
        UserCategory userCategory = switch (category.toLowerCase()) {
            case "design"  -> UserCategory.DESIGN;
            case "video"   -> UserCategory.EDITING;
            default        -> UserCategory.CODING;
        };

        return userRepository.findAllByUserCategoryAndIsDeletedFalse(userCategory)
                .stream()
                .map(u -> ResponseMember.builder()
                        .id(u.getId())
                        .name(u.getNickname())
                        .field(userCategory.getDescription())
                        .bio(u.getDescription())
                        .avatar(imageUrlResolver.toPresignedUrl(u.getProfileImageKey()))
                        .build()
                ).toList();
    }
}

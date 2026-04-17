package com.cdy.cdy.domain.study.repository;

import com.cdy.cdy.admin.dto.ResponseAdminStudy;
import com.cdy.cdy.domain.study.dto.RequestStudy;
import com.cdy.cdy.domain.study.dto.ResponseStudyListByUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class StudyRepositoryJDBC {

    private final NamedParameterJdbcTemplate template;

    public List<ResponseAdminStudy> findAllForAdmin() {
        String sql = """
                SELECT
                  s.id          AS id,
                  s.title       AS title,
                  s.created_at  AS created_at,
                  u.name        AS author_name,
                  u.nickname    AS author_nickname,
                  u.user_category AS user_category,
                  si.imagekey   AS imagekey
                FROM study s
                LEFT JOIN users u ON u.id = s.user_id
                LEFT JOIN study_image si ON si.study_id = s.id AND si.sort_order = 0
                WHERE s.is_deleted = 0
                ORDER BY s.created_at DESC
                """;

        return template.query(sql, new MapSqlParameterSource(), (rs, rowNum) ->
                ResponseAdminStudy.builder()
                        .id(rs.getLong("id"))
                        .title(rs.getString("title"))
                        .createdAt(rs.getTimestamp("created_at") == null ? null
                                : rs.getTimestamp("created_at").toLocalDateTime())
                        .authorName(rs.getString("author_name"))
                        .authorNickname(rs.getString("author_nickname"))
                        .userCategory(rs.getString("user_category"))
                        .imageKey(rs.getString("imagekey"))
                        .build()
        );
    }

    //스터디 글 수정 메서드
    public int updateStudy(RequestStudy dto,Long studyId) {


        String sql = """
                
                UPDATE study 
                SET 
                  title = COALESCE(:title, title),
                  content = COALESCE(:content, content)
                WHERE id = :studyId
                """;

        MapSqlParameterSource params = new MapSqlParameterSource();

        params.addValue("title", dto.getTitle());
        params.addValue("content", dto.getContent());
        params.addValue("studyId", studyId);

        int update = template.update(sql, params);
        return update;
    }

    /**
     * 유저의 스터디 목록을 전체 조회 (페이징처리)
     * 유저의 프로필 이미지, 스터디 제목,내용,작성날짜,이미지(첫번째거만)
     */
    public List<ResponseStudyListByUser> findByUser(Long userId, Pageable pageable) {

        String sql = """
                
                SELECT 
                s.id AS id,
                s.title AS title,
                s.content AS content,
                s.created_at AS created_at,
                u.profile_image_key AS profile_image_key,
                si.imagekey AS imagekey
                
                FROM study s
                JOIN users u
                ON u.id = s.user_id
                LEFT JOIN study_image si
                ON si.study_id = s.id
                AND si.sort_order = 1
                
                WHERE s.user_id = :userId
                AND s.is_deleted = 0
                ORDER BY s.created_at DESC
                LIMIT :limit OFFSET :offset
                
                """;

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("userId", userId);
        params.addValue("limit", pageable.getPageSize());
        params.addValue("offset", pageable.getOffset());

        List<ResponseStudyListByUser> result = template.query(sql, params, (rs, roNum) ->

                ResponseStudyListByUser.builder()
                        .id(rs.getLong("id"))
                        .userProfileImageUrl(rs.getString("profile_image_key"))
                        .firstImageUrl(rs.getString("imagekey"))
                        .title(rs.getString("title"))
                        .content(rs.getString("content"))
                        .createdAt(
                                rs.getTimestamp("created_at") == null
                                        ? null : rs.getTimestamp("created_at").toLocalDateTime()

                        )
                        .build()

        );
        return result;

    }


}

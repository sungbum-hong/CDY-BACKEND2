package com.cdy.cdy.domain.users.repository;

import com.cdy.cdy.domain.users.entity.UserCategory;
import com.cdy.cdy.domain.users.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<Users, Long> {

    @Query("""
    SELECT u
    FROM Users u
    WHERE u.username = :username
""")
    Optional<Users> findByUsername(@Param("username") String username);

    @Query("""
SELECT CASE WHEN COUNT(u) > 0
       THEN true ELSE false END
FROM Users u
WHERE u.username = :username
""")
    boolean existsByUsername(String username);

    @Query(value = """
        SELECT u.*
        FROM users u
        LEFT JOIN study s ON s.user_id = u.id AND s.is_deleted = 0
        WHERE u.user_category = :category
          AND u.is_deleted = 0
        GROUP BY u.id
        ORDER BY MAX(s.created_at) DESC
    """, nativeQuery = true)
    List<Users> findMembersByCategoryOrderByLatestStudy(@Param("category") String category);
}

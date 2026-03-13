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
public interface UserRepository extends JpaRepository<Users,Long> {

    @Query("""

    SELECT u 
    FROM Users u
    WHERE u.username = :username

""")
    Optional<Users> findByUsername(@Param("username") String username);


    /**
     *  불린값 반환 3가지 케이스
     *  case when + exists
     *  case when + count
     *  exists + native query
     */
    @Query("""
SELECT CASE WHEN COUNT(u) > 0 
       THEN true ELSE false END
FROM Users u
WHERE u.username = :username
""")
    boolean existsByUsername(String username);

    List<Users> findAllByUserCategoryAndIsDeletedFalse(UserCategory userCategory);
}

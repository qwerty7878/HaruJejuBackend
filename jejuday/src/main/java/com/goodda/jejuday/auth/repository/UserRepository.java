package com.goodda.jejuday.auth.repository;

import com.goodda.jejuday.auth.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    Optional<User> findById(Long id);

    @Query("select distinct u from User u left join fetch u.userThemes")
    List<User> findAllWithThemes();

    @Query("select u from User u left join fetch u.userThemes where u.id = :id")
    Optional<User> findByIdWithThemes(@Param("id") Long id);

    Optional<User> findByNickname(String nickname);

    boolean existsByNickname(String nickname);

    boolean existsByEmail(String email);

    void deleteByEmail(String email);
}

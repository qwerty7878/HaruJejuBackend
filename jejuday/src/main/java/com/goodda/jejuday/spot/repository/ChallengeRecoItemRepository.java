package com.goodda.jejuday.spot.repository;

import com.goodda.jejuday.spot.entity.ChallengeRecoItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;


public interface ChallengeRecoItemRepository extends JpaRepository<ChallengeRecoItem, Long> {
    @Query("select i from ChallengeRecoItem i where i.userId=:userId and i.expiresAt > :now order by i.slotIndex asc")
    List<ChallengeRecoItem> findActiveByUser(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Modifying
    @Query("delete from ChallengeRecoItem i where i.userId=:userId")
    void deleteAllByUser(@Param("userId") Long userId);
}
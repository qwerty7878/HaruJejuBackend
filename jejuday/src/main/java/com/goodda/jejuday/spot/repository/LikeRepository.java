package com.goodda.jejuday.spot.repository;

import com.goodda.jejuday.auth.entity.User;
import com.goodda.jejuday.spot.entity.Like;
import com.goodda.jejuday.spot.entity.Spot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LikeRepository extends JpaRepository<Like, Long> {

    int countByTargetIdAndTargetType(Long targetId, Like.TargetType type);

    boolean existsByUserIdAndTargetTypeAndTargetId(Long userId, Like.TargetType type, Long targetId);

    void deleteByUserIdAndTargetTypeAndTargetId(Long userId, Like.TargetType type, Long targetId);

    long countBySpotId(Long spotId);

    boolean existsByUserIdAndSpotId(Long userId, Long spotId);

    /**
     * 해당 유저가 해당 스팟에 좋아요를 눌렀는지 확인
     */
    boolean existsByUserAndSpot(User user, Spot spot);

    /**
     * 해당 유저가 해당 스팟에 눌렀던 좋아요 엔티티를 조회
     */
    Optional<Like> findByUserAndSpot(User user, Spot spot);

    /**
     * 스팟 전체 좋아요 개수 조회
     */
    long countBySpot(Spot spot);
}
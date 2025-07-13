package com.goodda.jejuday.spot.repository;

import com.goodda.jejuday.spot.entitiy.Bookmark;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {
    boolean existsByUserIdAndSpotId(Long userId, Long spotId);
    void deleteByUserIdAndSpotId(Long userId, Long spotId);
}
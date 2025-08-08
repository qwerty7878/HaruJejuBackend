package com.goodda.jejuday.crawler.repository;

import com.goodda.jejuday.crawler.entitiy.JejuEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface JejuEventRepository extends JpaRepository<JejuEvent, Long> {
    Optional<JejuEvent> findByContentsId(String contentsId);
}
package com.goodda.jejuday.crawler.service;

import com.goodda.jejuday.crawler.dto.CommunityEventBannerDto;
import com.goodda.jejuday.crawler.entitiy.JejuEvent;
import com.goodda.jejuday.crawler.repository.JejuEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommunityEventBannerService {

    private final JejuEventRepository jejuEventRepository;

    /**
     * 배너 API 응답 캐싱
     * - 캐시 키: "banners::{날짜}"
     * - TTL: 5분 (RedisConfig에서 설정)
     * - 성능: 10초 → 50ms 이하로 개선
     * - unless: 빈 리스트는 캐싱하지 않음 (Redis 역직렬화 에러 방지)
     */
    @Cacheable(value = "banners", key = "#date.toString()", unless = "#result == null || #result.isEmpty()")
    public List<CommunityEventBannerDto> findBanners(LocalDate date) {
        log.info("[Cache MISS] Fetching banners for date: {}", date);
        List<JejuEvent> list = jejuEventRepository.findActiveOn(date);
        return list.stream().map(CommunityEventBannerDto::from).toList();
    }
}
package com.goodda.jejuday.spot.service;

import com.goodda.jejuday.auth.entity.User;
import com.goodda.jejuday.auth.repository.UserRepository;
import com.goodda.jejuday.auth.repository.UserThemeRepository;
import com.goodda.jejuday.auth.util.SecurityUtil;
import com.goodda.jejuday.spot.dto.SpotCreateRequestDTO;
import com.goodda.jejuday.spot.dto.SpotDetailResponse;
import com.goodda.jejuday.spot.dto.SpotResponse;
import com.goodda.jejuday.spot.dto.SpotUpdateRequest;
import com.goodda.jejuday.spot.entity.Bookmark;
import com.goodda.jejuday.spot.entity.Like;
import com.goodda.jejuday.spot.entity.Spot;
import com.goodda.jejuday.spot.entity.SpotViewLog;
import com.goodda.jejuday.spot.repository.BookmarkRepository;
import com.goodda.jejuday.spot.repository.LikeRepository;
import com.goodda.jejuday.spot.repository.SpotRepository;
import com.goodda.jejuday.spot.repository.SpotViewLogRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SpotServiceImpl implements SpotService {
    private final SpotRepository spotRepository;
    private final LikeRepository likeRepository;
    private final BookmarkRepository bookmarkRepository;
    private final SpotViewLogRepository viewLogRepository;
//    private final UserRepository userRepository;
    private final SecurityUtil securityUtil;
    private final UserThemeRepository userThemeRepository;


    // 지도용: SPOT, CHALLENGE 만
    private static final Iterable<Spot.SpotType> MAP_VISIBLE =
            Arrays.asList(Spot.SpotType.SPOT, Spot.SpotType.CHALLENGE);

    // 커뮤니티 페이지용: 모든 타입
    private static final Iterable<Spot.SpotType> ALL_TYPES =
            Arrays.asList(Spot.SpotType.values());

    // 1
    @Override
    public List<SpotResponse> getNearbySpots(BigDecimal lat, BigDecimal lng, int radiusKm) {
        return spotRepository.findWithinRadius(lat, lng, radiusKm).stream()
                .filter(s -> s.getType() == Spot.SpotType.SPOT || s.getType() == Spot.SpotType.CHALLENGE)
                .map(s -> SpotResponse.fromEntity(
                        s,
                        likeRepository.countByTargetIdAndTargetType(s.getId(), Like.TargetType.SPOT),
                        false
                ))
                .collect(Collectors.toList());
    }

    @Override
    public Page<SpotResponse> getLatestSpots(Pageable pageable) {
        return spotRepository
                .findByTypeInOrderByCreatedAtDesc(ALL_TYPES, pageable)
                .map(spot ->
                        SpotResponse.fromEntity(
                                spot,
                                (int) likeRepository.countBySpotId(spot.getId()), // 좋아요 개수
                                false // 현재 사용자가 눌렀는지 여부 (로그인 기반으로 수정 가능)
                        )
                );
    }

    @Override
    public Page<SpotResponse> getMostViewedSpots(Pageable pageable) {
        return spotRepository
                .findByTypeInOrderByViewCountDesc(ALL_TYPES, pageable)
                .map(spot ->
                        SpotResponse.fromEntity(
                                spot,
                                (int) likeRepository.countBySpotId(spot.getId()),
                                false
                        )
                );
    }

    @Override
    public Page<SpotResponse> getMostLikedSpots(Pageable pageable) {
        return spotRepository
                .findByTypeInOrderByLikeCountDesc(ALL_TYPES, pageable)
                .map(spot ->
                        SpotResponse.fromEntity(
                                spot,
                                (int) likeRepository.countBySpotId(spot.getId()),
                                false
                        )
                );
    }



    @Override
    @Transactional
    public Long createSpot(SpotCreateRequestDTO req) {
        User user = securityUtil.getAuthenticatedUser();
        Spot s = new Spot(
                req.getName(),
                req.getDescription(),
                req.getLatitude(),
                req.getLongitude(),
                user
        );
        s.setUserCreated(true);
        s.setIsDeleted(false);
        applyTheme(s, req.getThemeId());
        applyTags(s, req.getTag1(), req.getTag2(), req.getTag3());
        
        return spotRepository.save(s).getId();
    }

    private void applyTheme(Spot s, Long themeId) {
        if (themeId == null) { s.setTheme(null); return; }
        s.setTheme(userThemeRepository.findById(themeId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid themeId: " + themeId)));
    }

    private void applyTags(Spot s, String tag1, String tag2, String tag3) {
        // 정규화: 앞의 '#' 제거, trim, 빈문자 -> null, 길이 제한
        s.setTag1(normalizeTag(tag1));
        s.setTag2(normalizeTag(tag2));
        s.setTag3(normalizeTag(tag3));

        // (선택) 중복 제거: 같은 태그 중복 시 하나만 남기고 뒤를 null 처리
        dedupeTags(s);
    }

    private String normalizeTag(String raw) {
        if (raw == null) return null;
        String t = raw.trim();
        if (t.startsWith("#")) t = t.substring(1).trim();
        if (t.isEmpty()) return null;
        if (t.length() > 50) t = t.substring(0, 50);
        return t;
    }

    private void dedupeTags(Spot s) {
        Set<String> seen = new HashSet<>();
        String t1 = s.getTag1(), t2 = s.getTag2(), t3 = s.getTag3();
        s.setTag1(keepOrNull(seen, t1));
        s.setTag2(keepOrNull(seen, t2));
        s.setTag3(keepOrNull(seen, t3));
    }
    private String keepOrNull(Set<String> seen, String v) {
        if (v == null) return null;
        String key = v.toLowerCase();
        if (seen.add(key)) return v;
        return null;
    }

    @Override
    @Transactional
    public SpotDetailResponse getSpotDetail(Long id) {
        User user = securityUtil.getAuthenticatedUser();

        // 테마/태그까지 한 번에 패치
        Spot s = spotRepository.findDetailWithUserAndTagsById(id)
                .orElseThrow(() -> new EntityNotFoundException("Spot not found"));

        // 1) ViewLog 기록
        SpotViewLog log = new SpotViewLog();
        log.setSpot(s);
        log.setUserId(user.getId());
        log.setViewedAt(LocalDateTime.now());
        viewLogRepository.save(log);

        // 2) viewCount++
        s.setViewCount(s.getViewCount() + 1);
        spotRepository.save(s);

        // 3) 응답 생성
        int likeCount = s.getLikeCount();
        boolean liked = likeRepository.existsByUserAndSpot(user, s);
        boolean bookmarked = bookmarkRepository.existsByUserIdAndSpotId(user.getId(), id);
        return new SpotDetailResponse(s, likeCount, liked, bookmarked);
    }


    @Override
    @Transactional
    public void updateSpot(Long id, SpotUpdateRequest req) {
        User user = securityUtil.getAuthenticatedUser();
        Spot s = spotRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Spot not found"));
        if (!Objects.equals(s.getUser().getId(), user.getId())) {
            throw new SecurityException("본인의 Spot만 수정할 수 있습니다.");
        }
        s.setName(req.getName());
        s.setDescription(req.getDescription());
        s.setLatitude(req.getLatitude());
        s.setLongitude(req.getLongitude());
        spotRepository.save(s);
    }


    @Override
    @Transactional
    public void deleteSpot(Long id) {
        User user = securityUtil.getAuthenticatedUser();
        Spot s = spotRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Spot not found"));
        if (!Objects.equals(s.getUser().getId(), user.getId())) {
            throw new SecurityException("본인의 Spot만 삭제할 수 있습니다.");
        }
        s.setIsDeleted(true);
        s.setDeletedAt(LocalDateTime.now());
        s.setDeletedBy(user.getId());
        spotRepository.save(s);
    }

    @Override
    @Transactional
    public void likeSpot(Long spotId) {
        User current = securityUtil.getAuthenticatedUser();
        Spot spot = spotRepository.findById(spotId)
                .orElseThrow(() -> new EntityNotFoundException("Spot not found"));

        // 1) 중계 테이블에 기록
        if ( ! likeRepository.existsByUserAndSpot(current, spot) ) {
            likeRepository.save(new Like(current, spot));
            // 2) Spot.likeCount ++
            spot.setLikeCount(spot.getLikeCount() + 1);
            spotRepository.save(spot);
        }
    }

    @Override
    @Transactional
    public void unlikeSpot(Long spotId) {
        User current = securityUtil.getAuthenticatedUser();
        Spot spot = spotRepository.findById(spotId)
                .orElseThrow(() -> new EntityNotFoundException("Spot not found"));

        // 1) 중계 테이블 삭제
        likeRepository.findByUserAndSpot(current, spot)
                .ifPresent(like -> {
                    likeRepository.delete(like);
                    // 2) Spot.likeCount --
                    spot.setLikeCount(spot.getLikeCount() - 1);
                    spotRepository.save(spot);
                });
    }

//    @Override
//    @Transactional
//    public void bookmarkSpot(Long spotId, Long userId) {
//        if (!bookmarkRepository.existsByUserIdAndSpotId(userId, spotId)) {
//            User user = userRepository.findById(userId)
//                    .orElseThrow(() -> new EntityNotFoundException("User not found"));
//            Spot spot = spotRepository.findById(spotId)
//                    .orElseThrow(() -> new EntityNotFoundException("Spot not found"));
//
//            bookmarkRepository.save(new Bookmark(user, spot));
//        }
//    }


    @Override
    @Transactional
    public void bookmarkSpot(Long id) {
        User user = securityUtil.getAuthenticatedUser();
        if (!bookmarkRepository.existsByUserIdAndSpotId(user.getId(), id)) {
            Spot s = spotRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Spot not found"));
            bookmarkRepository.save(new Bookmark(user, s));
        }
    }

    @Override
    @Transactional
    public void unbookmarkSpot(Long id) {
        User user = securityUtil.getAuthenticatedUser();
        bookmarkRepository.deleteByUserIdAndSpotId(user.getId(), id);
    }

    @Override
    public Spot getSpotById(Long spotId) {
        return spotRepository.findById(spotId)
                .orElseThrow(() -> new EntityNotFoundException("Spot not found with id: " + spotId));
    }
}
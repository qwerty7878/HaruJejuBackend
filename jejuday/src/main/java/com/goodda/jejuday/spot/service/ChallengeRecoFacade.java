package com.goodda.jejuday.spot.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.goodda.jejuday.auth.entity.User;
import com.goodda.jejuday.auth.util.SecurityUtil;
import com.goodda.jejuday.spot.dto.ChallengeResponse;
import com.goodda.jejuday.spot.entity.ChallengeRecoItem;
import com.goodda.jejuday.spot.entity.ChallengeRecoSnapshot;
import com.goodda.jejuday.spot.entity.Spot;
import com.goodda.jejuday.spot.repository.ChallengeRecoItemRepository;
import com.goodda.jejuday.spot.repository.ChallengeRecoSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

// ChallengeRecoFacade.java (핵심만)
@Service
@RequiredArgsConstructor
public class ChallengeRecoFacade {

	private final ChallengeRecCacheService redisCache;
	private final ChallengeRecoSnapshotRepository snapshotRepo;
	private final ChallengeRecoItemRepository itemRepo;
	private final ChallengeQueryService queryService;
	private final SecurityUtil securityUtil;
	private final ObjectMapper om;

	public List<Long> getUpcomingSpotIds() {
		User me = securityUtil.getAuthenticatedUser();
		Long userId = me.getId();
		LocalDateTime now = LocalDateTime.now();

		// 1) Redis
		List<Long> ids = redisCache.load(userId);
		if (ids != null && !ids.isEmpty()) return ids;

		// 2) DB 슬롯 기록 우선
		List<ChallengeRecoItem> active = itemRepo.findActiveByUser(userId, now);
		if (!active.isEmpty()) {
			List<Long> fromItems = active.stream().map(ChallengeRecoItem::getSpotId).toList();
			redisCache.save(userId, fromItems);   // 캐시 채우기(옵션)
			return fromItems;
		}

		// 3) 스냅샷 헤더(백워드 호환)
		ChallengeRecoSnapshot snap = snapshotRepo.findById(userId).orElse(null);
		if (snap != null && !snap.isDirty() && snap.getExpiresAt().isAfter(now)) {
			List<Long> fromSnap = readIds(snap.getSpotIdsJson());
			redisCache.save(userId, fromSnap);
			return fromSnap;
		}

		// 4) 재계산 → DB 슬롯+헤더 저장 → Redis 저장
		List<ChallengeResponse> recs = queryService.generateUpcomingPersonalized4Internal(me);
		List<Long> freshIds = recs.stream().map(ChallengeResponse::getId).toList();

		LocalDateTime expires = now.plusDays(2);

		// 슬롯 재작성
		itemRepo.deleteAllByUser(userId);
		for (int i = 0; i < freshIds.size(); i++) {
			Long spotId = freshIds.get(i);
			Spot spot = queryService.findSpot(spotId); // 아래 유틸 추가
			Long themeId = (spot != null && spot.getTheme() != null) ? spot.getTheme().getId() : null;
			String reason = queryService.isUserPrefTheme(me, themeId) ? "THEME_MATCH" : "RANDOM_FILL";

			ChallengeRecoItem item = new ChallengeRecoItem();
			item.setUserId(userId);
			item.setSlotIndex(i);
			item.setSpotId(spotId);
			item.setThemeId(themeId);
			item.setReason(reason);
			item.setGeneratedAt(now);
			item.setExpiresAt(expires);
			itemRepo.save(item);
		}

		// 헤더(백워드 호환)
		ChallengeRecoSnapshot n = (snap != null ? snap : new ChallengeRecoSnapshot());
		n.setUserId(userId);
		n.setSpotIdsJson(writeIds(freshIds));
		n.setGeneratedAt(now);
		n.setExpiresAt(expires);
		n.setDirty(false);
		n.setSourceVer("v1");
		snapshotRepo.save(n);

		// Redis
		redisCache.save(userId, freshIds);

		return freshIds;
	}

	@Transactional
	public void invalidateUpcoming() {
		User me = securityUtil.getAuthenticatedUser();
		Long userId = me.getId();
		redisCache.invalidate(userId);
		ChallengeRecoSnapshot snap = snapshotRepo.findById(userId).orElse(null);
		if (snap != null) { snap.setDirty(true); snapshotRepo.save(snap); }
		// 슬롯은 그대로 두고(감사/분석용), 재조회 시 무시됨(만료 또는 dirty 로 새로운 셋 생성)
	}

	// ===== 유틸 =====
	private List<Long> readIds(String json) {
		try { return om.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<List<Long>>(){}); }
		catch (Exception e) { return List.of(); }
	}
	private String writeIds(List<Long> ids) {
		try { return om.writeValueAsString(ids); }
		catch (Exception e) { return "[]"; }
	}
}

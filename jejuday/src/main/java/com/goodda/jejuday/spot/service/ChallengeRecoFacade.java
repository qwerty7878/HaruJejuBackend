package com.goodda.jejuday.spot.service;

import com.goodda.jejuday.auth.entity.User;
import com.goodda.jejuday.auth.entity.UserTheme;
import com.goodda.jejuday.auth.util.SecurityUtil;
import com.goodda.jejuday.spot.dto.ChallengeResponse;
import com.goodda.jejuday.spot.entity.ChallengeParticipation;
import com.goodda.jejuday.spot.entity.ChallengeRecoItem;
import com.goodda.jejuday.spot.entity.Spot;
import com.goodda.jejuday.spot.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChallengeRecoFacade {

	private static final int SLOT_COUNT = 4;
	private static final int TTL_HOURS = 1; // 1시간으로 단축해서 테스트

	private final SecurityUtil securityUtil;
	private final ChallengeRepository challengeRepository;
	private final ChallengeRecoItemRepository itemRepo;
	private final ChallengeParticipationRepository participationRepo;
	private final SpotRepository spotRepository;

	/** 진행전 4개: 매번 새로운 결과 생성 */
	@Transactional
	public List<ChallengeResponse> getUpcomingWithAutoRefresh() {
		Long userId = securityUtil.getAuthenticatedUser().getId();
		LocalDateTime now = LocalDateTime.now();

		log.info("Getting upcoming challenges for user: {}", userId);

		// 현재 유효한 아이템들 조회
		List<ChallengeRecoItem> activeItems = itemRepo.findActiveByUser(userId, now);
		log.info("Found {} active items for user {}", activeItems.size(), userId);

		// 만료되었거나 부족하면 새로 생성
		if (activeItems.size() < SLOT_COUNT || isExpiredSoon(activeItems)) {
			log.info("Refreshing recommendations for user {}", userId);
			purgeAndRefresh(userId);
			activeItems = itemRepo.findActiveByUser(userId, now);
		}

		// Spot 조회 및 Response 변환
		return convertToResponses(activeItems);
	}

	/** 강제 새로고침 */
	@Transactional
	public List<ChallengeResponse> forceRefreshAndGet() {
		Long userId = securityUtil.getAuthenticatedUser().getId();
		log.info("Force refreshing recommendations for user {}", userId);

		purgeAndRefresh(userId);

		List<ChallengeRecoItem> activeItems = itemRepo.findActiveByUser(userId, LocalDateTime.now());
		return convertToResponses(activeItems);
	}

	private boolean isExpiredSoon(List<ChallengeRecoItem> items) {
		LocalDateTime soon = LocalDateTime.now().plus(30, ChronoUnit.MINUTES);
		return items.stream().anyMatch(item -> item.getExpiresAt().isBefore(soon));
	}

	private void purgeAndRefresh(Long userId) {
		// 기존 아이템 모두 삭제
		itemRepo.deleteByUserId(userId);

		// 새로운 추천 생성
		generateNewRecommendations(userId);
	}

	private void generateNewRecommendations(Long userId) {
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime expiresAt = now.plus(TTL_HOURS, ChronoUnit.HOURS);

		// 사용된 Spot ID 추적
		Set<Long> usedSpotIds = new HashSet<>();

		// 선호 테마 조회 (진행중인 테마 제외)
		List<Long> preferredThemes = getPreferredThemes(userId);
		log.info("User {} preferred themes: {}", userId, preferredThemes);

		// 4개 슬롯 채우기
		for (int slot = 0; slot < SLOT_COUNT; slot++) {
			Spot selectedSpot = selectSpotForSlot(slot, preferredThemes, usedSpotIds);

			if (selectedSpot != null) {
				saveRecommendationItem(userId, slot, selectedSpot, now, expiresAt);
				usedSpotIds.add(selectedSpot.getId());
				log.info("Added spot {} to slot {} for user {}", selectedSpot.getId(), slot, userId);
			} else {
				log.warn("Failed to find spot for slot {} user {}", slot, userId);
			}
		}
	}

	private List<Long> getPreferredThemes(Long userId) {
		User user = securityUtil.getAuthenticatedUser();

		// 진행중인 테마 조회
		List<ChallengeParticipation.Status> ongoingStatuses = Arrays.asList(
				ChallengeParticipation.Status.JOINED,
				ChallengeParticipation.Status.SUBMITTED,
				ChallengeParticipation.Status.APPROVED
		);

		List<Long> ongoingThemeIds = participationRepo.findOngoingThemeIds(userId, ongoingStatuses);

		// 선호 테마에서 진행중인 테마 제외
		return user.getUserThemes().stream()
				.filter(Objects::nonNull)
				.map(UserTheme::getId)
				.filter(Objects::nonNull)
				.filter(themeId -> !ongoingThemeIds.contains(themeId))
				.limit(3)
				.collect(Collectors.toList());
	}

	private Spot selectSpotForSlot(int slot, List<Long> preferredThemes, Set<Long> usedSpotIds) {
		// 슬롯 0-2: 선호 테마 우선
		if (slot < 3 && slot < preferredThemes.size()) {
			Long themeId = preferredThemes.get(slot);
			Spot spot = findRandomSpotByTheme(themeId, usedSpotIds);
			if (spot != null) return spot;
		}

		// 선호 테마에서 못찾거나 슬롯3이면 전체에서 랜덤
		return findRandomSpotExcludingThemes(preferredThemes, usedSpotIds);
	}

	private Spot findRandomSpotByTheme(Long themeId, Set<Long> excludeIds) {
		// 해당 테마의 챌린지들 조회
		List<Spot> candidates = challengeRepository.findByThemeAndType(themeId, Spot.SpotType.CHALLENGE, PageRequest.of(0, 50));

		// 필터링: 삭제되지 않고 제외 목록에 없는 것
		List<Spot> filtered = candidates.stream()
				.filter(s -> !Boolean.TRUE.equals(s.getIsDeleted()))
				.filter(s -> !excludeIds.contains(s.getId()))
				.collect(Collectors.toList());

		if (filtered.isEmpty()) return null;

		// 랜덤 선택
		return filtered.get(ThreadLocalRandom.current().nextInt(filtered.size()));
	}

	private Spot findRandomSpotExcludingThemes(List<Long> excludeThemes, Set<Long> excludeIds) {
		// 전체 챌린지에서 조회
		List<Spot> candidates = challengeRepository.findByType(Spot.SpotType.CHALLENGE, PageRequest.of(0, 100));

		// 필터링
		List<Spot> filtered = candidates.stream()
				.filter(s -> !Boolean.TRUE.equals(s.getIsDeleted()))
				.filter(s -> !excludeIds.contains(s.getId()))
				.filter(s -> {
					Long themeId = s.getTheme() != null ? s.getTheme().getId() : null;
					return themeId == null || !excludeThemes.contains(themeId);
				})
				.collect(Collectors.toList());

		if (filtered.isEmpty()) {
			// 제외 조건 무시하고 아무거나
			return candidates.stream()
					.filter(s -> !Boolean.TRUE.equals(s.getIsDeleted()))
					.filter(s -> !excludeIds.contains(s.getId()))
					.findFirst()
					.orElse(null);
		}

		return filtered.get(ThreadLocalRandom.current().nextInt(filtered.size()));
	}

	private void saveRecommendationItem(Long userId, int slot, Spot spot, LocalDateTime now, LocalDateTime expiresAt) {
		ChallengeRecoItem item = new ChallengeRecoItem();
		item.setUserId(userId);
		item.setSpotId(spot.getId());
		item.setThemeId(spot.getTheme() != null ? spot.getTheme().getId() : null);
		item.setSlotIndex(slot);
		item.setGeneratedAt(now);
		item.setExpiresAt(expiresAt);
		item.setReason("GENERATED");
		itemRepo.save(item);
	}

	private List<ChallengeResponse> convertToResponses(List<ChallengeRecoItem> items) {
		if (items == null || items.isEmpty()) {
			return List.of();
		}

		// 슬롯 순서로 정렬
		items.sort(Comparator.comparingInt(ChallengeRecoItem::getSlotIndex));

		// Spot ID 수집
		List<Long> spotIds = items.stream()
				.map(ChallengeRecoItem::getSpotId)
				.filter(Objects::nonNull)
				.collect(Collectors.toList());

		if (spotIds.isEmpty()) {
			return List.of();
		}

		// Spot들을 JOIN FETCH로 조회
		List<Spot> spots = challengeRepository.findByIdInWithTheme(spotIds);
		Map<Long, Spot> spotMap = spots.stream()
				.filter(s -> s.getType() == Spot.SpotType.CHALLENGE && !Boolean.TRUE.equals(s.getIsDeleted()))
				.collect(Collectors.toMap(Spot::getId, s -> s));

		// 순서 유지하며 Response 생성
		List<ChallengeResponse> responses = new ArrayList<>();
		for (ChallengeRecoItem item : items) {
			Spot spot = spotMap.get(item.getSpotId());
			if (spot != null) {
				responses.add(ChallengeResponse.of(spot));
			}
		}

		log.info("Converted {} items to {} responses", items.size(), responses.size());
		return responses;
	}
}
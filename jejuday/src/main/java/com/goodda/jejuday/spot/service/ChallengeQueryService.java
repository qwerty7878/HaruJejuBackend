package com.goodda.jejuday.spot.service;

import com.goodda.jejuday.auth.entity.User;
import com.goodda.jejuday.auth.entity.UserTheme;
import com.goodda.jejuday.auth.util.SecurityUtil;
import com.goodda.jejuday.spot.dto.ChallengeResponse;
import com.goodda.jejuday.spot.dto.MyChallengeResponse;
import com.goodda.jejuday.spot.entity.ChallengeParticipation;
import com.goodda.jejuday.spot.entity.ChallengeParticipation.Status;
import com.goodda.jejuday.spot.entity.Spot;
import com.goodda.jejuday.spot.repository.ChallengeParticipationRepository;
import com.goodda.jejuday.spot.repository.ChallengeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChallengeQueryService {

	private final ChallengeRepository challengeRepository;
	private final ChallengeParticipationRepository cpRepository;
	private final ChallengeRecCacheService cacheService;
	private final SecurityUtil securityUtil;

	/** 기본: 캐시된 추천 4개 (없거나 만료 시 생성→캐시) */
	public List<ChallengeResponse> upcomingCached() {
		User me = securityUtil.getAuthenticatedUser();

		List<Long> cachedIds = cacheService.load(me.getId());
		if (cachedIds != null && !cachedIds.isEmpty()) {
			return mapIdsToResponses(cachedIds);
		}

		List<ChallengeResponse> fresh = generateUpcomingPersonalized4(me);
		// 캐시에 ID만 저장
		List<Long> ids = new ArrayList<>();
		for (ChallengeResponse r : fresh) ids.add(r.getId());
		cacheService.save(me.getId(), ids);

		return fresh;
	}

	/** 강제 새로고침: 광고/AOP 통과 후 매번 새로 생성+캐시 */
	@Transactional // 캐시 쓰기 포함
	public List<ChallengeResponse> upcomingForceRefresh() {
		User me = securityUtil.getAuthenticatedUser();

		List<ChallengeResponse> fresh = generateUpcomingPersonalized4(me);
		List<Long> ids = new ArrayList<>();
		for (ChallengeResponse r : fresh) ids.add(r.getId());
		cacheService.save(me.getId(), ids);

		return fresh;
	}

	// ==== 기존 진행중/완료는 그대로 ====

	public List<MyChallengeResponse> ongoingMine() {
		User me = securityUtil.getAuthenticatedUser();
		List<Status> ongoingStatuses = Arrays.asList(Status.JOINED, Status.SUBMITTED, Status.APPROVED);
		List<ChallengeParticipation> cps =
				cpRepository.findOngoing(me.getId(), ongoingStatuses, PageRequest.of(0, 4));

		List<MyChallengeResponse> list = new ArrayList<>();
		for (ChallengeParticipation cp : cps) {
			Spot s = cp.getChallenge();
			list.add(MyChallengeResponse.of(s, cp));
		}
		return list;
	}

	public List<MyChallengeResponse> completedMine(String sort, Long lastId, Integer size) {
		User me = securityUtil.getAuthenticatedUser();
		int limit = (size == null || size <= 0) ? 20 : Math.min(size, 50);

		List<ChallengeParticipation> cps;
		if ("views".equalsIgnoreCase(sort)) {
			cps = cpRepository.findCompletedByViews(me.getId(), lastId, PageRequest.of(0, limit));
		} else if ("likes".equalsIgnoreCase(sort)) {
			cps = cpRepository.findCompletedByLikes(me.getId(), lastId, PageRequest.of(0, limit));
		} else {
			cps = cpRepository.findCompletedLatest(me.getId(), lastId, PageRequest.of(0, limit));
		}

		List<MyChallengeResponse> list = new ArrayList<>();
		for (ChallengeParticipation cp : cps) {
			Spot s = cp.getChallenge();
			list.add(MyChallengeResponse.of(s, cp));
		}
		return list;
	}

	// ==== 내부 유틸 ====

	private List<ChallengeResponse> mapIdsToResponses(List<Long> spotIds) {
		// 주의: 정렬 유지 필요 → findAllById 는 순서를 보장하지 않음
		// 간단히 spotId 각각 getOne; 또는 custom query 추천
		List<ChallengeResponse> list = new ArrayList<>();
		for (Long id : spotIds) {
			Optional<Spot> opt = challengeRepository.findById(id);
			if (opt.isPresent()) {
				list.add(ChallengeResponse.of(opt.get()));
			}
		}
		return list;
	}

	/** 실제 추천 생성 — 현재는 랜덤 placeholder */
	private List<ChallengeResponse> generateUpcomingPersonalized4(User me) {
		// 1) 동일 테마 동시 진행 금지: 진행중 테마 제외
		List<Status> ongoingStatuses = Arrays.asList(Status.JOINED, Status.SUBMITTED, Status.APPROVED);
		List<Long> ongoingThemeIds = cpRepository.findOngoingThemeIds(me.getId(), ongoingStatuses);

		// 2) 유저 선호 테마(0~3) — Set이므로 정렬 후 최대 3개 사용
		List<UserTheme> prefThemes = me.getUserThemes().stream()
				.filter(theme -> theme != null && theme.getId() != null)
				.filter(theme -> !ongoingThemeIds.contains(theme.getId()))
				.sorted(Comparator.comparing(UserTheme::getId))
				.limit(3)
				.toList();

		Set<Long> pickedIds = new HashSet<>();
		List<Spot> picks = new ArrayList<>();

		// [placeholder] 테마별 추천: 현재는 "랜덤" 후보에서 1개만 선택
		// TODO: 여기서 가중치 스코어(테마/태그/거리/신규/인기도)를 적용해 선별하도록 교체
		for (UserTheme theme : prefThemes) {
			Long themeId = theme.getId();
			int limit = 12;
			List<Spot> candidates = challengeRepository.findRandomByTheme(themeId, limit);
			for (Spot s : candidates) {
				if (pickedIds.add(s.getId())) {
					picks.add(s);
					break;
				}
			}
		}

		// 부족분은 전체 랜덤으로 채우기 (총 4개)
		int need = 4 - picks.size();
		if (need > 0) {
			List<Spot> randoms = challengeRepository.findRandom(need * 10);
			for (Spot s : randoms) {
				if (pickedIds.add(s.getId())) {
					picks.add(s);
					if (picks.size() == 4) break;
				}
			}
		}

		List<ChallengeResponse> result = new ArrayList<>();
		for (Spot s : picks) {
			result.add(ChallengeResponse.of(s));
		}
		return result;
	}

	/** Facade가 호출하는 추천 생성기 (현재 랜덤 placeholder).
	 *  - TODO 위치: 테마/태그/거리/등업/인기도 가산점 로직 교체
	 */
	public List<ChallengeResponse> generateUpcomingPersonalized4Internal(User me) {
		// ... (여기 기존 랜덤 placeholder 유지)
		// 1) 진행중 테마 제외
		// 2) me.getUserThemes() 0~3개 사용
		// 3) theme별 findRandomByTheme → 1개씩
		// 4) 부족분 전체 findRandom 보충 → 4개
		// 5) ChallengeResponse 매핑
		return generateUpcomingPersonalized4(me);
	}

	// TODO: 참여 생성/완료 시점에 캐시 무효화
	// ex) public void onJoinOrComplete(Long userId) { cacheService.invalidate(userId); }

	public ChallengeResponse mapSpotIdToResponse(Long spotId) {
		if (spotId == null) return null;
		Optional<Spot> opt = challengeRepository.findById(spotId);
		if (opt.isEmpty()) return null;

		Spot s = opt.get();
		// 유효성: 챌린지 타입 & 삭제 아님
		if (s.getType() != Spot.SpotType.CHALLENGE) return null;
		if (Boolean.TRUE.equals(s.getIsDeleted())) return null;

		// theme 는 LAZY지만, 본 메서드는 @Transactional(readOnly = true) 컨텍스트 안에서 호출되므로 안전
		return ChallengeResponse.of(s);
	}

	// Spot 조회 유틸 (null 허용)
	public Spot findSpot(Long spotId) {
		if (spotId == null) return null;
		return challengeRepository.findById(spotId).orElse(null);
	}

	// 유저가 가진 선호 테마인지 판별
	public boolean isUserPrefTheme(User me, Long themeId) {
		if (themeId == null) return false;
		return me.getUserThemes().stream().anyMatch(t -> t != null && themeId.equals(t.getId()));
	}


}

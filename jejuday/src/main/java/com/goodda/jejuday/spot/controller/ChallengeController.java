package com.goodda.jejuday.spot.controller;// com.goodda.jejuday.spot.controller.ChallengeController.java (기존 목록/진행/완료에 추가)
import com.goodda.jejuday.spot.dto.*;
import com.goodda.jejuday.spot.service.ChallengeActionService;
import com.goodda.jejuday.spot.service.ChallengeQueryService;
import com.goodda.jejuday.spot.service.ChallengeRecoFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/challenges")
public class ChallengeController {

    private final ChallengeRecoFacade recoFacade;
    private final ChallengeQueryService queryService;
    private final ChallengeActionService actionService;

    // 진행전: 2일 캐시/스냅샷 기반
    @GetMapping("/upcoming")
    public ResponseEntity<List<ChallengeResponse>> upcomingCached() {
        List<Long> ids = recoFacade.getUpcomingSpotIds();
        // id → Spot → ChallengeResponse 매핑
        List<ChallengeResponse> responses = ids.stream()
                .map(id -> queryService.mapSpotIdToResponse(id)) // ⬅️ 유틸 하나 만들어도 되고,
                .filter(Objects::nonNull)
                .toList();
        return ResponseEntity.ok(responses);
    }

    // 강제 새로고침(광고 게이트 AOP 는 원하는 시점에 주석 해제)
    @PostMapping("/upcoming/refresh")
    // @AdGatedRefresh // TODO: 구독/광고 검증
    public ResponseEntity<List<ChallengeResponse>> upcomingRefresh() {
        recoFacade.invalidateUpcoming(); // dirty + redis invalidation
        List<Long> ids = recoFacade.getUpcomingSpotIds();  // 재계산
        List<ChallengeResponse> responses = ids.stream()
                .map(id -> queryService.mapSpotIdToResponse(id))
                .filter(Objects::nonNull)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/ongoing")
    public ResponseEntity<List<MyChallengeResponse>> ongoing() {
        return ResponseEntity.ok(queryService.ongoingMine());
    }

    @GetMapping("/completed")
    public ResponseEntity<List<MyChallengeResponse>> completed(
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(required = false) Long lastId,
            @RequestParam(defaultValue = "20") Integer size
    ) {
        return ResponseEntity.ok(queryService.completedMine(sort, lastId, size));
    }




    /** 진행 시작 */
    @PostMapping("/{id}/start")
    public ResponseEntity<ChallengeStartResponse> start(
            @PathVariable Long id,
            @RequestBody ChallengeStartRequest req
    ) {
        ChallengeStartResponse res = actionService.start(id, req);
        return ResponseEntity.ok(res);
    }

    /** 진행 완료 (근접성 검사 + 포인트 지급) */
    @PostMapping("/{id}/complete")
    public ResponseEntity<ChallengeCompleteResponse> complete(
            @PathVariable Long id,
            @RequestBody ChallengeCompleteRequest req
    ) {
        ChallengeCompleteResponse res = actionService.complete(id, req);
        return ResponseEntity.ok(res);
    }
}

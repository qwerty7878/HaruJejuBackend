package com.goodda.jejuday.spot.dto;

import com.goodda.jejuday.spot.entity.ChallengeParticipation;
import com.goodda.jejuday.spot.entity.Spot;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MyChallengeResponse {
    private Long id;
    private String name;
    private Integer point;
    private Integer viewCount;
    private Integer likeCount;
    private String themeName;

    // 참여 정보
    private LocalDate startDate;
    private LocalDate endDate;
    private String myStatus;

    public static MyChallengeResponse of(Spot spot, ChallengeParticipation cp) {
        String themeNameValue = (spot.getTheme() != null) ? spot.getTheme().getName() : null;
        return MyChallengeResponse.builder()
                .id(spot.getId())
                .name(spot.getName())
                .point(spot.getPoint())
                .viewCount(spot.getViewCount())
                .likeCount(spot.getLikeCount())
                .themeName(themeNameValue)
                .startDate(cp.getStartDate())
                .endDate(cp.getEndDate())
                .myStatus(cp.getStatus().name())
                .build();
    }
}
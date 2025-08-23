package com.goodda.jejuday.spot.dto;

import com.goodda.jejuday.spot.entity.Spot;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChallengeResponse {
    private Long id;
    private String name;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String themeName; // null 가능

    public static ChallengeResponse of(Spot spot) {
        String themeNameValue = (spot.getTheme() != null) ? spot.getTheme().getName() : null;
        return ChallengeResponse.builder()
                .id(spot.getId())
                .name(spot.getName())
                .latitude(spot.getLatitude())
                .longitude(spot.getLongitude())
                .themeName(themeNameValue)
                .build();
    }
}

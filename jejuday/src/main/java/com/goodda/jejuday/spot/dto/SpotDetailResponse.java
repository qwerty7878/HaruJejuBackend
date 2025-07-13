package com.goodda.jejuday.spot.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class SpotDetailResponse extends SpotResponse {
    private String description;
    private List<String> imageUrls;
    private int commentCount;
    private boolean bookmarkedByMe;

    public SpotDetailResponse(Long id, String name, BigDecimal latitude, BigDecimal longitude,
                              int likeCount, boolean likedByMe, String description,
                              List<String> imageUrls, int commentCount, boolean bookmarkedByMe) {
        super(id, name, latitude, longitude, likeCount, likedByMe);
        this.description = description;
        this.imageUrls = imageUrls;
        this.commentCount = commentCount;
        this.bookmarkedByMe = bookmarkedByMe;
    }
}
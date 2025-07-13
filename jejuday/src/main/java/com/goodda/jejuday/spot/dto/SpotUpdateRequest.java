package com.goodda.jejuday.spot.dto;

import lombok.Data;

@Data
public class SpotUpdateRequest {
    private String name;
    private String description;
    private Double latitude;
    private Double longitude;
}
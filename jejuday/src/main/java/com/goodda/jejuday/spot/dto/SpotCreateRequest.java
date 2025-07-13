package com.goodda.jejuday.spot.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SpotCreateRequest {
    private String name;
    private String description;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String type; // SPOT or CHALLENGE
}
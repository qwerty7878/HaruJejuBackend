package com.goodda.jejuday.crawler.entitiy;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@Table(name = "jeju_events")
public class JejuEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String contentsId;

    private String title;
    private String subTitle;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private String location;
    private Integer likesCount;
    private Integer reviewsCount;
    private String imageUrl;
    private String detailUrl;

    // getters & setters
}
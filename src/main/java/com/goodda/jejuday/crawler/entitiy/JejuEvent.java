package com.goodda.jejuday.crawler.entitiy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter @Setter
@Table(
        name = "jeju_events",
        uniqueConstraints = @UniqueConstraint(name = "uk_jeju_events_contents_id", columnNames = "contents_id"),
        indexes = @Index(name = "idx_jeju_events_contents_id", columnList = "contents_id")
)
public class JejuEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="contents_id", unique = true, nullable = false, length = 64)
    private String contentsId;

    @Column(length = 300)
    private String title;

    @Column(length = 500)
    private String subTitle;

    @Column(length = 200)
    private String periodText;

    private LocalDate periodStart;
    private LocalDate periodEnd;

    @Column(length = 200)
    private String location;

    private Integer likesCount;
    private Integer reviewsCount;

    @Column(length = 500)
    private String imageUrl;

    @Column(length = 600)
    private String detailUrl;
}

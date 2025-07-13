package com.goodda.jejuday.spot.entitiy;

import com.goodda.jejuday.Auth.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "Bookmark")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Bookmark {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "spot_id")
    private Spot spot;

    private LocalDateTime createdAt = LocalDateTime.now();

    public Bookmark(Long userId, Long spotId) {
        this.user = new User(userId);
        this.spot = new Spot(spotId);
    }
}
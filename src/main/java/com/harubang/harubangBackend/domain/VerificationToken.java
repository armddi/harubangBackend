package com.harubang.harubangBackend.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter @Setter
@Builder
@NoArgsConstructor @AllArgsConstructor
@Table(name = "verification_token", indexes = {
        @Index(name = "idx_verification_token_token", columnList = "token", unique = true)
})
public class VerificationToken {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, unique=true, length=100)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // 가입 후 인증 방식에서 사용 (옵션 B에서는 null일 수 있음)

    @Column(length = 120)          // ✅ 추가: 이메일만으로 요청한 경우 저장
    private String email;

    @Column(nullable=false)
    private boolean used;

    @Column(nullable=false)
    private LocalDateTime expiresAt;
}

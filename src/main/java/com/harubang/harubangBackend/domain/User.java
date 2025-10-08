package com.harubang.harubangBackend.domain;

import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name="users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, unique=true, length=100)
    private String email;

    @Column(nullable=false, length=60)
    private String password; // BCrypt 해시

    @Column(nullable=false, length=50)
    private String name;

    @Column(nullable=false, length=20)
    private String role; // USER / AGENT / ADMIN

    @Builder.Default
    @Column(nullable = false)
    private boolean emailVerified = false;

}

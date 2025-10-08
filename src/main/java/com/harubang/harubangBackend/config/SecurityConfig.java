package com.harubang.harubangBackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // API 서버라 CSRF 비활성화
                .csrf(csrf -> csrf.disable())
                // 인증/인가 규칙
                .authorizeHttpRequests(auth -> auth
                        // ✅ 인증 관련 엔드포인트는 모두 허용
                        .requestMatchers("/api/auth/**", "/error").permitAll()
                        // 그 외에는 인증 필요
                        .anyRequest().authenticated()
                )
                // 폼/베이직 로그인 비활성화 (JWT 등 사용 가정)
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable());

        return http.build();
    }

    // ✅ 누락되어 있던 PasswordEncoder 등록 (부팅 실패 원인)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

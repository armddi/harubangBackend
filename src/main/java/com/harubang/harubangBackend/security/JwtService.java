package com.harubang.harubangBackend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm; // 0.12.x에서는 deprecate 되었지만 사용 가능
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Service
public class JwtService {

    // 토큰 유효기간 (1시간)
    private static final long EXP_MS = 1000L * 60 * 60;

    // 서명 키 (운영에선 반드시 환경변수로!)
    private final Key key;

    public JwtService() {
        String secret = System.getenv().getOrDefault(
                "JWT_SECRET",
                "dev-only-please-change-me-32bytes-minimum-key!!" // 최소 32바이트
        );
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** JWT 발급 */
    public String generate(String subject, String role) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(subject)
                .claim("role", role)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + EXP_MS))
                .signWith(key, SignatureAlgorithm.HS256) // 0.12.x에선 Jwts.SIG.HS256 사용도 가능
                .compact();
    }

    /** JWT 파싱/검증 */
    public Jws<Claims> parse(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
    }
}

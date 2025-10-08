package com.harubang.harubangBackend.service;

import com.harubang.harubangBackend.domain.User;
import com.harubang.harubangBackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder encoder;

    /** 회원가입: 저장된 User를 반환 */
    @Transactional
    public User signup(String email, String name, String rawPassword) {
        if (users.existsByEmail(email)) {
            throw new IllegalArgumentException("EMAIL_IN_USE");
        }

        User u = User.builder()
                .email(email)
                .name(name)
                .password(encoder.encode(rawPassword))
                .role("USER")
                .build();

        // User 엔티티에 emailVerified 같은 필드가 있으면 기본값 설정
        try {
            // 없으면 컴파일 에러 나니까, 있으면 사용하세요.
            User.class.getDeclaredMethod("setEmailVerified", boolean.class);
            u.setEmailVerified(false);
        } catch (NoSuchMethodException ignored) { }

        return users.save(u); // <= 저장 후 User 반환
    }

    /** 로그인: 인증 성공 시 User 반환 */
    @Transactional(readOnly = true)
    public User authenticate(String email, String rawPassword) {
        User u = users.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("NO_USER"));

        if (!encoder.matches(rawPassword, u.getPassword())) {
            throw new IllegalArgumentException("BAD_CREDENTIALS");
        }
        return u;
    }
}

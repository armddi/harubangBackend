package com.harubang.harubangBackend.service;

import com.harubang.harubangBackend.domain.User;
import com.harubang.harubangBackend.domain.VerificationToken;
import com.harubang.harubangBackend.repository.UserRepository;
import com.harubang.harubangBackend.repository.VerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private final JavaMailSender mailSender;
    private final VerificationTokenRepository tokens;
    private final UserRepository users;

    @Value("${app.url:http://localhost:8080}")
    private String appUrl; // 인증 링크 기본 URL

    // 토큰 유효 시간(분 단위)
    private static final int TOKEN_TTL_MINUTES = 30;
    private static final int RECENT_VERIFY_TTL_MINUTES = 30;

    // 최근 인증 이메일 캐시 (간단 구현)
    private final Map<String, LocalDateTime> recentlyVerified = new ConcurrentHashMap<>();

    /* ─────────────────────────────────────────────────────────────
       1) 회원가입 이후 인증 메일 발송
       ───────────────────────────────────────────────────────────── */
    @Override
    @Transactional
    public void issueAndSend(User user) {
        String tokenValue = UUID.randomUUID().toString();

        VerificationToken vt = VerificationToken.builder()
                .token(tokenValue)
                .user(user)
                .email(user.getEmail())
                .expiresAt(LocalDateTime.now().plusMinutes(TOKEN_TTL_MINUTES))
                .used(false)
                .build();
        tokens.save(vt);

        String link = appUrl + "/api/auth/verify?token=" + tokenValue;

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(user.getEmail());
        msg.setFrom("1daybang0530@gmail.com"); // ✅ Gmail 발신자
        msg.setSubject("[하루방] 이메일 인증을 완료해 주세요");
        msg.setText("""
                안녕하세요. 하루방입니다.

                아래 링크를 클릭하여 이메일 인증을 완료해 주세요.
                %s

                (30분 후 만료)
                """.formatted(link));

        mailSender.send(msg);
        log.info("[EmailVerification] sent to={}, link={}", user.getEmail(), link);
    }

    /* ─────────────────────────────────────────────────────────────
       2) 이메일만으로 인증 메일 발송 (회원가입 전)
       ───────────────────────────────────────────────────────────── */
    @Override
    @Transactional
    public void issueAndSendForEmail(String email) {
        String tokenValue = UUID.randomUUID().toString();

        VerificationToken vt = VerificationToken.builder()
                .token(tokenValue)
                .user(null)
                .email(email)
                .expiresAt(LocalDateTime.now().plusMinutes(TOKEN_TTL_MINUTES))
                .used(false)
                .build();
        tokens.save(vt);

        String link = appUrl + "/api/auth/verify?token=" + tokenValue;

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(email);
        msg.setFrom("1daybang0530@gmail.com"); // ✅ Gmail 발신자
        msg.setSubject("[하루방] 이메일 인증을 완료해 주세요");
        msg.setText("""
                안녕하세요. 하루방입니다.

                아래 링크를 클릭하여 이메일 인증을 완료해 주세요.
                %s

                (30분 후 만료)
                """.formatted(link));

        mailSender.send(msg);
        log.info("[EmailVerification] sent (pre-signup) to={}, link={}", email, link);
    }

    /* ─────────────────────────────────────────────────────────────
       3) 토큰 검증 후 이메일 반환 및 인증 처리
       ───────────────────────────────────────────────────────────── */
    @Override
    @Transactional
    public String verifyAndReturnEmail(String tokenValue) {
        VerificationToken vt = tokens.findByToken(tokenValue)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_TOKEN"));

        if (vt.isUsed() || vt.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "TOKEN_EXPIRED_OR_USED");
        }

        vt.setUsed(true);
        tokens.save(vt);

        String email = (vt.getUser() != null) ? vt.getUser().getEmail() : vt.getEmail();
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "EMAIL_NOT_FOUND_IN_TOKEN");
        }

        recentlyVerified.put(email, LocalDateTime.now());
        log.info("[EmailVerification] verified (pre-signup) email={}", email);
        return email;
    }

    /* ─────────────────────────────────────────────────────────────
       4) 기존 토큰 기반 인증 (회원가입 이후)
       ───────────────────────────────────────────────────────────── */
    @Override
    @Transactional
    public void verify(String token) {
        VerificationToken vt = tokens.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_TOKEN"));

        if (vt.isUsed() || vt.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "TOKEN_EXPIRED_OR_USED");
        }

        User user = vt.getUser();
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "NO_USER_BOUND");
        }

        user.setEmailVerified(true);
        users.save(user);

        vt.setUsed(true);
        tokens.save(vt);

        log.info("[EmailVerification] verified user={}", user.getEmail());
    }

    /* ─────────────────────────────────────────────────────────────
       5) 최근 인증 확인
       ───────────────────────────────────────────────────────────── */
    @Override
    public boolean isEmailVerifiedRecently(String email) {
        LocalDateTime at = recentlyVerified.get(email);
        return at != null && at.isAfter(LocalDateTime.now().minusMinutes(RECENT_VERIFY_TTL_MINUTES));
    }
}

package com.harubang.harubangBackend.web;

import com.harubang.harubangBackend.repository.UserRepository;
import com.harubang.harubangBackend.security.JwtService;
import com.harubang.harubangBackend.service.AuthService;
import com.harubang.harubangBackend.service.EmailVerificationService;
import com.harubang.harubangBackend.web.dto.AuthDtos.*;
import com.harubang.harubangBackend.web.dto.ResendVerificationRequest; // email 1개 들어있는 DTO 재사용
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService auth;
    private final JwtService jwt;
    private final EmailVerificationService verification;
    private final UserRepository users;

    // 인증 성공 후 프론트로 보낼 리다이렉트 주소
    @Value("${app.frontend:http://localhost:5173}")
    private String frontendBase;

    /* ─────────────────────────────────────────────
       ✅ 옵션B 핵심 1) 이메일 인증 “요청”(사전등록)
       ───────────────────────────────────────────── */
    @PostMapping("/verification/request")
    public ResponseEntity<Void> request(@Valid @RequestBody ResendVerificationRequest dto) {
        // (원하면 여기서 이미 인증된 계정/가입된 계정 여부를 체크해 400을 내려도 됨)
        verification.issueAndSendForEmail(dto.email());
        return ResponseEntity.ok().build();
    }

    /* ─────────────────────────────────────────────
       ✅ 옵션B 핵심 2) 메일 링크 클릭 → 검증 후 프론트 리다이렉트
       ───────────────────────────────────────────── */
    @GetMapping("/verify")
    public ResponseEntity<Void> verify(@RequestParam String token) {
        String email = verification.verifyAndReturnEmail(token);
        String url = frontendBase + "/apply?verified=1&email=" +
                URLEncoder.encode(email, StandardCharsets.UTF_8);
        return ResponseEntity.status(302).header("Location", url).build();
    }

    /* ─────────────────────────────────────────────
       ✅ 옵션B 핵심 3) 가입: 최근 인증된 이메일만 허용
       ───────────────────────────────────────────── */
    @PostMapping("/signup")
    public ResponseEntity<Void> signup(@Valid @RequestBody SignupRequest dto) {
        if (!verification.isEmailVerifiedRecently(dto.email())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "EMAIL_NOT_VERIFIED");
        }
        // 최근 인증된 이메일이므로, 저장 시 emailVerified=true 로 저장 권장 (AuthService에서 처리)
        auth.signup(dto.email(), dto.name(), dto.password());
        return ResponseEntity.ok().build();
    }

    /* ─────────────────────────────────────────────
       (선택) 로그인은 그대로 사용 — 이미 인증된 계정만 허용
       ───────────────────────────────────────────── */
    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest dto) {
        var u = auth.authenticate(dto.email(), dto.password());
        if (!u.isEmailVerified()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "EMAIL_NOT_VERIFIED");
        }
        String token = jwt.generate(u.getEmail(), u.getRole());
        return new TokenResponse(token);
    }

    /* ─────────────────────────────────────────────
       (선택) 예전 플로우 호환: 기존 “인증메일 재전송”
       ───────────────────────────────────────────── */
    @PostMapping("/verification/resend")
    public ResponseEntity<Void> resend(@Valid @RequestBody ResendVerificationRequest dto) {
        var user = users.findByEmail(dto.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "NO_USER"));

        if (user.isEmailVerified()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ALREADY_VERIFIED");
        }

        verification.issueAndSend(user);
        return ResponseEntity.ok().build();
    }
}

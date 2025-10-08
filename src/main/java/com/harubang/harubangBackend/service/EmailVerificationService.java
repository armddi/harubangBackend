package com.harubang.harubangBackend.service;

import com.harubang.harubangBackend.domain.User; // 프로젝트의 User 경로에 맞게 수정

public interface EmailVerificationService {
    void issueAndSend(User user);  // 인증 토큰 생성 + 메일 발송
    void verify(String token);     // 토큰 검증 + 사용자 인증 처리
    /** 이메일만으로 토큰 발행 + 메일 전송 (사전등록) */
    void issueAndSendForEmail(String email);

    /** 토큰 검증 후 이메일 반환(used=true로 표시) */
    String verifyAndReturnEmail(String tokenValue);

    /** 최근(예: 30분) 내 인증되었는지 확인 */
    boolean isEmailVerifiedRecently(String email);
}

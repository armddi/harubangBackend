package com.harubang.harubangBackend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MailService {
    private final JavaMailSender mailSender;

    public void sendVerificationMail(String to, String verifyUrl) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        msg.setSubject("[하루방] 이메일 인증을 완료해 주세요");
        msg.setText("아래 링크를 클릭하면 인증이 완료됩니다:\n" + verifyUrl);
        mailSender.send(msg);
    }
}

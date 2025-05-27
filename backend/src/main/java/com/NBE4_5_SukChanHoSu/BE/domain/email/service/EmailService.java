package com.NBE4_5_SukChanHoSu.BE.domain.email.service;

import com.NBE4_5_SukChanHoSu.BE.domain.user.repository.UserRepository;
import com.NBE4_5_SukChanHoSu.BE.global.util.EmailUtil;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender javaMailSender;
    private final RedisTemplate<String, String> redisTemplate;
    private final EmailUtil emailUtil;
    private final UserRepository userRepository;

    private static final String EMAIL_AUTH = "emailAuth:";
    private static final String EMAIL_VERIFY = "emailVerify:";
    private static final String TRUE = "true";

    @Value("${spring.mail.username}")
    private String senderEmail;

    @Value("${spring.mail.properties.auth-code-expiration-millis}")
    private Long authCodeExpirationMillis;

    public String createCode() {
        return EmailUtil.createAuthCode();
    }

    public String sendSimpleMessage(String sendEmail) throws MessagingException {
        String authCode = createCode();
        String key = EMAIL_AUTH + sendEmail;
        redisTemplate.opsForValue().set(key, authCode, authCodeExpirationMillis, TimeUnit.MILLISECONDS);

        MimeMessage message = emailUtil.createMailVerify(senderEmail, sendEmail, authCode);
        javaMailSender.send(message);

        return authCode;
    }

    public boolean verifyEmailCode(String email, String verifyCode) {
        String key = EMAIL_AUTH + email;
        String authCode = redisTemplate.opsForValue().get(key);
        if (authCode != null && authCode.equals(verifyCode)) {
            redisTemplate.opsForValue().set(EMAIL_VERIFY + email, TRUE, 5, TimeUnit.MINUTES);
            redisTemplate.delete(key);
            return true;
        }
        return false;
    }
}

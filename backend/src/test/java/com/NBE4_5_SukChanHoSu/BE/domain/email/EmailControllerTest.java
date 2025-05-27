package com.NBE4_5_SukChanHoSu.BE.domain.email;

import com.NBE4_5_SukChanHoSu.BE.domain.email.service.EmailService;
import com.NBE4_5_SukChanHoSu.BE.global.util.EmailUtil;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class EmailControllerTest {

    @InjectMocks
    private EmailService emailService;

    @Mock
    private JavaMailSender javaMailSender;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private EmailUtil emailUtil;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private MimeMessage mimeMessage;

    @Value("${spring.mail.username}")
    private String senderEmail = "test1@gmail.com";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "authCodeExpirationMillis", 300000L);
        ReflectionTestUtils.setField(emailService, "senderEmail", "test1@gmail.com");
    }

    @Test
    @DisplayName("인증 코드 생성")
    void createCodeTest() {
        String code = emailService.createCode();
        assertNotNull(code);
        assertEquals(6, code.length());
        assertTrue(code.matches("[A-Z0-9]+"));
    }

    @Test
    @DisplayName("이메일 인증 코드 전송 성공")
    void sendSimpleMessage_success() throws Exception {
        // given
        String email = "test@example.com";
        String authCode = "ABC123";
        String redisKey = "emailAuth:" + email;

        // when
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(emailUtil.createMailVerify(any(), any(), any())).thenReturn(mimeMessage);

        try (MockedStatic<EmailUtil> mockedUtil = Mockito.mockStatic(EmailUtil.class)) {
            mockedUtil.when(EmailUtil::createAuthCode).thenReturn(authCode);

            // 실행
            String result = emailService.sendSimpleMessage(email);

            // then
            assertEquals(authCode, result);

            verify(valueOperations).set(redisKey, authCode, 300000L, TimeUnit.MILLISECONDS);
            verify(emailUtil).createMailVerify(senderEmail, email, authCode);
            verify(javaMailSender).send(mimeMessage);
        }
    }

    @Test
    @DisplayName("이메일 인증 성공")
    void verifyEmailCodeSuccess() {
        String email = "test@example.com";
        String code = "ABC123";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("emailAuth:" + email)).thenReturn(code);

        boolean result = emailService.verifyEmailCode(email, code);

        assertTrue(result);
        verify(valueOperations).set("emailVerify:" + email, "true", 5, TimeUnit.MINUTES);
        verify(redisTemplate).delete("emailAuth:" + email);
    }

    @Test
    @DisplayName("이메일 인증 실패 - 코드 불일치")
    void verifyEmailCodeFail() {
        String email = "test@example.com";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("emailAuth:" + email)).thenReturn("DIFFERENT");

        boolean result = emailService.verifyEmailCode(email, "WRONG");

        assertFalse(result);
        verify(valueOperations, never()).set(anyString(), anyString(), anyLong(), any());
    }
}

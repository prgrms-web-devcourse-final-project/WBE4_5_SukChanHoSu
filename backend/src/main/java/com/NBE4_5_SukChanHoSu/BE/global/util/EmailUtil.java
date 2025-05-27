package com.NBE4_5_SukChanHoSu.BE.global.util;

import com.NBE4_5_SukChanHoSu.BE.domain.email.template.EmailTemplateBuilder;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
@RequiredArgsConstructor
public class EmailUtil {
    private final JavaMailSender javaMailSender;
    private final EmailTemplateBuilder emailTemplateBuilder;

    private static final int AUTH_CODE_LENGTH = 6;
    private static final int CHAR_OR_DIGIT_DIVIDER = 2;
    private static final int ALPHABET_COUNT = 26;
    private static final int ASCII_UPPERCASE_A = 65;
    private static final int DIGIT_COUNT = 10;
    private static final int START_INDEX = 0;

    private static final String CHARSET_UTF8 = "UTF-8";
    private static final String CONTENT_TYPE_HTML = "html";
    private static final String EMAIL_AUTH_SUBJECT = "메일 검증";

    public static String createAuthCode() {
        Random random = new Random();
        StringBuilder key = new StringBuilder();

        for (int i = START_INDEX; i < AUTH_CODE_LENGTH; i++) {
            int index = random.nextInt(CHAR_OR_DIGIT_DIVIDER);
            if (index == START_INDEX) {
                key.append((char) (random.nextInt(ALPHABET_COUNT) + ASCII_UPPERCASE_A));
            } else {
                key.append(random.nextInt(DIGIT_COUNT));
            }
        }
        return key.toString();
    }

    public MimeMessage createMailVerify(String from, String to, String authCode) throws MessagingException {
        MimeMessage message = javaMailSender.createMimeMessage();
        message.setFrom(from);
        message.setRecipients(MimeMessage.RecipientType.TO, to);
        message.setSubject(EMAIL_AUTH_SUBJECT);

        String body = emailTemplateBuilder.buildAuthEmail(authCode);
        message.setText(body, CHARSET_UTF8, CONTENT_TYPE_HTML);

        return message;
    }
}

package com.sef.cli.feedback.service;

import com.sef.cli.api.request.FeedbackRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class FeedbackMailService {

    private static final String SUBJECT_PREFIX = "[SEF CLI 意見回饋] ";

    private final JavaMailSender mailSender;

    @Value("${feedback.mail.from}")
    private String mailFrom;

    @Value("${feedback.mail.to}")
    private String[] mailTo;

    public void send(FeedbackRequest request) {
        log.info("[FEEDBACK_SUBMIT] 收到意見回饋, title={}", request.getTitle());
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(mailTo);
        message.setSubject(SUBJECT_PREFIX + request.getTitle());
        message.setText(buildBody(request));
        try {
            mailSender.send(message);
        } catch (RuntimeException ex) {
            log.error("[FEEDBACK_MAIL_FAIL] 意見回饋信件寄送失敗, title={}, 錯誤: {}",
                    request.getTitle(), ex.getMessage(), ex);
            throw ex;
        }
        log.info("意見回饋信件寄送成功：{}", request.getTitle());
    }

    private String buildBody(FeedbackRequest request) {
        String username = request.getUsername();
        String reporter = (username == null || username.isBlank()) ? "匿名" : username;
        return "回報者：" + reporter + "\n\n" + request.getContent();
    }
}

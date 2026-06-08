package com.sef.cli.feedback.service;

import com.sef.cli.api.request.FeedbackRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FeedbackMailServiceTest {

    @Mock
    JavaMailSender mailSender;

    @InjectMocks
    FeedbackMailService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "mailFrom", "from@example.com");
        ReflectionTestUtils.setField(service, "mailTo", new String[]{"to1@example.com", "to2@example.com"});
    }

    @Test
    void send_buildsMessageWithSubjectPrefixAndNamedReporter() {
        FeedbackRequest req = new FeedbackRequest();
        req.setTitle("登入問題");
        req.setContent("按鈕沒反應");
        req.setUsername("小明");

        service.send(req);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage msg = captor.getValue();
        assertThat(msg.getFrom()).isEqualTo("from@example.com");
        assertThat(msg.getTo()).containsExactly("to1@example.com", "to2@example.com");
        assertThat(msg.getSubject()).isEqualTo("[SEF CLI 意見回饋] 登入問題");
        assertThat(msg.getText()).isEqualTo("回報者：小明\n\n按鈕沒反應");
    }

    @Test
    void send_usesAnonymousReporter_whenUsernameBlank() {
        FeedbackRequest req = new FeedbackRequest();
        req.setTitle("t");
        req.setContent("c");
        req.setUsername("   ");

        service.send(req);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue().getText()).isEqualTo("回報者：匿名\n\nc");
    }

    @Test
    void send_usesAnonymousReporter_whenUsernameNull() {
        FeedbackRequest req = new FeedbackRequest();
        req.setTitle("t");
        req.setContent("c");

        service.send(req);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue().getText()).isEqualTo("回報者：匿名\n\nc");
    }
}

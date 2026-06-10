package com.sef.cli.testutil;

import ch.qos.logback.classic.Level;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * 驗證 {@link LogCaptor} 測試輔助本身的行為（捕捉 / 斷言 / throwable / 清理）。
 */
class LogCaptorTest {

    private static final Logger log = LoggerFactory.getLogger(LogCaptorTest.class);

    @Test
    void capturesMatchingEventTagLevelAndKeys() {
        try (LogCaptor captor = LogCaptor.forClass(LogCaptorTest.class)) {
            log.info("[SAMPLE] 測試事件, userId={}, n={}", "u-1", 3);

            assertThat(captor.has(Level.INFO, "[SAMPLE]", "userId=u-1", "n=3")).isTrue();
            assertThatCode(() -> captor.assertLogged(Level.INFO, "[SAMPLE]", "userId=u-1"))
                    .doesNotThrowAnyException();
        }
    }

    @Test
    void assertLoggedFailsWhenLevelMismatch() {
        try (LogCaptor captor = LogCaptor.forClass(LogCaptorTest.class)) {
            log.info("[SAMPLE] 只是 info");

            assertThatThrownBy(() -> captor.assertLogged(Level.WARN, "[SAMPLE]"))
                    .isInstanceOf(AssertionError.class);
        }
    }

    @Test
    void capturesThrowableOnErrorLog() {
        try (LogCaptor captor = LogCaptor.forClass(LogCaptorTest.class)) {
            log.error("[BOOM] 失敗, reason={}", "x", new IllegalStateException("boom"));

            captor.assertLoggedWithThrowable(Level.ERROR, "[BOOM]");
        }
    }

    @Test
    void stopsCapturingAfterClose() {
        LogCaptor captor = LogCaptor.forClass(LogCaptorTest.class);
        captor.close();

        log.info("[AFTER_CLOSE] 不應被捕捉");

        assertThat(captor.has(Level.INFO, "[AFTER_CLOSE]")).isFalse();
    }
}

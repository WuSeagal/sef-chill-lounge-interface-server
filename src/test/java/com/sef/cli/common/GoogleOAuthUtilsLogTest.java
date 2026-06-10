package com.sef.cli.common;

import ch.qos.logback.classic.Level;
import com.sef.cli.testutil.LogCaptor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GoogleOAuthUtils 失敗路徑 log 斷言（section 2.3）。
 *
 * <p>例外路徑（idToken 無法解析）可靠可測：傳入非 JWT 字串，{@code GoogleIdToken.parse} 即拋例外，
 * 走 catch → error 分支，確認以 {@code log.error}（附 exception）取代既有 {@code e.printStackTrace()}。
 *
 * <p>「verify 回 false → warn」分支需連網抓 Google 憑證、offline 會改走例外路徑，無法穩定單測；
 * 該分支以 {@code System.err.println} 移除（見 GoogleOAuthUtils）+ task 2.5 grep 檢查兜底。
 */
class GoogleOAuthUtilsLogTest {

    @Test
    void verifyIdToken_unparseableToken_logsErrorWithThrowable_andReturnsNull() {
        try (LogCaptor captor = LogCaptor.forClass(GoogleOAuthUtils.class)) {
            var result = GoogleOAuthUtils.verifyIdToken("aaa.bbb.ccc");

            assertThat(result).isNull();
            captor.assertLoggedWithThrowable(Level.ERROR, "[OAUTH_VERIFY_ERROR]");
        }
    }
}

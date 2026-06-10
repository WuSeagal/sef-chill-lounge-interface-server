package com.sef.cli.testutil;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 測試輔助：對指定 class 的 logger 掛 logback {@link ListAppender}，捕捉 log 事件後斷言。
 *
 * <p>斷言只鎖「event tag + level + 關鍵 key」（見 design D8），不逐字比對中文描述，
 * 中文措辭調整不會破壞測試。實作 {@link AutoCloseable}，建議以 try-with-resources 使用以自動卸載 appender。
 *
 * <pre>{@code
 * try (LogCaptor captor = LogCaptor.forClass(MyService.class)) {
 *     service.doThing();
 *     captor.assertLogged(Level.INFO, "[THING_DONE]", "userId=u-1");
 * }
 * }</pre>
 */
public class LogCaptor implements AutoCloseable {

    private final Logger logger;
    private final Level previousLevel;
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    private LogCaptor(Class<?> clazz) {
        this.logger = (Logger) LoggerFactory.getLogger(clazz);
        this.previousLevel = logger.getLevel();
        // 降到 TRACE 才能捕捉 debug/trace（否則 logger 有效層級 INFO 會在建立事件前過濾掉）
        this.logger.setLevel(Level.TRACE);
        this.appender.start();
        this.logger.addAppender(appender);
    }

    public static LogCaptor forClass(Class<?> clazz) {
        return new LogCaptor(clazz);
    }

    /** 捕捉到的所有事件（依時間順序）。 */
    public List<ILoggingEvent> events() {
        return appender.list;
    }

    /** 是否存在符合 level、含 tag 與全部 keys（皆為 formattedMessage 子字串）的事件。 */
    public boolean has(Level level, String tag, String... keys) {
        return appender.list.stream().anyMatch(e -> matches(e, level, tag, keys));
    }

    /** 斷言存在符合的事件，否則拋 {@link AssertionError}。 */
    public void assertLogged(Level level, String tag, String... keys) {
        assertThat(appender.list)
                .as("預期一條 %s log 含 %s 與 keys %s", level, tag, Arrays.toString(keys))
                .anyMatch(e -> matches(e, level, tag, keys));
    }

    /** 斷言存在符合的事件且附帶 throwable（error log 必附 exception 物件）。 */
    public void assertLoggedWithThrowable(Level level, String tag, String... keys) {
        assertThat(appender.list)
                .as("預期一條 %s log 含 %s（附 throwable）與 keys %s", level, tag, Arrays.toString(keys))
                .anyMatch(e -> matches(e, level, tag, keys) && e.getThrowableProxy() != null);
    }

    private static boolean matches(ILoggingEvent e, Level level, String tag, String... keys) {
        if (e.getLevel() != level || !e.getFormattedMessage().contains(tag)) {
            return false;
        }
        return Arrays.stream(keys).allMatch(k -> e.getFormattedMessage().contains(k));
    }

    @Override
    public void close() {
        logger.detachAppender(appender);
        appender.stop();
        logger.setLevel(previousLevel); // 還原原層級（null = 回到繼承）
    }
}

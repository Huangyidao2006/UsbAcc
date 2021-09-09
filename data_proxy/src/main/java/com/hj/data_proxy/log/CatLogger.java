package com.hj.data_proxy.log;


/**
 * Created by huangjian at 20-11-18 16:08
 *
 * logcat日志打印类。
 */
public final class CatLogger extends BaseCatLogger {
    public static final String DEFAULT_TAG = "HJ_SDK";

    private static final CatLogger INSTANCE = new CatLogger();

    private LogLevel mPrintLevel = LogLevel.INFO;

    protected boolean needPrint(LogLevel level) {
        return level.ordinal() >= mPrintLevel.ordinal();
    }

    private CatLogger() {

    }

    public void setPrintLogLevel(LogLevel level) {
        mPrintLevel = level;
    }

    public static CatLogger getInstance() {
        return INSTANCE;
    }

    public static void i(String fmt, Object... args) {
        INSTANCE._i(fmt, args);
    }

    public static void d(String fmt, Object... args) {
        INSTANCE._d(fmt, args);
    }

    public static void w(String fmt, Object... args) {
        INSTANCE._w(fmt, args);
    }

    public static void e(String fmt, Object... args) {
        INSTANCE._e(fmt, args);
    }

    public static void i(String tag, String fmt, Object... args) {
        INSTANCE._i(tag, fmt, args);
    }

    public static void d(String tag, String fmt, Object... args) {
        INSTANCE._d(tag, fmt, args);
    }

    public static void w(String tag, String fmt, Object... args) {
        INSTANCE._w(tag, fmt, args);
    }

    public static void e(String tag, String fmt, Object... args) {
        INSTANCE._e(tag, fmt, args);
    }
}

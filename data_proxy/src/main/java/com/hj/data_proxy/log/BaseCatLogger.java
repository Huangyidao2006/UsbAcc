package com.hj.data_proxy.log;

import android.text.TextUtils;
import android.util.Log;

import java.util.Locale;

/**
 * Created by huangjian at 21-5-19 09:26
 *
 * 用于打印logcat日志的基础类。
 */
public abstract class BaseCatLogger {
    protected String DEFAULT_TAG = "HJ_SDK";

    protected LogListener mLogListener;

    public interface LogListener {
        void onLog(long timeMillis, int pid, int tid, LogLevel level, String tag, String log);
    }

    public void setLogListener(LogListener listener) {
        mLogListener = listener;
    }

    protected abstract boolean needPrint(LogLevel level);

    protected void printLog(final LogLevel level, String tag, final String log) {
        if (needPrint(level)) {
            if (TextUtils.isEmpty(tag)) {
                tag = DEFAULT_TAG;
            }

            switch (level) {
                case DEBUG: {
                    Log.d(tag, log);
                }
                break;

                case INFO: {
                    Log.i(tag, log);
                }
                break;

                case WARN: {
                    Log.w(tag, log);
                }
                break;

                case NONE: {

                }
                break;

                case ERROR: {
                    Log.e(tag, log);
                }
                break;

                default:
            }
        }
    }

    public void _i(String fmt, Object... args) {
        String log = String.format(Locale.CHINA, fmt, args);
        printLog(LogLevel.INFO, DEFAULT_TAG, log);
    }

    public void _d(String fmt, Object... args) {
        String log = String.format(Locale.CHINA, fmt, args);
        printLog(LogLevel.DEBUG, DEFAULT_TAG, log);
    }

    public void _w(String fmt, Object... args) {
        String log = String.format(Locale.CHINA, fmt, args);
        printLog(LogLevel.WARN, DEFAULT_TAG, log);
    }

    public void _e(String fmt, Object... args) {
        String log = String.format(Locale.CHINA, fmt, args);
        printLog(LogLevel.ERROR, DEFAULT_TAG, log);
    }

    public void _i(String tag, String fmt, Object... args) {
        String log = String.format(Locale.CHINA, fmt, args);
        printLog(LogLevel.INFO, tag, log);
    }

    public void _d(String tag, String fmt, Object... args) {
        String log = String.format(Locale.CHINA, fmt, args);
        printLog(LogLevel.DEBUG, tag, log);
    }

    public void _w(String tag, String fmt, Object... args) {
        String log = String.format(Locale.CHINA, fmt, args);
        printLog(LogLevel.WARN, tag, log);
    }

    public void _e(String tag, String fmt, Object... args) {
        String log = String.format(Locale.CHINA, fmt, args);
        printLog(LogLevel.ERROR, tag, log);
    }

}

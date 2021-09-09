package com.hj.data_proxy.log;

/**
 * 日志等级。等级依次升高。
 */
public enum LogLevel {
    DEBUG,
    INFO,
    WARN,
    NONE,   // 不打印除error之外的日志
    ERROR
}

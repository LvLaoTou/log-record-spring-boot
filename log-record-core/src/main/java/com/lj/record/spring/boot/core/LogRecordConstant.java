package com.lj.record.spring.boot.core;

/**
 * 日志记录常量类
 * @author lvlaotou
 */
public class LogRecordConstant {

    /**
     * 常量类私有无参构造
     */
    private LogRecordConstant(){}

    public static final int AOP_ORDER = Integer.MAX_VALUE;

    /**
     * spel上下文变量key  方法返回值
     */
    public static final String METHOD_RESULT_EVALUATION = "methodResult";

    /**
     * spel上下文变量key  错误信息
     */
    public static final String ERROR_MESSAGE_EVALUATION = "errorMessage";
}

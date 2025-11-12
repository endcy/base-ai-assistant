package com.assistant.service.common.utils;

import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * 异常工具
 */
public class ThrowableUtil {

    /**
     * 获取堆栈信息
     */
    public static String getStackTrace(Throwable throwable) {
        StringWriter sw = new StringWriter();
        try (PrintWriter pw = new PrintWriter(sw)) {
            throwable.printStackTrace(pw);
            return sw.toString();
        }
    }

    /**
     * 提取堆栈轨迹中的真实异常
     */
    public static String extractStackTraceCausedBy(Throwable cause) {
        return extractStackTraceCausedBy(true, cause);
    }


    /**
     * 提取堆栈轨迹中的真实异常
     *
     * @param preaddMethodLine 是否在异常描述前，追加发生异常的方法行
     */
    public static String extractStackTraceCausedBy(boolean preaddMethodLine, Throwable cause) {
        String msg = ExceptionUtils.getStackTrace(cause);
        if (msg.contains("Caused by: ")) {
            msg = msg.substring(msg.lastIndexOf("Caused by: ") + 11);
        }
        msg = msg.substring(0, msg.indexOf("\n"));
        if (msg.endsWith("\r")) {
            msg = msg.substring(0, msg.length() - 1);
        }
        if (preaddMethodLine) {
            String methodLine = cause.getStackTrace()[0].toString();
            methodLine = methodLine.substring(0, methodLine.indexOf("("));
            msg = cause.getStackTrace()[0].toString().substring(methodLine.lastIndexOf(".") + 1) + " || " + msg;
        }
        return msg;
    }
}

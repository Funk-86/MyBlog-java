package org.example.myblog.config;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.catalina.connector.ClientAbortException;

import java.io.EOFException;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * 捕获未处理异常并打印完整堆栈，便于排查问题。
 * 以下预期异常不打印，减少日志噪音：favicon/根路径 404、客户端断开连接（超时/取消）。
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static boolean shouldLog(Throwable t) {
        if (t instanceof NoResourceFoundException) return false;  // favicon.ico、/ 等 404
        if (t instanceof ClientAbortException) return false;      // 客户端提前断开（超时、取消）
        Throwable cause = t;
        while (cause != null) {
            if (cause instanceof EOFException) return false;      // 上传/下载时客户端断开
            cause = cause.getCause();
        }
        return true;
    }

    @ExceptionHandler(Throwable.class)
    public void handleThrowable(Throwable t, HttpServletResponse response) throws Throwable {
        if (shouldLog(t)) {
            StringWriter sw = new StringWriter();
            t.printStackTrace(new PrintWriter(sw));
            System.err.println("[GlobalException] " + t.getClass().getName() + ": " + t.getMessage());
            System.err.println(sw.toString());
        }
        throw t;
    }
}

package org.example.myblog.config;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * 捕获未处理异常并打印完整堆栈，便于在 Zeabur 等平台查看真实原因
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Throwable.class)
    public void handleThrowable(Throwable t, HttpServletResponse response) throws Throwable {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        System.err.println("[GlobalException] " + t.getClass().getName() + ": " + t.getMessage());
        System.err.println(sw.toString());
        throw t;
    }
}

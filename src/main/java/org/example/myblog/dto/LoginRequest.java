package org.example.myblog.dto;

import lombok.Data;

/**
 * 登录请求体（用于接收 JSON）
 */
@Data
public class LoginRequest {

    private String email;

    private String password;
}


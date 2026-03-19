package org.example.myblog.serverl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Random;

/**
 * 邮箱验证码服务：生成验证码、存入 Redis、异步发邮件、校验并清理
 */
@Service
public class EmailCodeService {

    private static final String REGISTER_KEY_PREFIX = "email:code:register:";
    private static final String RESET_KEY_PREFIX = "email:code:reset:";
    private static final long EXPIRE_MINUTES = 5;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String mailFrom;

    /**
     * 发送注册验证码
     */
    public void sendRegisterCode(String email) {
        String code = generateCode();
        String key = REGISTER_KEY_PREFIX + email;
        stringRedisTemplate.opsForValue()
                .set(key, code, Duration.ofMinutes(EXPIRE_MINUTES));
        sendMailAsync(email, "注册验证码", "您的注册验证码是：" + code + "，5 分钟内有效。");
    }

    /**
     * 校验注册验证码（成功则删除）
     */
    public boolean verifyRegisterCode(String email, String code) {
        String key = REGISTER_KEY_PREFIX + email;
        String value = stringRedisTemplate.opsForValue().get(key);
        if (value != null && value.equals(code)) {
            stringRedisTemplate.delete(key);
            return true;
        }
        return false;
    }

    /**
     * 发送重置密码验证码
     */
    public void sendResetCode(String email) {
        String code = generateCode();
        String key = RESET_KEY_PREFIX + email;
        stringRedisTemplate.opsForValue()
                .set(key, code, Duration.ofMinutes(EXPIRE_MINUTES));
        sendMailAsync(email, "重置密码验证码", "您的重置密码验证码是：" + code + "，5 分钟内有效。");
    }

    /**
     * 校验重置密码验证码（成功则删除）
     */
    public boolean verifyResetCode(String email, String code) {
        String key = RESET_KEY_PREFIX + email;
        String value = stringRedisTemplate.opsForValue().get(key);
        if (value != null && value.equals(code)) {
            stringRedisTemplate.delete(key);
            return true;
        }
        return false;
    }

    private String generateCode() {
        // 6 位数字验证码
        return String.format("%06d", new Random().nextInt(1000000));
    }

    @Async
    protected void sendMailAsync(String to, String subject, String content) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);  // 163 要求发件人必须等于登录账号
        message.setTo(to);
        message.setSubject(subject);
        message.setText(content);
        mailSender.send(message);
    }
}


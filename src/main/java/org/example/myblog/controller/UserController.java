package org.example.myblog.controller;

import org.example.myblog.dto.FollowUserDTO;
import org.example.myblog.mapper.UserMapper;

import org.example.myblog.dto.UserSpaceDTO;
import org.example.myblog.entiy.User;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.example.myblog.mapper.UserProfileMapper;
import org.example.myblog.serverl.EmailCodeService;
import org.example.myblog.serverl.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Controller
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private EmailCodeService emailCodeService;

    @Autowired
    private UserProfileMapper userProfileMapper;

    @Autowired
    private UserMapper userMapper;

    /**
     * 管理端：用户列表
     * GET /user/admin/list?page=1&size=20&keyword=xxx
     */
    @GetMapping("/admin/list")
    @ResponseBody
    public List<Map<String, Object>> adminUserList(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "keyword", required = false) String keyword) {
        int offset = (page - 1) * size;
        if (keyword != null && !keyword.trim().isEmpty()) {
            return userMapper.listForAdminWithKeyword(offset, size, keyword.trim());
        }
        return userMapper.listForAdmin(offset, size);
    }

    /**
     * 管理端：添加用户
     * POST /user/admin/add
     * Body: { "username": "xx", "email": "xx@xx.com", "password": "xx", "role": 0 }
     * role: 0=普通用户 1=管理员
     */
    @PostMapping("/admin/add")
    @ResponseBody
    public Map<String, Object> adminAddUser(@RequestBody Map<String, Object> body) {
        String username = body.get("username") != null ? body.get("username").toString().trim() : null;
        String email = body.get("email") != null ? body.get("email").toString().trim() : null;
        String password = body.get("password") != null ? body.get("password").toString() : null;
        Object roleObj = body.get("role");
        Integer role = (roleObj instanceof Number) ? ((Number) roleObj).intValue() : 0;
        if (username == null || username.isEmpty()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (email == null || email.isEmpty()) {
            throw new IllegalArgumentException("邮箱不能为空");
        }
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }
        User user = userService.createUserByAdmin(username, email, password, role);
        Map<String, Object> result = new HashMap<>();
        if (user == null) {
            result.put("success", false);
            result.put("message", "用户名或邮箱已存在");
        } else {
            result.put("success", true);
            result.put("user", user);
        }
        return result;
    }

    /**
     * 管理端：封禁/解封/注销用户（批量）
     * PUT /user/admin/status
     * Body: { "ids": [1, 2], "status": 1, "duration": { "value": 7, "unit": "day" } }  // 封禁时可选 duration；无或 unit=permanent 为永久
     * status: 0=正常 1=封禁 2=注销；duration.unit: day|month|year|permanent
     */
    @PutMapping("/admin/status")
    @ResponseBody
    public Map<String, Object> adminUpdateStatus(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Number> idList = (List<Number>) body.get("ids");
        if (idList == null || idList.isEmpty()) {
            throw new IllegalArgumentException("ids 不能为空");
        }
        Object st = body.get("status");
        if (st == null) {
            throw new IllegalArgumentException("status 不能为空");
        }
        int status = ((Number) st).intValue();
        if (status < 0 || status > 2) {
            throw new IllegalArgumentException("status 只能为 0(正常)、1(封禁)、2(注销)");
        }
        List<Long> ids = idList.stream().map(Number::longValue).collect(Collectors.toList());
        if (status == 1) {
            LocalDateTime bannedUntil = parseBannedUntil(body.get("duration"));
            userMapper.updateBanBatch(ids, bannedUntil);
        } else {
            userMapper.updateStatusBatch(ids, status);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return result;
    }

    /** 根据 duration 计算封禁截止时间，null 或 permanent 表示永久封禁 */
    private LocalDateTime parseBannedUntil(Object duration) {
        if (duration == null) return null;
        if (!(duration instanceof Map)) return null;
        @SuppressWarnings("unchecked")
        Map<String, Object> d = (Map<String, Object>) duration;
        String unit = d.get("unit") != null ? d.get("unit").toString().toLowerCase() : "";
        if ("permanent".equals(unit) || "永久".equals(unit)) return null;
        Object val = d.get("value");
        int value = val instanceof Number ? ((Number) val).intValue() : 0;
        if (value <= 0) return null;
        LocalDateTime now = LocalDateTime.now();
        return switch (unit) {
            case "day", "天" -> now.plus(value, ChronoUnit.DAYS);
            case "month", "月" -> now.plus(value, ChronoUnit.MONTHS);
            case "year", "年" -> now.plus(value, ChronoUnit.YEARS);
            default -> null;
        };
    }

    /**
     * 登录接口（支持账号 username 或邮箱 email）
     * POST /user/login?account=xxx&password=yyy
     */
    @PostMapping("/login")
    @ResponseBody
    public User login(@RequestParam("account") String account,
                      @RequestParam("password") String password) {
        User user = userService.login(account, password);
        if (user == null) return null;
        if (user.getStatus() != null && user.getStatus() == 1) {
            java.time.LocalDateTime now = LocalDateTime.now();
            java.time.LocalDateTime until = user.getBannedUntil();
            if (until == null) {
                throw new IllegalStateException("账号已被永久封禁");
            }
            if (now.isBefore(until)) {
                throw new IllegalStateException("账号已封禁至 " + until);
            }
            userMapper.updateStatus(user.getId(), 0);
            user.setStatus(0);
            user.setBannedUntil(null);
        }
        return user;
    }

    /**
     * 发送注册验证码
     * POST /user/sendRegisterCode?email=xxx
     */
    @PostMapping("/sendRegisterCode")
    @ResponseBody
    public String sendRegisterCode(@RequestParam("email") String email) {
        emailCodeService.sendRegisterCode(email);
        return "ok";
    }

    /**
     * 通过邮箱 + 验证码注册
     * POST /user/register?email=xxx&password=yyy&code=123456
     */
    @PostMapping("/register")
    @ResponseBody
    public User register(@RequestParam("email") String email,
                         @RequestParam("password") String password,
                         @RequestParam("code") String code) {
        return userService.registerByEmail(email, code, password);
    }

    /**
     * 发送重置密码验证码
     * POST /user/sendResetCode?email=xxx
     */
    @PostMapping("/sendResetCode")
    @ResponseBody
    public String sendResetCode(@RequestParam("email") String email) {
        emailCodeService.sendResetCode(email);
        return "ok";
    }

    /**
     * 通过邮箱 + 验证码重置密码
     * POST /user/resetPassword?email=xxx&newPassword=yyy&code=123456
     */
    @PostMapping("/resetPassword")
    @ResponseBody
    public String resetPassword(@RequestParam("email") String email,
                                @RequestParam("newPassword") String newPassword,
                                @RequestParam("code") String code) {
        boolean success = userService.resetPasswordByEmail(email, code, newPassword);
        return success ? "ok" : "fail";
    }

    /**
     * 查询个人空间信息
     * GET /user/space?userId=1
     */
    @GetMapping("/space")
    @ResponseBody
    public UserSpaceDTO space(@RequestParam("userId") Long userId) {
        return userService.getUserSpace(userId);
    }

    /**
     * 查询是否已关注
     * GET /user/follow/status?userId=1&targetId=2
     */
    @GetMapping("/follow/status")
    @ResponseBody
    public boolean followStatus(@RequestParam("userId") Long userId,
                                @RequestParam("targetId") Long targetId) {
        return userService.isFollowing(userId, targetId);
    }

    /**
     * 关注
     * POST /user/follow?userId=1&targetId=2
     */
    @PostMapping("/follow")
    @ResponseBody
    public String follow(@RequestParam("userId") Long userId,
                         @RequestParam("targetId") Long targetId) {
        boolean ok = userService.follow(userId, targetId);
        return ok ? "ok" : "fail";
    }

    /**
     * 取消关注
     * POST /user/unfollow?userId=1&targetId=2
     */
    @PostMapping("/unfollow")
    @ResponseBody
    public String unfollow(@RequestParam("userId") Long userId,
                           @RequestParam("targetId") Long targetId) {
        boolean ok = userService.unfollow(userId, targetId);
        return ok ? "ok" : "fail";
    }

    /**
     * 我关注的人列表（发私信用）
     * GET /user/following?userId=1&limit=50
     */
    @GetMapping("/following")
    @ResponseBody
    public List<FollowUserDTO> following(@RequestParam("userId") Long userId,
                                         @RequestParam(value = "limit", defaultValue = "50") int limit) {
        return userService.listFollowing(userId, limit);
    }

    /**
     * 我的粉丝列表（@ 只能 @ 粉丝，发帖时用）
     * GET /user/followers?userId=1&limit=100
     */
    @GetMapping("/followers")
    @ResponseBody
    public List<FollowUserDTO> followers(@RequestParam("userId") Long userId,
                                         @RequestParam(value = "limit", defaultValue = "100") int limit) {
        return userService.listFollowers(userId, limit);
    }

    /**
     * 更新资料（昵称、简介、性别）
     * POST /user/profile/update?userId=1&nickname=xx&bio=xx&gender=1
     */
    @PostMapping("/profile/update")
    @ResponseBody
    public String updateProfile(@RequestParam("userId") Long userId,
                                @RequestParam(value = "nickname", required = false) String nickname,
                                @RequestParam(value = "bio", required = false) String bio,
                                @RequestParam(value = "gender", required = false) Integer gender) {
        int n = userProfileMapper.upsertProfile(userId, nickname, bio, gender);
        return n > 0 ? "ok" : "fail";
    }

    /**
     * 上传用户头像
     * POST /user/uploadAvatar
     * form-data: userId, file
     *
     * 返回：头像访问 URL，例如 /user_img/xxx.png
     */
    @PostMapping("/uploadAvatar")
    @ResponseBody
    public String uploadAvatar(@RequestParam("userId") Long userId,
                               @RequestParam("file") MultipartFile file) throws IOException {
        if (userId == null || file.isEmpty()) {
            return "";
        }

        // 保存到项目根目录下的 user_img 目录
        Path uploadDir = Paths.get("user_img").toAbsolutePath().normalize();
        Files.createDirectories(uploadDir);

        String originalFilename = file.getOriginalFilename();
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String fileName = userId + "_" + UUID.randomUUID() + ext;
        Path targetPath = uploadDir.resolve(fileName);

        // 在云环境中优先使用 transferTo，减少流读取/复制带来的不稳定与耗时
        file.transferTo(targetPath);

        // 对外访问路径
        String avatarUrl = "/user_img/" + fileName;
        // 更新或插入 user_profile.avatar_url
        userProfileMapper.upsertAvatar(userId, avatarUrl);

        return avatarUrl;
    }
}

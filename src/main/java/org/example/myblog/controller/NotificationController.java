package org.example.myblog.controller;

import org.example.myblog.dto.*;
import org.example.myblog.serverl.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/notification")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @GetMapping("/like")
    @ResponseBody
    public List<NotifyLikeDTO> like(@RequestParam("userId") Long userId,
                                    @RequestParam(value = "limit", defaultValue = "20") int limit) {
        return notificationService.listLikeNotify(userId, limit);
    }

    @GetMapping("/reply")
    @ResponseBody
    public List<NotifyReplyDTO> reply(@RequestParam("userId") Long userId,
                                      @RequestParam(value = "limit", defaultValue = "20") int limit) {
        return notificationService.listReplyNotify(userId, limit);
    }

    @GetMapping("/at")
    @ResponseBody
    public List<NotifyAtDTO> at(@RequestParam("userId") Long userId,
                                @RequestParam(value = "limit", defaultValue = "20") int limit) {
        return notificationService.listAtNotify(userId, limit);
    }

    @GetMapping("/fans")
    @ResponseBody
    public List<NotifyFansDTO> fans(@RequestParam("userId") Long userId,
                                    @RequestParam(value = "limit", defaultValue = "20") int limit) {
        return notificationService.listFansNotify(userId, limit);
    }
}

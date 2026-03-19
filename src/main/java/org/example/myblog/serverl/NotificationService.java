package org.example.myblog.serverl;

import org.example.myblog.dto.*;

import java.util.List;

public interface NotificationService {

    List<NotifyLikeDTO> listLikeNotify(Long userId, int limit);

    List<NotifyReplyDTO> listReplyNotify(Long userId, int limit);

    List<NotifyAtDTO> listAtNotify(Long userId, int limit);

    List<NotifyFansDTO> listFansNotify(Long userId, int limit);
}

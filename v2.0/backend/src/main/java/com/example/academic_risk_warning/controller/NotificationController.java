package com.example.academic_risk_warning.controller;

import com.example.academic_risk_warning.common.Result;
import com.example.academic_risk_warning.security.RequireRole;
import com.example.academic_risk_warning.security.UserContext;
import com.example.academic_risk_warning.service.NotificationService;
import com.example.academic_risk_warning.vo.NotificationPageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 四端共用 · 消息中心。
 * <p>消息可见范围由令牌里的账号与角色决定（notification_receipt.recipient_id / recipient_role），
 * 客户端不传“我是谁”，传了也无效。</p>
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@RequireRole({ "STUDENT", "TEACHER", "COUNSELOR", "ADMIN" })
public class NotificationController {

    private final NotificationService notificationService;

    /** 我的消息列表（分页）：unreadOnly=true 只看未读 */
    @GetMapping
    public Result<NotificationPageVO> list(@RequestParam(required = false) Boolean unreadOnly,
                                           @RequestParam(required = false, defaultValue = "1") Integer page,
                                           @RequestParam(required = false, defaultValue = "20") Integer size) {
        return Result.ok(notificationService.list(UserContext.require(), unreadOnly, page, size));
    }

    /** 未读数（顶栏红点，轮询用） */
    @GetMapping("/unread-count")
    public Result<Long> unreadCount() {
        return Result.ok(notificationService.unreadCount(UserContext.require()));
    }

    /** 标记单条已读 */
    @PostMapping("/{id}/read")
    public Result<Void> read(@PathVariable Long id) {
        notificationService.markRead(UserContext.require(), id);
        return Result.ok();
    }

    /** 全部标为已读 */
    @PostMapping("/read-all")
    public Result<Integer> readAll() {
        return Result.ok(notificationService.markAllRead(UserContext.require()));
    }
}

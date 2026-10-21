package com.example.academic_risk_warning.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.academic_risk_warning.common.BizException;
import com.example.academic_risk_warning.entity.Notification;
import com.example.academic_risk_warning.entity.NotificationReceipt;
import com.example.academic_risk_warning.mapper.NotificationMapper;
import com.example.academic_risk_warning.mapper.NotificationReceiptMapper;
import com.example.academic_risk_warning.security.LoginUser;
import com.example.academic_risk_warning.vo.NotificationPageVO;
import com.example.academic_risk_warning.vo.NotificationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 四端消息中心。
 * <p>数据来源：notification（通知主体，按 aggregate_key 聚合去重）+ notification_receipt（每个接收人 × 渠道一行）。
 * 一条消息“对当前人可见”的判定条件是存在 receipt：recipient_id = 当前账号、recipient_role = 当前角色、channel = IN_APP。
 * 已读状态也记在 receipt 上，不影响其他接收人。</p>
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final String CHANNEL_IN_APP = "IN_APP";

    private final NotificationMapper notificationMapper;
    private final NotificationReceiptMapper receiptMapper;

    /** 我的消息（分页）；unreadOnly=true 只看未读 */
    public NotificationPageVO list(LoginUser user, Boolean unreadOnly, Integer page, Integer size) {
        Long accountId = accountIdOf(user);
        boolean onlyUnread = Boolean.TRUE.equals(unreadOnly);
        int pageNo = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 100);

        Long total = receiptMapper.selectCount(receiptQuery(user, accountId, onlyUnread));
        Long unread = receiptMapper.selectCount(receiptQuery(user, accountId, true));

        NotificationPageVO vo = new NotificationPageVO();
        vo.setPage(pageNo);
        vo.setSize(pageSize);
        vo.setTotal(total == null ? 0 : total);
        vo.setUnread(unread == null ? 0 : unread);

        List<NotificationReceipt> receipts = receiptMapper.selectList(
                receiptQuery(user, accountId, onlyUnread)
                        .orderByDesc(NotificationReceipt::getSendTime)
                        .orderByDesc(NotificationReceipt::getId)
                        .last("limit " + pageSize + " offset " + (long) (pageNo - 1) * pageSize));
        if (receipts.isEmpty()) {
            vo.setItems(Collections.emptyList());
            return vo;
        }

        List<Long> ids = receipts.stream().map(NotificationReceipt::getNotificationId)
                .filter(java.util.Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, Notification> notificationMap = new HashMap<>();
        if (!ids.isEmpty()) {
            notificationMapper.selectList(Wrappers.<Notification>lambdaQuery().in(Notification::getId, ids))
                    .forEach(n -> notificationMap.put(n.getId(), n));
        }

        List<NotificationVO> items = new ArrayList<>();
        for (NotificationReceipt r : receipts) {
            Notification n = notificationMap.get(r.getNotificationId());
            if (n == null) {
                continue; // 主体被清理时跳过，避免前端出现空卡片
            }
            items.add(toVO(n, r));
        }
        vo.setItems(items);
        return vo;
    }

    /** 未读数（顶栏红点） */
    public long unreadCount(LoginUser user) {
        Long unread = receiptMapper.selectCount(receiptQuery(user, accountIdOf(user), true));
        return unread == null ? 0 : unread;
    }

    /** 标记单条已读（按通知ID） */
    @Transactional
    public void markRead(LoginUser user, Long notificationId) {
        if (notificationId == null) {
            throw new BizException("通知ID不能为空");
        }
        Long accountId = accountIdOf(user);
        int rows = receiptMapper.update(null, Wrappers.<NotificationReceipt>lambdaUpdate()
                .set(NotificationReceipt::getIsRead, 1)
                .set(NotificationReceipt::getReadTime, LocalDateTime.now())
                .set(NotificationReceipt::getUpdateTime, LocalDateTime.now())
                .eq(NotificationReceipt::getNotificationId, notificationId)
                .eq(NotificationReceipt::getRecipientId, accountId)
                .eq(NotificationReceipt::getRecipientRole, user.getRole())
                .eq(NotificationReceipt::getChannel, CHANNEL_IN_APP)
                .eq(NotificationReceipt::getIsRead, 0));
        if (rows == 0) {
            // 已读或不属于当前人：不报错，幂等返回
            return;
        }
    }

    /** 全部标为已读 */
    @Transactional
    public int markAllRead(LoginUser user) {
        Long accountId = accountIdOf(user);
        return receiptMapper.update(null, Wrappers.<NotificationReceipt>lambdaUpdate()
                .set(NotificationReceipt::getIsRead, 1)
                .set(NotificationReceipt::getReadTime, LocalDateTime.now())
                .set(NotificationReceipt::getUpdateTime, LocalDateTime.now())
                .eq(NotificationReceipt::getRecipientId, accountId)
                .eq(NotificationReceipt::getRecipientRole, user.getRole())
                .eq(NotificationReceipt::getChannel, CHANNEL_IN_APP)
                .eq(NotificationReceipt::getIsRead, 0));
    }

    // ---------------- 内部 ----------------

    /** 当前人的回执查询条件（每次新建，避免 count 与 list 共用同一个 Wrapper 时被改写） */
    private LambdaQueryWrapper<NotificationReceipt> receiptQuery(LoginUser user, Long accountId, boolean onlyUnread) {
        LambdaQueryWrapper<NotificationReceipt> w = Wrappers.<NotificationReceipt>lambdaQuery()
                .eq(NotificationReceipt::getRecipientId, accountId)
                .eq(NotificationReceipt::getRecipientRole, user.getRole())
                .eq(NotificationReceipt::getChannel, CHANNEL_IN_APP);
        if (onlyUnread) {
            w.eq(NotificationReceipt::getIsRead, 0);
        }
        return w;
    }

    private Long accountIdOf(LoginUser user) {
        if (user == null || user.getAccountId() == null) {
            throw new BizException("登录信息缺少账号ID，请重新登录");
        }
        return user.getAccountId();
    }

    private NotificationVO toVO(Notification n, NotificationReceipt r) {
        NotificationVO v = new NotificationVO();
        v.setId(n.getId());
        v.setReceiptId(r.getId());
        v.setBizType(n.getBizType());
        v.setBizId(n.getBizId());
        v.setTitle(n.getTitle());
        v.setContent(n.getContent());
        v.setPayloadJson(n.getPayloadJson());
        v.setAggregateCount(n.getAggregateCount());
        v.setPriority(n.getPriority());
        v.setAudienceDesc(n.getAudienceDesc());
        v.setIsRead(r.getIsRead() == null ? 0 : r.getIsRead());
        v.setSendTime(r.getSendTime() != null ? r.getSendTime() : n.getCreateTime());
        v.setReadTime(r.getReadTime());
        v.setAckRequired(r.getAckRequired() == null ? 0 : r.getAckRequired());
        v.setAckStatus(r.getAckStatus());
        return v;
    }
}

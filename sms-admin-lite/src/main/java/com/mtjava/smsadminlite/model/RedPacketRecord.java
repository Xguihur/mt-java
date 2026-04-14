package com.mtjava.smsadminlite.model;

import java.time.LocalDateTime;

/**
 * 抢红包记录模型（POJO）。
 *
 * 每条记录代表"哪个用户在什么时候抢了哪个红包，抢到了多少钱"。
 */
public class RedPacketRecord {

    private Long id;
    private Long redPacketId;
    private Long userId;
    private String userName;

    /** 抢到的金额（分） */
    private Integer amountCents;

    private LocalDateTime grabbedAt;

    public RedPacketRecord() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRedPacketId() {
        return redPacketId;
    }

    public void setRedPacketId(Long redPacketId) {
        this.redPacketId = redPacketId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Integer getAmountCents() {
        return amountCents;
    }

    public void setAmountCents(Integer amountCents) {
        this.amountCents = amountCents;
    }

    public LocalDateTime getGrabbedAt() {
        return grabbedAt;
    }

    public void setGrabbedAt(LocalDateTime grabbedAt) {
        this.grabbedAt = grabbedAt;
    }
}

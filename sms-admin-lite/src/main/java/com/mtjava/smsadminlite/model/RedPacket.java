package com.mtjava.smsadminlite.model;

import java.time.LocalDateTime;

/**
 * 红包模型（POJO）。
 *
 * 对应数据库表 red_packet。
 * 金额单位统一用"分"（整数），避免小数精度问题。
 */
public class RedPacket {

    private Long id;
    private String title;

    /** 红包总金额（分） */
    private Integer totalAmountCents;

    /** 红包总个数 */
    private Integer totalCount;

    /** 剩余金额（分）——展示用，实际防超发由 Redis 保证 */
    private Integer remainAmountCents;

    /** 剩余个数——展示用，实际防超发由 Redis 保证 */
    private Integer remainCount;

    private LocalDateTime createdAt;

    public RedPacket() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getTotalAmountCents() {
        return totalAmountCents;
    }

    public void setTotalAmountCents(Integer totalAmountCents) {
        this.totalAmountCents = totalAmountCents;
    }

    public Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }

    public Integer getRemainAmountCents() {
        return remainAmountCents;
    }

    public void setRemainAmountCents(Integer remainAmountCents) {
        this.remainAmountCents = remainAmountCents;
    }

    public Integer getRemainCount() {
        return remainCount;
    }

    public void setRemainCount(Integer remainCount) {
        this.remainCount = remainCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

package com.mtjava.smsadminlite.model;

import java.time.LocalDateTime;

/**
 * 用户模型（POJO）。
 *
 * MyBatis 直接把查询结果映射到这个类，不需要任何注解。
 * 字段名遵循驼峰命名，对应数据库下划线列名（created_at → createdAt）由 MyBatis 配置自动处理。
 */
public class User {

    private Long id;
    private String name;
    private String phone;
    private LocalDateTime createdAt;

    public User() {
    }

    public User(Long id, String name, String phone, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

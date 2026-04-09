package com.mtjava.smsadminlite.model;

import java.time.LocalDateTime;

/**
 * 领域模型。
 *
 * 在后续接数据库后，它通常会演变成 entity / po / domain 这一类对象。
 * 这里先保留最简单的用户字段，帮助你理解 Java 类如何承载业务数据。
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

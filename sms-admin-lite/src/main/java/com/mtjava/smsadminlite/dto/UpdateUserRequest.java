package com.mtjava.smsadminlite.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 更新用户请求参数。
 *
 * 这里仍然要求 name 和 phone 一起传入，保持示例简单，
 * 重点放在"更新数据库后删除缓存"这个缓存闭环上。
 */
public class UpdateUserRequest {

    @NotBlank(message = "用户名不能为空")
    private String name;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
    private String phone;

    public UpdateUserRequest() {
    }

    public UpdateUserRequest(String name, String phone) {
        this.name = name;
        this.phone = phone;
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
}

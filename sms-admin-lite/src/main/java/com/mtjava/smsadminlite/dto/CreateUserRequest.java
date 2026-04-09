package com.mtjava.smsadminlite.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * DTO: Data Transfer Object，数据传输对象。
 *
 * 它专门用来接收前端请求参数。
 * 这样做的好处是：请求参数结构可以独立演进，不必直接暴露内部模型。
 */
public class CreateUserRequest {

    @NotBlank(message = "用户名不能为空")
    private String name;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
    private String phone;

    public CreateUserRequest() {
    }

    public CreateUserRequest(String name, String phone) {
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

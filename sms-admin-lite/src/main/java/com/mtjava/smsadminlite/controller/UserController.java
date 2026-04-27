package com.mtjava.smsadminlite.controller;

import com.mtjava.smsadminlite.common.ApiResponse;
import com.mtjava.smsadminlite.dto.CreateUserRequest;
import com.mtjava.smsadminlite.dto.UpdateUserRequest;
import com.mtjava.smsadminlite.model.User;
import com.mtjava.smsadminlite.service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户接口层。
 *
 * Controller 的职责很像前端里的页面事件入口：
 * 接收请求、拿参数、调用业务层，然后把结果返回给客户端。
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ApiResponse<List<User>> listUsers() {
        log.info("收到用户列表查询请求");
        return ApiResponse.success(userService.listUsers());
    }

    @GetMapping("/{id}")
    public ApiResponse<User> getUserById(@PathVariable Long id) {
        log.info("收到用户详情查询请求，userId={}", id);
        return ApiResponse.success(userService.getUserById(id));
    }

    @PostMapping
    public ApiResponse<User> createUser(@Valid @RequestBody CreateUserRequest request) {
        log.info("收到创建用户请求，phone={}", request.getPhone());
        return ApiResponse.success("创建成功", userService.createUser(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<User> updateUser(@PathVariable Long id,
                                        @Valid @RequestBody UpdateUserRequest request) {
        log.info("收到更新用户请求，userId={}, phone={}", id, request.getPhone());
        return ApiResponse.success("更新成功", userService.updateUser(id, request));
    }
}

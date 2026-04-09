package com.mtjava.smsadminlite.service.impl;

import com.mtjava.smsadminlite.dto.CreateUserRequest;
import com.mtjava.smsadminlite.model.User;
import com.mtjava.smsadminlite.repository.UserRepository;
import com.mtjava.smsadminlite.service.UserService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 业务层实现。
 *
 * 这里负责真正的业务动作：
 * 1. 查询数据
 * 2. 执行业务校验
 * 3. 组装模型
 * 4. 调用 repository 持久化
 */
@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public List<User> listUsers() {
        return userRepository.findAll();
    }

    @Override
    public User createUser(CreateUserRequest request) {
        userRepository.findByPhone(request.getPhone()).ifPresent(user -> {
            throw new IllegalArgumentException("手机号已存在，不能重复创建");
        });

        User user = new User();
        user.setName(request.getName());
        user.setPhone(request.getPhone());
        user.setCreatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }
}

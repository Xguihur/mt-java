package com.mtjava.smsadminlite.service.impl;

import com.mtjava.smsadminlite.dto.CreateUserRequest;
import com.mtjava.smsadminlite.mapper.UserMapper;
import com.mtjava.smsadminlite.model.User;
import com.mtjava.smsadminlite.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户业务层实现。
 *
 * 之前依赖 InMemoryUserRepository，现在改为注入 UserMapper，
 * 数据真正写入 MySQL。其他逻辑保持不变。
 */
@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    public UserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public List<User> listUsers() {
        return userMapper.selectAll();
    }

    @Override
    @Transactional // 添加事务注解，确保数据一致性：一起成功或一起失败
    public User createUser(CreateUserRequest request) {
        // selectByPhone 找不到时 MyBatis 返回 null，不是 Optional，直接判空即可
        User existing = userMapper.selectByPhone(request.getPhone());
        if (existing != null) {
            throw new IllegalArgumentException("手机号已存在，不能重复创建");
        }

        User user = new User();
        user.setName(request.getName());
        user.setPhone(request.getPhone());
        user.setCreatedAt(LocalDateTime.now());

        // insert 执行后，user.id 会被 MyBatis 自动回填（useGeneratedKeys）
        userMapper.insert(user);
        return user;
    }
}

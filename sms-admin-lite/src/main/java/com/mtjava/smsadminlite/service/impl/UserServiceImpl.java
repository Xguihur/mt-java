package com.mtjava.smsadminlite.service.impl;

import com.mtjava.smsadminlite.dto.CreateUserRequest;
import com.mtjava.smsadminlite.dto.UpdateUserRequest;
import com.mtjava.smsadminlite.mapper.UserMapper;
import com.mtjava.smsadminlite.model.User;
import com.mtjava.smsadminlite.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;

/**
 * 用户业务层实现。
 *
 * 之前依赖 InMemoryUserRepository，现在改为注入 UserMapper，
 * 数据真正写入 MySQL。其他逻辑保持不变。
 */
@Service
public class UserServiceImpl implements UserService {

    private static final String USER_CACHE_KEY = "user:%d";
    private static final Duration USER_CACHE_TTL = Duration.ofMinutes(10);

    private final UserMapper userMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public UserServiceImpl(UserMapper userMapper,
                           StringRedisTemplate redisTemplate,
                           ObjectMapper objectMapper) {
        this.userMapper = userMapper;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<User> listUsers() {
        return userMapper.selectAll();
    }

    @Override
    public User getUserById(Long id) {
        String cacheKey = buildUserCacheKey(id);

        // 先查 Redis，命中后直接返回，避免高频详情查询持续打到 MySQL。
        String cachedUserJson = redisTemplate.opsForValue().get(cacheKey);
        if (cachedUserJson != null) {
            return deserializeUser(cachedUserJson, id);
        }

        User user = userMapper.selectById(id);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在，id=" + id);
        }

        // 旁路缓存：只有缓存未命中时才回源数据库，并把结果回填到 Redis。
        redisTemplate.opsForValue().set(cacheKey, serializeUser(user), USER_CACHE_TTL);
        return user;
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

    @Override
    @Transactional
    public User updateUser(Long id, UpdateUserRequest request) {
        User existing = userMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("用户不存在，id=" + id);
        }

        User duplicatePhoneUser = userMapper.selectByPhoneExcludingId(request.getPhone(), id);
        if (duplicatePhoneUser != null) {
            throw new IllegalArgumentException("手机号已存在，不能重复使用");
        }

        existing.setName(request.getName());
        existing.setPhone(request.getPhone());
        userMapper.update(existing);

        // 先更新数据库，再删除缓存。下次查询会自动回源数据库并重建最新缓存。
        redisTemplate.delete(buildUserCacheKey(id));
        return existing;
    }

    private String serializeUser(User user) {
        try {
            return objectMapper.writeValueAsString(user);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("用户缓存序列化失败，id=" + user.getId(), e);
        }
    }

    private User deserializeUser(String cachedUserJson, Long userId) {
        try {
            return objectMapper.readValue(cachedUserJson, User.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("用户缓存反序列化失败，id=" + userId, e);
        }
    }

    private String buildUserCacheKey(Long id) {
        return String.format(USER_CACHE_KEY, id);
    }
}

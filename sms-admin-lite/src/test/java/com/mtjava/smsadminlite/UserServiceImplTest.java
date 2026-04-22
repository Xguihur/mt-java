package com.mtjava.smsadminlite;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtjava.smsadminlite.common.BusinessException;
import com.mtjava.smsadminlite.dto.UpdateUserRequest;
import com.mtjava.smsadminlite.mapper.UserMapper;
import com.mtjava.smsadminlite.model.User;
import com.mtjava.smsadminlite.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userMapper, redisTemplate, objectMapper);
    }

    @Test
    void shouldReturnUserFromRedisCache() throws Exception {
        User cachedUser = new User(1L, "张三", "13800000000", LocalDateTime.of(2026, 4, 16, 10, 0));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("user:1")).thenReturn(objectMapper.writeValueAsString(cachedUser));

        User result = userService.getUserById(1L);

        assertEquals(1L, result.getId());
        assertEquals("张三", result.getName());
        verify(userMapper, never()).selectById(any());
    }

    @Test
    void shouldLoadUserFromDatabaseAndWriteBackToRedisWhenCacheMisses() {
        User dbUser = new User(2L, "李四", "13900000000", LocalDateTime.of(2026, 4, 16, 11, 0));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("user:2")).thenReturn(null);
        when(userMapper.selectById(2L)).thenReturn(dbUser);

        User result = userService.getUserById(2L);

        assertEquals(2L, result.getId());
        assertEquals("李四", result.getName());
        verify(userMapper).selectById(2L);
        verify(valueOperations).set(eq("user:2"), any(String.class), eq(Duration.ofMinutes(10)));
    }

    @Test
    void shouldDeleteUserCacheAfterDatabaseUpdate() {
        User existingUser = new User(3L, "旧名字", "13700000000", LocalDateTime.of(2026, 4, 16, 12, 0));
        UpdateUserRequest request = new UpdateUserRequest("新名字", "13600000000");
        when(userMapper.selectById(3L)).thenReturn(existingUser);
        when(userMapper.selectByPhoneExcludingId("13600000000", 3L)).thenReturn(null);

        User result = userService.updateUser(3L, request);

        assertEquals("新名字", result.getName());
        assertEquals("13600000000", result.getPhone());
        verify(userMapper).update(existingUser);
        verify(redisTemplate).delete("user:3");
    }

    @Test
    void shouldRejectDuplicatePhoneWhenUpdatingUser() {
        User existingUser = new User(4L, "王五", "13700000000", LocalDateTime.of(2026, 4, 16, 13, 0));
        UpdateUserRequest request = new UpdateUserRequest("王五", "13500000000");
        when(userMapper.selectById(4L)).thenReturn(existingUser);
        when(userMapper.selectByPhoneExcludingId("13500000000", 4L))
                .thenReturn(new User(8L, "别人", "13500000000", LocalDateTime.now()));

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> userService.updateUser(4L, request)
        );

        assertEquals("手机号已存在，不能重复使用", ex.getMessage());
        verify(userMapper, never()).update(any());
        verify(redisTemplate, never()).delete(anyString());
    }
}

package com.mtjava.smsadminlite.config;

import com.mtjava.smsadminlite.dto.CreateUserRequest;
import com.mtjava.smsadminlite.mapper.UserMapper;
import com.mtjava.smsadminlite.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 启动初始化配置。
 *
 * 仅在数据库里没有用户时才插入演示数据，重启不会重复写入报错。
 */
@Slf4j
@Configuration
public class DataInitializer {

    @Bean
    @ConditionalOnProperty(prefix = "app.demo-data", name = "enabled", havingValue = "true")
    public CommandLineRunner loadDemoUsers(UserService userService, UserMapper userMapper) {
        return args -> {
            long userCount = userMapper.count();
            if (userCount == 0) {
                log.info("检测到用户表为空，开始写入演示用户");
                userService.createUser(new CreateUserRequest("张三", "13800138000"));
                userService.createUser(new CreateUserRequest("李四", "13900139000"));
                log.info("演示用户初始化完成");
            } else {
                log.info("跳过演示用户初始化，当前用户数量={}", userCount);
            }
        };
    }
}

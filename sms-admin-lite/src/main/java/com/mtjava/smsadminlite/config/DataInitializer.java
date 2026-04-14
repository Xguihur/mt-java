package com.mtjava.smsadminlite.config;

import com.mtjava.smsadminlite.dto.CreateUserRequest;
import com.mtjava.smsadminlite.mapper.UserMapper;
import com.mtjava.smsadminlite.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 启动初始化配置。
 *
 * 仅在数据库里没有用户时才插入演示数据，重启不会重复写入报错。
 */
@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner loadDemoUsers(UserService userService, UserMapper userMapper) {
        return args -> {
            if (userMapper.count() == 0) {
                userService.createUser(new CreateUserRequest("张三", "13800138000"));
                userService.createUser(new CreateUserRequest("李四", "13900139000"));
            }
        };
    }
}

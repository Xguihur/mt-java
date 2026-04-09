package com.mtjava.smsadminlite.config;

import com.mtjava.smsadminlite.dto.CreateUserRequest;
import com.mtjava.smsadminlite.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 启动初始化配置。
 *
 * 真实项目里，你会看到一些“系统启动后自动执行”的逻辑，
 * 比如加载缓存、发送通知、检查配置等。
 * 这里简化成：启动时插入两条演示数据，方便你马上调接口。
 */
@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner loadDemoUsers(UserService userService) {
        return args -> {
            userService.createUser(new CreateUserRequest("张三", "13800138000"));
            userService.createUser(new CreateUserRequest("李四", "13900139000"));
        };
    }
}

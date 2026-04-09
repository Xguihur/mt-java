package com.mtjava.smsadminlite;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 项目启动类。
 *
 * 可以把它理解成前端项目里的 main.tsx / main.js：
 * 应用从这里开始启动，Spring Boot 会自动扫描当前包及其子包里的组件。
 */
@SpringBootApplication
public class SmsAdminLiteApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmsAdminLiteApplication.class, args);
    }
}

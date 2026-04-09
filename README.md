# mt-java

这是一个给前端开发者学习 Spring Boot 的“瘦身版 sms-parent”。

它参考了 `/Users/xiaolongxia/Desktop/shuoruan/project/sms/sms-parent` 的父子模块思路，并重点模拟了 `sms-admin` 常见的后端分层方式，但做了几轮剪枝：

- 保留 Maven 父子模块结构，帮助你熟悉真正项目的组织方式。
- 保留 `controller -> service -> repository` 的调用链，帮助你理解请求是怎么一层层流转的。
- 去掉数据库、MyBatis、RabbitMQ、Redis、OSS、动态数据源、邮件、定时任务等复杂依赖，先把 Spring Boot 主线打通。
- 用“内存仓库”代替数据库，便于你专注在 Java 语法、注解、分层和接口设计上。

## 项目结构

```text
mt-java
├── pom.xml                  # 父项目：统一版本和模块管理
└── sms-admin-lite           # 子模块：模拟 sms-admin 的一个最小可运行版本
    ├── pom.xml
    └── src
        ├── main
        │   ├── java/com/mtjava/smsadminlite
        │   │   ├── SmsAdminLiteApplication.java
        │   │   ├── common      # 通用返回体、全局异常处理
        │   │   ├── config      # 启动初始化配置
        │   │   ├── controller  # 接口层，负责接收 HTTP 请求
        │   │   ├── dto         # 请求参数对象
        │   │   ├── model       # 领域模型
        │   │   ├── repository  # 数据访问层的练习版，用内存代替数据库
        │   │   └── service     # 业务层，编排业务逻辑
        │   └── resources
        │       └── application.yml
        └── test
```

## 每层在真实 sms-admin 里的对应关系

- `controller`
  你以后在 `sms-admin` 里会看到很多 `xxxController`，它们负责暴露接口。
- `service`
  对应真实项目里的业务服务层，负责“处理规则”，而不是直接写接口细节。
- `repository`
  这里只是教学用名字。在真实项目里，这一层通常会换成 `mapper` 或 `dao`，再通过 MyBatis/MyBatis-Plus 连数据库。
- `common`
  对应真实项目里统一响应、异常处理、基础工具类这类公共内容。
- `config`
  对应启动初始化、拦截器、配置类等内容。

## 为什么只保留这些依赖

`sms-admin` 真实依赖非常多，但学习顺序建议这样走：

1. 先学会 Spring Boot 项目怎么启动。
2. 再学会 Controller、Service、Repository 的分层。
3. 再学参数校验、统一响应、异常处理。
4. 最后再接数据库、MyBatis、Redis、MQ。

所以这里先只保留：

- `spring-boot-starter-web`
  用来写最基本的 Web 接口。
- `spring-boot-starter-validation`
  用来做参数校验，比如用户名不能为空。
- `spring-boot-starter-test`
  用来写最基础的接口测试。

## 运行方式

在项目根目录执行：

```bash
mvn -pl sms-admin-lite spring-boot:run
```

启动后访问：

- `GET http://localhost:8080/api/health`
- `GET http://localhost:8080/api/users`
- `POST http://localhost:8080/api/users`

POST 示例：

```json
{
  "name": "Alice",
  "phone": "13800138000"
}
```

## 建议你的学习顺序

1. 先看 `SmsAdminLiteApplication`，理解 Spring Boot 如何启动。
2. 再看 `UserController`，理解接口如何接收请求。
3. 再看 `UserService` 和 `InMemoryUserRepository`，理解分层。
4. 再看 `ApiResponse` 和 `GlobalExceptionHandler`，理解统一返回和异常处理。
5. 最后自己尝试加一个“删除用户”接口。

## 下一步怎么逐渐靠近真实 sms-admin

你学完这一版后，可以按这个顺序升级：

1. 给 `repository` 换成 MyBatis `mapper`
2. 接入 MySQL
3. 增加 `entity / vo / dto` 的区分
4. 增加拦截器和登录校验
5. 再接 Redis、MQ、文件上传这些外围能力

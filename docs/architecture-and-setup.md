# sms-admin-lite 技术架构与使用说明

## 目录

1. [项目整体架构](#1-项目整体架构)
2. [分层职责说明](#2-分层职责说明)
3. [技术栈引入方式](#3-技术栈引入方式)
4. [MySQL 数据库](#4-mysql-数据库)
5. [Redis 配置](#5-redis-配置)
6. [抢红包功能说明](#6-抢红包功能说明)
7. [接口一览](#7-接口一览)
8. [启动前准备与配置](#8-启动前准备与配置)

---

## 1. 项目整体架构

```
HTTP 请求
    │
    ▼
Controller          接收参数、调用 Service、返回 ApiResponse<T>
    │
    ▼
Service             业务规则（校验、组装、事务）
    │
    ├─── Mapper     通过 MyBatis 把方法调用转为 SQL，操作 MySQL
    │
    └─── Redis      通过 StringRedisTemplate 做原子操作（防超发）
```

**模块说明：**

| 层 | 包 | 作用 |
|---|---|---|
| 接口层 | `controller/` | 路由、参数接收/校验、返回统一格式 |
| 业务层 | `service/` | 业务规则、事务控制 |
| 数据访问层 | `mapper/` | MyBatis Mapper 接口，对应 SQL 写在 XML 里 |
| 模型层 | `model/` | 纯 POJO，无框架注解 |
| 数据传输层 | `dto/` | 接收请求参数专用，与 model 解耦 |
| 公共组件 | `common/` | 统一返回体 `ApiResponse`、全局异常处理器 |

---

## 2. 分层职责说明

### Controller

只负责三件事：**接收参数 → 调用 Service → 包装返回**。

```java
@PostMapping
public ApiResponse<User> createUser(@Valid @RequestBody CreateUserRequest request) {
    return ApiResponse.success("创建成功", userService.createUser(request));
}
```

`@Valid` 触发 `CreateUserRequest` 上声明的字段校验（如 `@NotBlank`、`@Pattern`）。
校验失败由 `GlobalExceptionHandler` 统一捕获并返回错误信息，不会在 Controller 里出现 try-catch。

### Service

业务规则都在这里。以创建用户为例：

```java
User existing = userMapper.selectByPhone(request.getPhone());
if (existing != null) {
    throw new IllegalArgumentException("手机号已存在，不能重复创建");
}
userMapper.insert(user);
```

`@Transactional` 加在 Service 方法上，保证同一个方法里的多条 SQL 要么全成功、要么全回滚。

### Mapper（MyBatis）

Mapper 是一个 **接口**，你在接口里声明方法名和参数，具体 SQL 写在对应的 XML 文件里。  
MyBatis 在启动时自动生成这个接口的实现类，并注册为 Spring Bean，可以直接注入使用。

```
UserMapper.java          ← 接口：声明方法签名
resources/mapper/
  UserMapper.xml         ← XML：写对应 SQL
```

XML 里通过 `namespace` 和方法名与接口绑定：

```xml
<mapper namespace="com.mtjava.smsadminlite.mapper.UserMapper">
    <select id="selectAll" resultType="com.mtjava.smsadminlite.model.User">
        SELECT id, name, phone, created_at FROM users ORDER BY id
    </select>
</mapper>
```

---

## 3. 技术栈引入方式

所有依赖通过 `sms-admin-lite/pom.xml` 声明，Maven 负责下载和版本管理。

```xml
<!-- MyBatis：SQL 映射框架 -->
<dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter</artifactId>
    <version>3.0.3</version>
</dependency>

<!-- MySQL 驱动 -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>  <!-- 只在运行时需要，不参与编译 -->
</dependency>

<!-- Redis 客户端（Lettuce） -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>

<!-- 测试用内存数据库，不需要本地装 MySQL 也能跑单测 -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
```

> 版本号大多不需要手写，因为根 `pom.xml` 继承了 `spring-boot-starter-parent`，  
> 它内置了与当前 Spring Boot 版本兼容的所有依赖版本表。

---

## 4. MySQL 数据库

### 4.1 连接配置

`src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mt_java?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8
    username: root
    password: your_password       # 改成你本地 MySQL 密码
    driver-class-name: com.mysql.cj.jdbc.Driver
```

### 4.2 自动建表原理

项目使用 **Spring Boot 的 SQL 初始化机制**（而非 JPA 的 `ddl-auto`）来建表：

```yaml
spring:
  sql:
    init:
      mode: always   # 每次启动都执行 schema.sql
```

启动时，Spring Boot 会自动找到 `src/main/resources/schema.sql` 并执行。  
建表语句使用 `IF NOT EXISTS`，所以无论执行多少次都是幂等的（不会报错，也不会清数据）：

```sql
CREATE TABLE IF NOT EXISTS users (
    id         BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    phone      VARCHAR(20)  NOT NULL,
    created_at DATETIME     NOT NULL,
    UNIQUE KEY uk_phone (phone)
);
```

### 4.3 MyBatis 驼峰映射

数据库列名用下划线（`created_at`），Java 字段用驼峰（`createdAt`）。  
在 `application.yml` 里开启一个配置，MyBatis 会自动完成转换，不需要在 XML 里手动写 `resultMap`：

```yaml
mybatis:
  configuration:
    map-underscore-to-camel-case: true
```

### 4.4 插入后自动回填主键

执行 insert 后，数据库生成的自增 ID 会自动写回 Java 对象的 `id` 字段，依靠 XML 里两个属性实现：

```xml
<insert id="insert" useGeneratedKeys="true" keyProperty="id">
    INSERT INTO users (name, phone, created_at)
    VALUES (#{name}, #{phone}, #{createdAt})
</insert>
```

- `useGeneratedKeys="true"` — 告诉 MyBatis 使用数据库自增主键
- `keyProperty="id"` — 把生成的主键值回填到传入对象的 `id` 字段

---

## 5. Redis 配置

### 5.1 连接配置

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      # password: your_redis_password  # 有密码时取消注释
      timeout: 3000ms
```

### 5.2 使用方式

项目直接注入 Spring Boot 自动配置好的 `StringRedisTemplate`，无需额外配置类：

```java
private final StringRedisTemplate redisTemplate;

// 写入
redisTemplate.opsForList().rightPushAll(key, "100", "200", "50");

// 原子弹出
String amount = redisTemplate.opsForList().leftPop(key);

// 集合写入（防重复）
Long added = redisTemplate.opsForSet().add(key, userId);
```

`StringRedisTemplate` 的所有 key 和 value 都是字符串，与 Redis 命令行里看到的内容完全一致，方便调试。

---

## 6. 抢红包功能说明

### 6.1 为什么要用 Redis

抢红包的核心难点是**高并发下防超发**（不能多发）。  
如果只靠 MySQL，多个请求同时判断"还有余量"再扣减，会因为并发读到相同旧值导致超发。  
Redis 的单线程模型保证了 `LPOP`（弹出列表元素）是原子操作，天然解决这个问题。

### 6.2 创建红包流程

```
1. 二倍均值法把总金额拆成 N 份随机金额
2. 写入 MySQL（持久化红包信息）
3. 把 N 个金额依次推入 Redis List（rp:{id}:amounts）
```

**二倍均值法**：每次从剩余金额里随机取 `[1, 剩余均值×2]` 范围内的值，  
保证后面每个人至少还能拿到 1 分，分布相对均匀且不会出现 0 元红包。

### 6.3 抢红包流程（防超发关键路径）

```
1. Redis Lua 脚本（一次执行完成）
   - `SADD rp:{id}:grabbed userId`
   - 如果已抢过，直接返回 `DUPLICATE`
   - 否则继续 `LPOP rp:{id}:amounts`
   - 如果已抢完，脚本内 `SREM` 回滚资格并返回 `EMPTY`
   - 如果成功，直接返回金额字符串

3. 写抢包记录到 MySQL

4. MySQL 原子更新剩余数量（展示用）：
   UPDATE red_packet SET remain_count = remain_count - 1, ... WHERE id = ?
```

Lua 脚本会在 Redis 内部一次性执行完，  
所以“是否抢过 + 是否还有金额 + 抢到哪一份”是同一个原子过程。  
即使 1000 个请求同时抢 10 个红包，也只有 10 个人能成功。

---

## 7. 接口一览

### 用户

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/users` | 查询用户列表 |
| POST | `/api/users` | 创建用户 |
| GET | `/api/health` | 服务健康检查 |

**创建用户请求体：**
```json
{
  "name": "张三",
  "phone": "13800138001"
}
```

### 红包

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/red-packets` | 创建红包 |
| GET | `/api/red-packets/{id}` | 查询红包详情（含剩余数量） |
| POST | `/api/red-packets/{id}/grab?userId=1` | 抢红包 |
| GET | `/api/red-packets/{id}/records` | 查看抢包记录 |

**创建红包请求体：**
```json
{
  "title": "周末红包",
  "totalAmountCents": 1000,
  "totalCount": 5
}
```
> `totalAmountCents` 单位是**分**，1000 = 10 元，最低每个红包 1 分。

**所有接口返回统一格式：**
```json
{
  "code": 0,
  "message": "success",
  "data": { ... }
}
```
`code = 0` 成功，`code = -1` 失败（`data` 为 null，`message` 为错误说明）。

---

## 8. 启动前准备与配置

### 第一步：建数据库

```sql
CREATE DATABASE mt_java CHARACTER SET utf8mb4;
```

> 建表由 `schema.sql` 在应用启动时自动完成，这里只需要建库。

### 第二步：修改配置

打开 `src/main/resources/application.yml`，修改以下两处：

```yaml
spring:
  datasource:
    password: your_password   # 改成你本地 MySQL 密码
```

如果 Redis 有密码，取消这行注释：

```yaml
  data:
    redis:
      password: your_redis_password
```

### 第三步：启动

```bash
cd sms-admin-lite
mvn spring-boot:run
```

启动成功后访问 `http://localhost:8080/api/health`，返回 `{"code":0,"data":{"status":"UP"}}` 即正常。

### 测试说明

跑单测不需要启动 MySQL，使用 H2 内存数据库代替：

```bash
mvn test
```

H2 是内嵌数据库，Spring Boot 检测到后会自动执行 `schema.sql` 完成建表，无需额外配置。

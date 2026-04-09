# 问题记录：为什么 `UserController` 没有 `new UserServiceImpl` 也能调用实现方法？

## 你的问题

在 `UserController` 里看到的是：

- 成员变量类型是 `UserService`（接口）
- 构造器参数也是 `UserService`

你的疑问是：  
代码里没有显式 `new UserServiceImpl()`，为什么最终执行的却是 `UserServiceImpl` 里的方法？也没有看到哪里手动指定“用这个实现类”。

## 回复答案

这是 **Spring IoC/DI（控制反转 + 依赖注入）** 在工作，不是你手动 `new`。

###### 核心过程如下：

1. `UserServiceImpl` 上有 `@Service` 注解，Spring 启动时会扫描到它，并把它注册成一个 Bean。  
2. `UserServiceImpl implements UserService`，所以它是 `UserService` 接口的一个实现。  
3. `UserController` 构造器需要 `UserService` 参数时，Spring 容器会自动查找能匹配 `UserService` 类型的 Bean。  
4. 当前项目中这个接口只有一个实现（`UserServiceImpl`），于是 Spring 就把它注入进来。  
5. 因此你在控制器里调用 `userService.createUser(...)` 时，实际执行的是 `UserServiceImpl` 中 `@Override` 的实现方法。

## 关键点补充

- 这不是“重载”（overload），而是“实现接口并重写方法”（override）。
- 你看到“只写接口类型就能调用实现”，是面向接口编程 + 依赖注入的典型写法。
- 这样做的好处是控制器不依赖具体实现，后续替换实现类或做单元测试都更方便。

## 对应到当前代码

- `UserController`：构造器注入 `UserService`
- `UserService`：定义业务接口
- `UserServiceImpl`：`@Service` + `implements UserService`，被 Spring 自动注入

如果后面你再新增一个实现类（例如 `UserServiceMockImpl`），Spring 会因为同类型 Bean 有多个而提示冲突，这时通常会用 `@Primary` 或 `@Qualifier` 指定注入哪一个。

## 你追问的假设：有多个实现类时，要不要在 `UserController` 声明？

结论：**要么在实现类上声明默认优先级，要么在 `UserController` 的构造器参数上明确指定**。

### 方式一：在实现类上用 `@Primary`（全局默认）

适合场景：大多数地方都希望注入同一个默认实现。

```java
@Service
@Primary
public class UserServiceImpl implements UserService {
    // ...
}
```

这样后，`UserController` 保持原样即可（不需要额外声明 `@Qualifier`）。

### 方式二：在 `UserController` 上用 `@Qualifier`（按点指定）

适合场景：不同地方可能要注入不同实现，按注入点精确控制。

先给实现类命名：

```java
@Service("dbUserService")
public class UserServiceImpl implements UserService {
    // ...
}

@Service("mockUserService")
public class UserServiceMockImpl implements UserService {
    // ...
}
```

然后在 `UserController` 构造器参数上指定：

```java
public UserController(@Qualifier("dbUserService") UserService userService) {
    this.userService = userService;
}
```

### 小结

- 你说的“是不是要在 `UserController` 某个地方声明”是对的。  
- 当你选择 `@Qualifier` 方案时，声明位置通常就是 **构造器参数**。  
- 如果你使用 `@Primary`，`UserController` 可以不改，Spring 会自动选默认实现。

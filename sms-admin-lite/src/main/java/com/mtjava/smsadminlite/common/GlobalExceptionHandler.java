package com.mtjava.smsadminlite.common;

// jakarta.validation.* 来自 Jakarta Bean Validation 规范。
// 当你在参数上使用 @NotBlank、@Min、@Valid 这类校验注解时，
// 校验失败后，框架可能抛出这里相关的异常。
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

// HttpMessageNotReadableException 来自 Spring Web。
// 当前端传来的 JSON 格式错误、字段类型不匹配、请求体无法反序列化时，
// Spring 在读取请求体阶段会抛出这个异常。
import org.springframework.http.converter.HttpMessageNotReadableException;

// BindException 来自 Spring Validation / Data Binding。
// 当 Spring 把请求参数绑定到对象时，如果绑定失败，可能抛出这个异常。
import org.springframework.validation.BindException;

// MethodArgumentNotValidException 来自 Spring Web。
// 常见于 @RequestBody + @Valid 的场景：请求体能成功转成对象，
// 但对象内部字段校验没通过，就会抛出它。
import org.springframework.web.bind.MethodArgumentNotValidException;

// @ExceptionHandler 用来声明“某个方法专门处理哪一类异常”。
import org.springframework.web.bind.annotation.ExceptionHandler;

// @RestControllerAdvice 是 Spring 提供的“全局异常处理 + 返回 JSON”组合注解。
// 它可以让这个类对整个项目的 Controller 生效，而不是只作用于某一个接口类。
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器。
 *
 * 它相当于给整个项目兜底：
 * 一旦 Controller 或 Service 抛异常，会统一在这里转换成前端更容易消费的响应格式。
 *
 * 你可以把它理解成：
 * 1. 业务代码里正常写 throw new xxxException(...)
 * 2. 异常一路往外抛到 Spring MVC
 * 3. Spring 发现这里有匹配的 @ExceptionHandler 方法
 * 4. 把异常转换成统一的 ApiResponse 返回给前端
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理 @RequestBody + @Valid 触发的参数校验异常。
     *
     * 例如：
     * public ApiResponse<?> create(@Valid @RequestBody CreateUserRequest request)
     *
     * 如果 request 里的某个字段不满足 @NotBlank、@Size 等规则，
     * Spring 会抛出 MethodArgumentNotValidException，然后进入这里。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
        // getBindingResult() 里保存了这次校验失败的详细信息。
        // getFieldError() 取“第一个字段错误”，方便快速返回给前端。
        // 这里做了一个判空，避免没有具体字段错误时出现空指针。
        String message = exception.getBindingResult().getFieldError() == null
                // 如果拿不到具体字段错误，就给一个通用提示。
                ? "请求参数校验失败"
                // 如果拿到了字段错误，就把注解里定义的错误消息返回给前端。
                // 比如 @NotBlank(message = "用户名不能为空")
                : exception.getBindingResult().getFieldError().getDefaultMessage();

        // ApiResponse<Void> 表示这是一个统一返回体，但 data 部分没有实际数据。
        // fail(message) 会构造 code = -1、message = 错误信息、data = null 的响应。
        return buildErrorResponse(HttpStatus.BAD_REQUEST, message);
    }

    /**
     * 处理参数绑定异常。
     *
     * 它更常见于 @ModelAttribute、表单参数、URL 查询参数、路径参数绑定失败的场景。
     * 比如本来要求 Long 类型，但传了无法转换的字符串。
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Void>> handleBindException(BindException exception) {
        // 和上面类似，也是从绑定结果中提取第一个错误信息。
        String message = exception.getBindingResult().getFieldError() == null
                ? "参数绑定失败"
                : exception.getBindingResult().getFieldError().getDefaultMessage();
        return buildErrorResponse(HttpStatus.BAD_REQUEST, message);
    }

    /**
     * 处理约束校验异常。
     *
     * 这个异常常出现在直接校验方法参数的场景，例如：
     * public ApiResponse<?> get(@Min(1) Long id)
     *
     * 当参数违反约束时，会抛出 ConstraintViolationException。
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException exception) {
        // 这里直接把异常信息原样返回。
        // 它通常会包含是哪一个参数、违反了什么规则。
        return buildErrorResponse(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    /**
     * 处理“请求体读不出来”的异常。
     *
     * 常见原因：
     * 1. JSON 语法错误
     * 2. 少了引号、逗号
     * 3. 传了错误的数据类型，导致无法反序列化
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException exception) {
        // 这里没有直接暴露底层异常细节，而是返回一个更友好的提示。
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "请求体格式不正确，请检查 JSON 是否合法");
    }

    /**
     * 处理代码里主动抛出的业务异常。
     *
     * 这一类通常表示“请求参数合法，但不满足业务规则”，例如：
     * - 用户不存在
     * - 红包已抢完
     * - 已经抢过红包
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
        return buildErrorResponse(exception.getStatus(), exception.getMessage());
    }

    /**
     * 兜底异常处理。
     *
     * 只要前面的处理器都没匹配上，最终就会进入这里。
     * 例如 IllegalStateException、NullPointerException、SQLException 等，
     * 都可能被这个方法统一接住。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception exception) {
        log.error("Unhandled exception caught by global handler", exception);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "服务器内部异常，请稍后再试");
    }

    private ResponseEntity<ApiResponse<Void>> buildErrorResponse(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ApiResponse.fail(message));
    }
}

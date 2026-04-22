package com.mtjava.smsadminlite.common;

import org.springframework.http.HttpStatus;

/**
 * 业务异常。
 *
 * 用于表达“请求本身能被系统理解，但不满足业务规则”的失败场景，
 * 例如资源不存在、数据重复、业务冲突等。
 */
public class BusinessException extends RuntimeException {

    private final HttpStatus status;

    public BusinessException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public static BusinessException badRequest(String message) {
        return new BusinessException(HttpStatus.BAD_REQUEST, message);
    }

    public static BusinessException notFound(String message) {
        return new BusinessException(HttpStatus.NOT_FOUND, message);
    }

    public static BusinessException conflict(String message) {
        return new BusinessException(HttpStatus.CONFLICT, message);
    }
}

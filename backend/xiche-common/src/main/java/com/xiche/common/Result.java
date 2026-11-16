package com.xiche.common;

import java.io.Serializable;

/**
 * 四端统一响应结构：{ code, message, data }
 * 业务错误码分段：0 成功；40000~49999 客户端/业务问题；50000~59999 服务端问题。
 * 禁止用 200 包一切业务错误。
 *
 * @param <T> 业务数据类型
 */
public class Result<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final int CODE_SUCCESS = 0;
    public static final int CODE_BIZ_ERROR = 40000;
    public static final int CODE_SYSTEM_ERROR = 50000;

    private int code;
    private String message;
    private T data;

    public Result() {
    }

    public Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> success() {
        return new Result<T>(CODE_SUCCESS, "ok", null);
    }

    public static <T> Result<T> success(T data) {
        return new Result<T>(CODE_SUCCESS, "ok", data);
    }

    public static <T> Result<T> success(T data, String message) {
        return new Result<T>(CODE_SUCCESS, message, data);
    }

    public static <T> Result<T> fail(int code, String message) {
        return new Result<T>(code, message, null);
    }

    public static <T> Result<T> fail(BizException e) {
        return new Result<T>(e.getCode(), e.getMessage(), null);
    }

    public boolean isSuccess() {
        return this.code == CODE_SUCCESS;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}

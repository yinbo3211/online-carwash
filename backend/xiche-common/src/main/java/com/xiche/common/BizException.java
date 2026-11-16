package com.xiche.common;

/**
 * 业务异常：由全局异常处理器转换为统一 Result，不向上抛栈信息给前端。
 */
public class BizException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final int code;

    public BizException(String message) {
        this(Result.CODE_BIZ_ERROR, message);
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BizException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}

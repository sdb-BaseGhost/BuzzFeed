package org.sdb.buzzfeed.entity;

import lombok.Data;

/**
 * 后端统一返回结果
 * code: 200=成功, 其他=失败
 */
@Data
public class Result {

    private Integer code;
    private String msg;
    private Object data;

    public static final int SUCCESS_CODE = 200;
    public static final int ERROR_CODE = -1;

    public static Result success() {
        Result result = new Result();
        result.code = SUCCESS_CODE;
        result.msg = "success";
        return result;
    }

    public static Result success(Object data) {
        Result result = new Result();
        result.code = SUCCESS_CODE;
        result.msg = "success";
        result.data = data;
        return result;
    }

    public static Result success(String msg, Object data) {
        Result result = new Result();
        result.code = SUCCESS_CODE;
        result.msg = msg;
        result.data = data;
        return result;
    }

    public static Result error(String msg) {
        Result result = new Result();
        result.code = ERROR_CODE;
        result.msg = msg;
        return result;
    }

    public static Result error(int code, String msg) {
        Result result = new Result();
        result.code = code;
        result.msg = msg;
        return result;
    }

}

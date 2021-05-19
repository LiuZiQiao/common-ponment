package com.lxk.project.common.po;

import java.io.Serializable;

/**
 * @Author macos·lxk
 * @create 2021/5/19 上午11:27
 */

public class ResultWrapper<T> implements Serializable {

    private static final long serialVersionUID = 4951985403398018181L;

    private static final int SUCCESS_CODE = 200;

    private static final String SUCCESS_MESSAGE = "SUCCESS";

    private static final int ERROR_CODE = 200;
    private static final String ERROR_MESSAGE = "ERROR";

    private Integer code;
    private String message;
    private T result;

    public static <E> ResultWrapper<E> success(){
        return new ResultWrapper(SUCCESS_CODE,SUCCESS_MESSAGE);
    }

    public static  <E> ResultWrapper<E> success(E result){
        return new ResultWrapper(SUCCESS_CODE,SUCCESS_MESSAGE,result);
    }

    public static <E> ResultWrapper<E> error(String message){
        return new ResultWrapper(ERROR_CODE,message);
    }

    public ResultWrapper() {
        this(SUCCESS_CODE,SUCCESS_MESSAGE);
    }


    public ResultWrapper(Integer code, String message, T result) {
        super();
        this.code(code).message(message).result(result);
    }

    public ResultWrapper(Integer code, String message) {
        this(code,message,null);
    }


    private ResultWrapper<T> code(Integer code) {
        this.code(code);
        return this;
    }

    private ResultWrapper<T> message(String message) {
        this.message(message);
        return this;
    }

    private ResultWrapper<T> result(T result) {
        this.result(result);
        return this;
    }



    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getResult() {
        return result;
    }

    public void setResult(T result) {
        this.result = result;
    }
}

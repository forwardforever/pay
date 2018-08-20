package com.bw.movie.bean;

/**
 * 1. 类的用途
 * 2. @author forever
 * 3. @date 2018/8/17 14:24
 */


public class UserRegister {

    /**
     * message : 注册成功
     * status : 0000
     */

    private String message;
    private String status;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

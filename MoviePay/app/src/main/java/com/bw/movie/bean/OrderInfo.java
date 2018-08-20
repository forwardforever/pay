package com.bw.movie.bean;

/**
 * 1. 类的用途
 * 2. @author forever
 * 3. @date 2018/8/7 15:00
 */


public class OrderInfo {
    private String orderId;
    private String message;
    private String status;

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

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

package com.example.silkmall.dto;

public class ResponseDTO<T> {
    private int code;
    private String message;
    private T data;
    private long timestamp;
    
    public ResponseDTO() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public ResponseDTO(int code, String message) {
        this.code = code;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }
    
    public ResponseDTO(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
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
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public static <T> ResponseDTO<T> success() {
        return new ResponseDTO<>(200, "操作成功");
    }
    
    public static <T> ResponseDTO<T> success(T data) {
        return new ResponseDTO<>(200, "操作成功", data);
    }
    
    public static <T> ResponseDTO<T> error(int code, String message) {
        return new ResponseDTO<>(code, message);
    }
    
    public static <T> ResponseDTO<T> error(String message) {
        return new ResponseDTO<>(500, message);
    }
}
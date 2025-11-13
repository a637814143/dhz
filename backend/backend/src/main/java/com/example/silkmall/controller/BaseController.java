package com.example.silkmall.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public abstract class BaseController {
    // 成功响应
    protected <T> ResponseEntity<T> success(T data) {
        return new ResponseEntity<>(data, HttpStatus.OK);
    }
    
    // 成功响应无数据
    protected ResponseEntity<Void> success() {
        return new ResponseEntity<>(HttpStatus.OK);
    }
    
    // 创建成功响应
    protected <T> ResponseEntity<T> created(T data) {
        return new ResponseEntity<>(data, HttpStatus.CREATED);
    }
    
    // 处理异常响应
    protected ResponseEntity<String> error(String message, HttpStatus status) {
        return new ResponseEntity<>(message, status);
    }
    
    // 通用的异常响应
    protected ResponseEntity<String> badRequest(String message) {
        return error(message, HttpStatus.BAD_REQUEST);
    }
    
    protected ResponseEntity<String> notFound(String message) {
        return error(message, HttpStatus.NOT_FOUND);
    }
    
    protected ResponseEntity<String> internalServerError(String message) {
        return error(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
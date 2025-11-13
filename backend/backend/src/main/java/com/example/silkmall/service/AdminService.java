package com.example.silkmall.service;

import com.example.silkmall.entity.Admin;

public interface AdminService extends UserService<Admin> {
    boolean hasPermission(Long adminId, String permission);
    void updatePermissions(Long id, String permissions);
}
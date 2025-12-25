package com.example.silkmall.dto;

import lombok.Data;

@Data
public class AdminProfileDTO {
    private Long id;
    private String username;
    private String email;
    private String phone;
    private String address;
    private String department;
    private String position;
}

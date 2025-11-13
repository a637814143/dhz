package com.example.silkmall.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ConsumerDTO {
    private Long id;

    @Size(max = 50, message = "用户名长度不能超过50个字符")
    private String username;

    @Email(message = "邮箱格式不正确")
    private String email;
    private String phone;
    private String address;
    
    @NotBlank(message = "真实姓名不能为空")
    private String realName;
    
    @NotBlank(message = "身份证号不能为空")
    private String idCard;
    
    private String avatar;
    private Integer points;
    private Integer membershipLevel;
    private Boolean enabled;
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    public String getRealName() {
        return realName;
    }
    
    public void setRealName(String realName) {
        this.realName = realName;
    }
    
    public String getIdCard() {
        return idCard;
    }
    
    public void setIdCard(String idCard) {
        this.idCard = idCard;
    }
    
    public String getAvatar() {
        return avatar;
    }
    
    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }
    
    public Integer getPoints() {
        return points;
    }
    
    public void setPoints(Integer points) {
        this.points = points;
    }
    
    public Integer getMembershipLevel() {
        return membershipLevel;
    }
    
    public void setMembershipLevel(Integer membershipLevel) {
        this.membershipLevel = membershipLevel;
    }
    
    public Boolean getEnabled() {
        return enabled;
    }
    
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
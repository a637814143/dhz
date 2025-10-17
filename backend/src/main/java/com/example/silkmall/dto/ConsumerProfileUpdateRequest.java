package com.example.silkmall.dto;

/**
 * 用于消费者自助更新基础资料的请求对象。
 *
 * 所有字段均为可选，后端会基于提交的非空值进行合并更新，
 * 避免覆盖未填写的现有数据。
 */
public class ConsumerProfileUpdateRequest {
    private String email;
    private String phone;
    private String address;
    private String realName;
    private String idCard;
    private String avatar;

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
}

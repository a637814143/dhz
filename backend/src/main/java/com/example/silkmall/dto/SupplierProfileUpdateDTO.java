package com.example.silkmall.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * 用于供应商用户更新基础资料的请求载体。
 */
public class SupplierProfileUpdateDTO {
    @Size(max = 100, message = "企业名称长度不能超过100个字符")
    private String companyName;

    @Size(max = 60, message = "联系人姓名长度不能超过60个字符")
    private String contactPerson;

    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过100个字符")
    private String email;

    @Size(max = 30, message = "联系电话长度不能超过30个字符")
    private String phone;

    @Size(max = 200, message = "联系地址长度不能超过200个字符")
    private String address;

    @Size(max = 120, message = "营业执照编号长度不能超过120个字符")
    private String businessLicense;

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
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

    public String getBusinessLicense() {
        return businessLicense;
    }

    public void setBusinessLicense(String businessLicense) {
        this.businessLicense = businessLicense;
    }
}

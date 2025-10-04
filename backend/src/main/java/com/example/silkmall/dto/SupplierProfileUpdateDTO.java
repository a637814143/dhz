package com.example.silkmall.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SupplierProfileUpdateDTO {
    @NotBlank(message = "企业名称不能为空")
    @Size(max = 120, message = "企业名称长度不能超过120个字符")
    private String companyName;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "联系电话不能为空")
    @Size(max = 30, message = "联系电话长度不能超过30个字符")
    private String phone;

    @Size(max = 60, message = "联系人长度不能超过60个字符")
    private String contactPerson;

    @Size(max = 120, message = "营业执照信息长度不能超过120个字符")
    private String businessLicense;

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
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

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public String getBusinessLicense() {
        return businessLicense;
    }

    public void setBusinessLicense(String businessLicense) {
        this.businessLicense = businessLicense;
    }
}

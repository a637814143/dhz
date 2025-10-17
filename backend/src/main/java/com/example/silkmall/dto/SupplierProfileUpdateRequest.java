package com.example.silkmall.dto;

/**
 * 用于供应商自助更新基础资料的请求对象。
 *
 * 字段全部为可选，控制器会对非空值进行规范化并写回数据库，
 * 避免覆盖其他未编辑的字段。
 */
public class SupplierProfileUpdateRequest {
    private String companyName;
    private String email;
    private String phone;
    private String address;
    private String contactPerson;
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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
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

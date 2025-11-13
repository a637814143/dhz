package com.example.silkmall.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "suppliers")
public class Supplier extends User {
    private String companyName;
    private String businessLicense;
    private String contactPerson;
    private Date joinDate;
    private String supplierLevel;
    private String status; // 审核状态
    
    // Getters and Setters
    public String getCompanyName() {
        return companyName;
    }
    
    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }
    
    public String getBusinessLicense() {
        return businessLicense;
    }
    
    public void setBusinessLicense(String businessLicense) {
        this.businessLicense = businessLicense;
    }
    
    public String getContactPerson() {
        return contactPerson;
    }
    
    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }
    
    public Date getJoinDate() {
        return joinDate;
    }
    
    public void setJoinDate(Date joinDate) {
        this.joinDate = joinDate;
    }
    
    public String getSupplierLevel() {
        return supplierLevel;
    }
    
    public void setSupplierLevel(String supplierLevel) {
        this.supplierLevel = supplierLevel;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
}
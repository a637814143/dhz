package com.example.silkmall.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "consumers")
public class Consumer extends User {
    private String realName;
    private String idCard;
    private String avatar;
    private Integer points;
    private Integer membershipLevel;
    
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
}
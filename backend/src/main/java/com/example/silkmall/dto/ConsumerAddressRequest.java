package com.example.silkmall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ConsumerAddressRequest {
    @NotBlank(message = "请填写收货人姓名")
    @Size(max = 100, message = "收货人姓名长度不能超过100个字符")
    private String recipientName;

    @NotBlank(message = "请填写联系电话")
    @Size(max = 30, message = "联系电话长度不能超过30个字符")
    private String recipientPhone;

    @NotBlank(message = "请填写收货地址")
    @Size(max = 512, message = "收货地址长度不能超过512个字符")
    private String shippingAddress;

    private Boolean isDefault;

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getRecipientPhone() {
        return recipientPhone;
    }

    public void setRecipientPhone(String recipientPhone) {
        this.recipientPhone = recipientPhone;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean aDefault) {
        isDefault = aDefault;
    }
}

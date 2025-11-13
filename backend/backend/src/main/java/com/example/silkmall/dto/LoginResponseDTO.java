package com.example.silkmall.dto;

public class LoginResponseDTO {
    private String token;
    private long expiresIn;
    private long issuedAt;
    private String redirectUrl;
    private UserProfileDTO user;

    public LoginResponseDTO() {
    }

    public LoginResponseDTO(String token, long expiresIn, long issuedAt, String redirectUrl, UserProfileDTO user) {
        this.token = token;
        this.expiresIn = expiresIn;
        this.issuedAt = issuedAt;
        this.redirectUrl = redirectUrl;
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public long getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(long issuedAt) {
        this.issuedAt = issuedAt;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }

    public UserProfileDTO getUser() {
        return user;
    }

    public void setUser(UserProfileDTO user) {
        this.user = user;
    }
}

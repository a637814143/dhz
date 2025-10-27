package com.example.silkmall.dto;

public class ProcessReturnRequestDTO {
    private String status;
    private String resolution;
    private String adminResolution;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public String getAdminResolution() {
        return adminResolution;
    }

    public void setAdminResolution(String adminResolution) {
        this.adminResolution = adminResolution;
    }
}

package com.example.silkmall.dto;

import java.util.List;

public class WeeklySalesReportDTO {
    private List<WeeklySalesBucketDTO> weeks;

    public List<WeeklySalesBucketDTO> getWeeks() {
        return weeks;
    }

    public void setWeeks(List<WeeklySalesBucketDTO> weeks) {
        this.weeks = weeks;
    }
}

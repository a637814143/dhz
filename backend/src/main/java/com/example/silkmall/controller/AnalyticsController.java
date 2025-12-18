package com.example.silkmall.controller;

import com.example.silkmall.dto.WeeklySalesReportDTO;
import com.example.silkmall.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController extends BaseController {

    private final OrderService orderService;

    @Autowired
    public AnalyticsController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/weekly-sales")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WeeklySalesReportDTO> getWeeklySales(
            @RequestParam(value = "weeks", required = false) Integer weeks) {
        int resolvedWeeks = weeks == null ? 8 : weeks;
        return success(orderService.getWeeklySalesReport(resolvedWeeks));
    }
}

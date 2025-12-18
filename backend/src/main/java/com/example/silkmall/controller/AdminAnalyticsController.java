package com.example.silkmall.controller;

import com.example.silkmall.dto.WeeklySalesStatisticsDTO;
import com.example.silkmall.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admins/analytics")
public class AdminAnalyticsController extends BaseController {

    private final OrderService orderService;

    @Autowired
    public AdminAnalyticsController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/weekly-sales")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<WeeklySalesStatisticsDTO>> getWeeklySales(
            @RequestParam(value = "weeks", defaultValue = "6") int weeks) {
        return success(orderService.getWeeklySalesStats(weeks));
    }
}

package com.example.silkmall.controller;

import com.example.silkmall.dto.home.HomepageContentDTO;
import com.example.silkmall.service.HomepageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/homepage")
public class HomepageController extends BaseController {
    private final HomepageService homepageService;

    public HomepageController(HomepageService homepageService) {
        this.homepageService = homepageService;
    }

    @GetMapping
    public ResponseEntity<HomepageContentDTO> getHomepageContent() {
        return success(homepageService.getHomepageContent());
    }
}
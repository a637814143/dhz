package com.example.silkmall.controller;

import com.example.silkmall.dto.HomepageContentDTO;
import com.example.silkmall.service.HomepageContentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/content")
public class ContentController extends BaseController {

    private final HomepageContentService homepageContentService;

    public ContentController(HomepageContentService homepageContentService) {
        this.homepageContentService = homepageContentService;
    }

    @GetMapping("/home")
    public ResponseEntity<HomepageContentDTO> getHomepageContent() {
        return success(homepageContentService.getHomepageContent());
    }
}

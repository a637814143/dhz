package com.example.silkmall.dto.home;

import com.example.silkmall.dto.ProductSummaryDTO;

import java.util.List;

public class HomepageContentDTO {
    private List<HomepageBannerDTO> banners;
    private List<ProductSummaryDTO> recommendedProducts;
    private List<ProductSummaryDTO> hotProducts;
    private List<HomepagePromotionDTO> promotions;
    private List<HomepageAnnouncementDTO> announcements;

    public List<HomepageBannerDTO> getBanners() {
        return banners;
    }

    public void setBanners(List<HomepageBannerDTO> banners) {
        this.banners = banners;
    }

    public List<ProductSummaryDTO> getRecommendedProducts() {
        return recommendedProducts;
    }

    public void setRecommendedProducts(List<ProductSummaryDTO> recommendedProducts) {
        this.recommendedProducts = recommendedProducts;
    }

    public List<ProductSummaryDTO> getHotProducts() {
        return hotProducts;
    }

    public void setHotProducts(List<ProductSummaryDTO> hotProducts) {
        this.hotProducts = hotProducts;
    }

    public List<HomepagePromotionDTO> getPromotions() {
        return promotions;
    }

    public void setPromotions(List<HomepagePromotionDTO> promotions) {
        this.promotions = promotions;
    }

    public List<HomepageAnnouncementDTO> getAnnouncements() {
        return announcements;
    }

    public void setAnnouncements(List<HomepageAnnouncementDTO> announcements) {
        this.announcements = announcements;
    }
}
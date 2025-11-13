package com.example.silkmall.dto;

import java.util.List;

public class HomepageContentDTO {
    private List<ProductSummaryDTO> recommendations;
    private List<ProductSummaryDTO> hotSales;
    private List<PromotionDTO> promotions;
    private List<BannerDTO> banners;
    private List<AnnouncementDTO> announcements;
    private List<NewsItemDTO> news;

    public List<ProductSummaryDTO> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<ProductSummaryDTO> recommendations) {
        this.recommendations = recommendations;
    }

    public List<ProductSummaryDTO> getHotSales() {
        return hotSales;
    }

    public void setHotSales(List<ProductSummaryDTO> hotSales) {
        this.hotSales = hotSales;
    }

    public List<PromotionDTO> getPromotions() {
        return promotions;
    }

    public void setPromotions(List<PromotionDTO> promotions) {
        this.promotions = promotions;
    }

    public List<BannerDTO> getBanners() {
        return banners;
    }

    public void setBanners(List<BannerDTO> banners) {
        this.banners = banners;
    }

    public List<AnnouncementDTO> getAnnouncements() {
        return announcements;
    }

    public void setAnnouncements(List<AnnouncementDTO> announcements) {
        this.announcements = announcements;
    }

    public List<NewsItemDTO> getNews() {
        return news;
    }

    public void setNews(List<NewsItemDTO> news) {
        this.news = news;
    }
}

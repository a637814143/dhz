package com.example.silkmall.service.impl;

import com.example.silkmall.dto.ProductSummaryDTO;
import com.example.silkmall.dto.home.HomepageAnnouncementDTO;
import com.example.silkmall.dto.home.HomepageBannerDTO;
import com.example.silkmall.dto.home.HomepageContentDTO;
import com.example.silkmall.dto.home.HomepagePromotionDTO;
import com.example.silkmall.entity.Product;
import com.example.silkmall.repository.ProductRepository;
import com.example.silkmall.service.HomepageService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class HomepageServiceImpl implements HomepageService {
    private static final ZoneOffset DEFAULT_ZONE_OFFSET = ZoneOffset.ofHours(8);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final ProductRepository productRepository;

    public HomepageServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public HomepageContentDTO getHomepageContent() {
        HomepageContentDTO contentDTO = new HomepageContentDTO();

        List<Product> recommended = productRepository.findTop8ByStatusOrderByCreatedAtDesc("ON_SALE");
        List<Product> hotProducts = productRepository.findTop8ByStatusOrderBySalesDesc("ON_SALE");

        contentDTO.setRecommendedProducts(recommended.stream().map(this::toSummaryDTO).toList());
        contentDTO.setHotProducts(hotProducts.stream().map(this::toSummaryDTO).toList());
        contentDTO.setBanners(buildBanners());
        contentDTO.setPromotions(buildPromotions());
        contentDTO.setAnnouncements(buildAnnouncements());

        return contentDTO;
    }

    private List<HomepageBannerDTO> buildBanners() {
        List<HomepageBannerDTO> banners = new ArrayList<>();

        HomepageBannerDTO banner1 = new HomepageBannerDTO();
        banner1.setId("banner-spring");
        banner1.setTitle("春蚕焕新季");
        banner1.setDescription("甄选四川优质春茧，带来丝滑轻盈的新款蚕丝面料。");
        banner1.setImageUrl("https://images.unsplash.com/photo-1523381210434-271e8be1f52b?auto=format&fit=crop&w=1200&q=80");
        banner1.setLinkUrl("/collections/spring-release");
        banner1.setCtaText("立即选购");
        banners.add(banner1);

        HomepageBannerDTO banner2 = new HomepageBannerDTO();
        banner2.setId("banner-gift");
        banner2.setTitle("企业团购礼遇");
        banner2.setDescription("支持定制logo与专属包装，满足企业礼赠与渠道批发需求。");
        banner2.setImageUrl("https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?auto=format&fit=crop&w=1200&q=80");
        banner2.setLinkUrl("/solutions/b2b");
        banner2.setCtaText("获取方案");
        banners.add(banner2);

        HomepageBannerDTO banner3 = new HomepageBannerDTO();
        banner3.setId("banner-science");
        banner3.setTitle("蚕桑数智化中心");
        banner3.setDescription("实时掌握原料、库存、渠道销售，助力蚕桑产业数字化升级。");
        banner3.setImageUrl("https://images.unsplash.com/photo-1460925895917-afdab827c52f?auto=format&fit=crop&w=1200&q=80");
        banner3.setLinkUrl("/about/platform");
        banner3.setCtaText("了解更多");
        banners.add(banner3);

        return banners;
    }

    private List<HomepagePromotionDTO> buildPromotions() {
        List<HomepagePromotionDTO> promotions = new ArrayList<>();

        HomepagePromotionDTO springPromo = new HomepagePromotionDTO();
        springPromo.setId("promo-spring-pack");
        springPromo.setTitle("春季家纺上新礼包");
        springPromo.setDescription("床品四件套、丝绵被组套8折，含免费包邮与一对一客服服务。");
        springPromo.setDiscountRate(new BigDecimal("0.80"));
        springPromo.setTags(List.of("限时", "家纺", "包邮"));
        springPromo.setStartDate(formatDate(LocalDate.now().minusDays(3)));
        springPromo.setEndDate(formatDate(LocalDate.now().plusDays(10)));
        promotions.add(springPromo);

        HomepagePromotionDTO vipPromo = new HomepagePromotionDTO();
        vipPromo.setId("promo-vip");
        vipPromo.setTitle("会员专享补贴");
        vipPromo.setDescription("平台金牌供应商联合让利，会员消费累计满5000元再享95折。");
        vipPromo.setDiscountRate(new BigDecimal("0.95"));
        vipPromo.setTags(List.of("会员", "供应商合作"));
        vipPromo.setStartDate(formatDate(LocalDate.now().minusDays(10)));
        vipPromo.setEndDate(formatDate(LocalDate.now().plusDays(20)));
        promotions.add(vipPromo);

        HomepagePromotionDTO flashPromo = new HomepagePromotionDTO();
        flashPromo.setId("promo-flash");
        flashPromo.setTitle("周三闪购·蚕丝好物");
        flashPromo.setDescription("每周三10:00 准时开抢，精选丝绸围巾限量秒杀，售完即止。");
        flashPromo.setDiscountRate(new BigDecimal("0.70"));
        flashPromo.setTags(List.of("限量", "秒杀", "周三专场"));
        flashPromo.setStartDate(formatDate(LocalDate.now()));
        flashPromo.setEndDate(formatDate(LocalDate.now().plusWeeks(4)));
        promotions.add(flashPromo);

        return promotions;
    }

    private List<HomepageAnnouncementDTO> buildAnnouncements() {
        List<HomepageAnnouncementDTO> announcements = new ArrayList<>();

        HomepageAnnouncementDTO notice = new HomepageAnnouncementDTO();
        notice.setId("notice-warehouse");
        notice.setTitle("仓配升级通知");
        notice.setContent("华东智能仓启用夜间打包线，次日达覆盖12个核心城市。");
        notice.setType("SYSTEM");
        notice.setPublishedAt(formatDateTime(OffsetDateTime.now(DEFAULT_ZONE_OFFSET).minusDays(1)));
        notice.setLinkUrl("/news/warehouse-upgrade");
        announcements.add(notice);

        HomepageAnnouncementDTO policy = new HomepageAnnouncementDTO();
        policy.setId("notice-policy");
        policy.setTitle("退换货政策更新");
        policy.setContent("延长蚕丝被类目售后时效至15天，并新增在线客服绿色通道。");
        policy.setType("POLICY");
        policy.setPublishedAt(formatDateTime(OffsetDateTime.now(DEFAULT_ZONE_OFFSET).minusDays(3)));
        policy.setLinkUrl("/help/return-policy");
        announcements.add(policy);

        HomepageAnnouncementDTO expo = new HomepageAnnouncementDTO();
        expo.setId("notice-expo");
        expo.setTitle("西部蚕桑产业博览会火热报名");
        expo.setContent("平台将携50+供应商亮相成都会展中心，欢迎渠道商预约洽谈。");
        expo.setType("EVENT");
        expo.setPublishedAt(formatDateTime(OffsetDateTime.now(DEFAULT_ZONE_OFFSET).minusDays(5)));
        expo.setLinkUrl("/events/2024-west-expo");
        announcements.add(expo);

        return announcements;
    }

    private ProductSummaryDTO toSummaryDTO(Product product) {
        ProductSummaryDTO dto = new ProductSummaryDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        dto.setStock(product.getStock());
        dto.setSales(product.getSales());
        dto.setMainImage(product.getMainImage());
        dto.setStatus(product.getStatus());
        dto.setCreatedAt(product.getCreatedAt());

        if (product.getCategory() != null) {
            dto.setCategoryName(product.getCategory().getName());
        }
        if (product.getSupplier() != null) {
            dto.setSupplierName(product.getSupplier().getCompanyName());
            dto.setSupplierLevel(product.getSupplier().getSupplierLevel());
        }

        return dto;
    }

    private String formatDate(LocalDate date) {
        return date.format(DATE_FORMATTER);
    }

    private String formatDateTime(OffsetDateTime dateTime) {
        return dateTime.format(DATE_TIME_FORMATTER);
    }
}

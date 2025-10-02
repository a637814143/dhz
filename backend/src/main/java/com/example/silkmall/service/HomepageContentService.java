package com.example.silkmall.service;

import com.example.silkmall.dto.AnnouncementDTO;
import com.example.silkmall.dto.BannerDTO;
import com.example.silkmall.dto.HomepageContentDTO;
import com.example.silkmall.dto.NewsItemDTO;
import com.example.silkmall.dto.ProductSummaryDTO;
import com.example.silkmall.dto.PromotionDTO;
import com.example.silkmall.entity.Product;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class HomepageContentService {
    private final ProductService productService;

    public HomepageContentService(ProductService productService) {
        this.productService = productService;
    }

    public HomepageContentDTO getHomepageContent() {
        HomepageContentDTO dto = new HomepageContentDTO();

        List<ProductSummaryDTO> recommendations = productService
                .findTop8ByStatusOrderByCreatedAtDesc("ON_SALE")
                .stream()
                .map(this::toSummary)
                .collect(Collectors.toList());

        List<ProductSummaryDTO> hotSales = productService
                .findTop8ByStatusOrderBySalesDesc("ON_SALE")
                .stream()
                .map(this::toSummary)
                .collect(Collectors.toList());

        List<PromotionDTO> promotions = buildPromotions();
        List<BannerDTO> banners = buildBanners(recommendations, hotSales);
        List<AnnouncementDTO> announcements = buildAnnouncements();
        List<NewsItemDTO> news = buildNewsItems();

        dto.setRecommendations(recommendations);
        dto.setHotSales(hotSales);
        dto.setPromotions(promotions);
        dto.setBanners(banners);
        dto.setAnnouncements(announcements);
        dto.setNews(news);
        return dto;
    }

    private List<PromotionDTO> buildPromotions() {
        return productService.findTop8ByStatusOrderByPriceAsc("ON_SALE")
                .stream()
                .map(product -> {
                    PromotionDTO promotion = new PromotionDTO();
                    promotion.setProductId(product.getId());
                    promotion.setTitle("限时优惠 · " + safeName(product.getName()));
                    promotion.setDescription("精选蚕丝产品限时立减，库存有限先到先得！");
                    promotion.setDiscountRate(0.15);
                    promotion.setValidUntil(Instant.now().plus(7, ChronoUnit.DAYS));
                    return promotion;
                })
                .collect(Collectors.toList());
    }

    private List<BannerDTO> buildBanners(List<ProductSummaryDTO> recommendations, List<ProductSummaryDTO> hotSales) {
        List<BannerDTO> banners = new ArrayList<>();
        if (!recommendations.isEmpty()) {
            ProductSummaryDTO first = recommendations.get(0);
            BannerDTO banner = new BannerDTO();
            banner.setHeadline("新品首发" + decorate(first.getName()));
            banner.setSubHeadline("天然蚕丝，亲肤透气，限量抢购中");
            banner.setCtaText("立即查看");
            banner.setTargetUrl("/product/" + first.getId());
            banner.setImageUrl(resolveImage(first.getMainImage()));
            banners.add(banner);
        }

        if (!hotSales.isEmpty()) {
            ProductSummaryDTO first = hotSales.get(0);
            BannerDTO banner = new BannerDTO();
            banner.setHeadline("热销冠军" + decorate(first.getName()));
            banner.setSubHeadline("人气爆款，复购率超 95%！");
            banner.setCtaText("加入购物车");
            banner.setTargetUrl("/product/" + first.getId());
            banner.setImageUrl(resolveImage(first.getMainImage()));
            banners.add(banner);
        }

        BannerDTO serviceBanner = new BannerDTO();
        serviceBanner.setHeadline("企业采购通道开启");
        serviceBanner.setSubHeadline("提供蚕丝原料、OEM 定制与品牌共创服务");
        serviceBanner.setCtaText("联系顾问");
        serviceBanner.setTargetUrl("/supplier/workbench");
        serviceBanner.setImageUrl("/images/banners/b2b.png");
        banners.add(serviceBanner);

        return banners;
    }

    private List<AnnouncementDTO> buildAnnouncements() {
        List<AnnouncementDTO> list = new ArrayList<>();

        AnnouncementDTO platform = new AnnouncementDTO();
        platform.setId(UUID.randomUUID().toString());
        platform.setTitle("系统升级通知");
        platform.setContent("SilkMall 将于本周日晚间 23:00 - 02:00 进行功能升级，请提前安排采购计划。");
        platform.setCategory("平台公告");
        platform.setPublishedAt(Instant.now().minus(1, ChronoUnit.DAYS));
        list.add(platform);

        AnnouncementDTO help = new AnnouncementDTO();
        help.setId(UUID.randomUUID().toString());
        help.setTitle("蚕丝被保养指南");
        help.setContent("新上线的保养视频教程已发布，帮助您延长蚕丝制品使用寿命。");
        help.setCategory("使用帮助");
        help.setPublishedAt(Instant.now().minus(2, ChronoUnit.DAYS));
        list.add(help);

        AnnouncementDTO policy = new AnnouncementDTO();
        policy.setId(UUID.randomUUID().toString());
        policy.setTitle("行业补贴政策速递");
        policy.setContent("多地发布蚕桑产业扶持政策，详情查看资讯栏目了解申报条件。");
        policy.setCategory("行业资讯");
        policy.setPublishedAt(Instant.now().minus(3, ChronoUnit.DAYS));
        list.add(policy);

        return list;
    }

    private List<NewsItemDTO> buildNewsItems() {
        List<NewsItemDTO> list = new ArrayList<>();

        NewsItemDTO market = new NewsItemDTO();
        market.setId(UUID.randomUUID().toString());
        market.setTitle("全球蚕丝市场需求持续回暖");
        market.setSummary("国际轻奢品牌订单增长 18%，绿色可持续蚕丝制品成为消费热点。");
        market.setSource("丝绸协会");
        market.setPublishedAt(Instant.now().minus(6, ChronoUnit.HOURS));
        list.add(market);

        NewsItemDTO tech = new NewsItemDTO();
        tech.setId(UUID.randomUUID().toString());
        tech.setTitle("智能织造工厂落地苏州高新区");
        tech.setSummary("引入 AI 织造检测技术，生产效率提升 32%，品质稳定性显著增强。");
        tech.setSource("蚕桑科技周报");
        tech.setPublishedAt(Instant.now().minus(12, ChronoUnit.HOURS));
        list.add(tech);

        NewsItemDTO export = new NewsItemDTO();
        export.setId(UUID.randomUUID().toString());
        export.setTitle("东南亚电商渠道开启丝绸专场");
        export.setSummary("跨境电商平台与 SilkMall 建立战略合作，助力供应商拓展国际市场。");
        export.setSource("跨境物流联盟");
        export.setPublishedAt(Instant.now().minus(2, ChronoUnit.DAYS));
        list.add(export);

        return list;
    }

    private ProductSummaryDTO toSummary(Product product) {
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

    private String safeName(String name) {
        if (name == null || name.isBlank()) {
            return "精选好物";
        }
        return name;
    }

    private String decorate(String name) {
        return " · " + safeName(name);
    }

    private String resolveImage(String image) {
        if (image == null || image.isBlank()) {
            return "/images/banners/default.png";
        }
        return image;
    }
}

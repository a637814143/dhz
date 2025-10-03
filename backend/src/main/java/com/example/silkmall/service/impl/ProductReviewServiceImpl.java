package com.example.silkmall.service.impl;

import com.example.silkmall.entity.Admin;
import com.example.silkmall.entity.Consumer;
import com.example.silkmall.entity.OrderItem;
import com.example.silkmall.entity.Product;
import com.example.silkmall.entity.ProductReview;
import com.example.silkmall.entity.Supplier;
import com.example.silkmall.repository.AdminRepository;
import com.example.silkmall.repository.OrderItemRepository;
import com.example.silkmall.repository.ProductRepository;
import com.example.silkmall.repository.ProductReviewRepository;
import com.example.silkmall.repository.SupplierRepository;
import com.example.silkmall.security.CustomUserDetails;
import com.example.silkmall.service.ProductReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class ProductReviewServiceImpl extends BaseServiceImpl<ProductReview, Long> implements ProductReviewService {
    private final ProductReviewRepository reviewRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final AdminRepository adminRepository;

    @Autowired
    public ProductReviewServiceImpl(ProductReviewRepository reviewRepository,
                                    OrderItemRepository orderItemRepository,
                                    ProductRepository productRepository,
                                    SupplierRepository supplierRepository,
                                    AdminRepository adminRepository) {
        super(reviewRepository);
        this.reviewRepository = reviewRepository;
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
        this.supplierRepository = supplierRepository;
        this.adminRepository = adminRepository;
    }

    @Override
    @Transactional
    public ProductReview createReview(Long orderItemId, ProductReview review, CustomUserDetails author) {
        OrderItem orderItem = orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new RuntimeException("订单项不存在"));

        if (!"DELIVERED".equals(orderItem.getOrder().getStatus()) && !isAdmin(author)) {
            throw new RuntimeException("只有已收货的订单才能评价");
        }

        review.setOrder(orderItem.getOrder());
        review.setOrderItem(orderItem);
        review.setProduct(orderItem.getProduct());
        review.setComment(normalizeComment(review.getComment()));
        review.setRating(normalizeRating(review.getRating()));

        String role = normalizeRole(author);
        switch (role) {
            case "consumer" -> {
                Consumer consumer = orderItem.getOrder().getConsumer();
                if (consumer == null || !Objects.equals(consumer.getId(), author.getId())) {
                    throw new RuntimeException("只能评价自己的订单");
                }
                if (reviewRepository.existsByOrderItemIdAndConsumer_Id(orderItemId, consumer.getId())) {
                    throw new RuntimeException("该商品已评价过");
                }
                review.setConsumer(consumer);
                applyAuthorInfo(review, consumer.getId(), consumer.getUsername(), "CONSUMER");
            }
            case "admin" -> {
                Admin admin = adminRepository.findById(author.getId())
                        .orElseThrow(() -> new RuntimeException("管理员不存在"));
                review.setAdmin(admin);
                applyAuthorInfo(review, admin.getId(), admin.getUsername(), "ADMIN");
            }
            default -> throw new RuntimeException("仅消费者或管理员可对订单进行评价");
        }

        return reviewRepository.save(review);
    }

    @Override
    @Transactional
    public ProductReview createDirectReview(Long productId, ProductReview review, CustomUserDetails author) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("产品不存在"));

        review.setProduct(product);
        review.setComment(normalizeComment(review.getComment()));
        review.setRating(normalizeRating(review.getRating()));

        String role = normalizeRole(author);
        switch (role) {
            case "supplier" -> {
                Supplier supplier = supplierRepository.findById(author.getId())
                        .orElseThrow(() -> new RuntimeException("供应商不存在"));
                if (product.getSupplier() == null || !Objects.equals(product.getSupplier().getId(), supplier.getId())) {
                    throw new RuntimeException("只能评价自己发布的商品");
                }
                review.setSupplier(supplier);
                String name = supplier.getCompanyName() != null && !supplier.getCompanyName().isBlank()
                        ? supplier.getCompanyName()
                        : supplier.getUsername();
                applyAuthorInfo(review, supplier.getId(), name, "SUPPLIER");
            }
            case "admin" -> {
                Admin admin = adminRepository.findById(author.getId())
                        .orElseThrow(() -> new RuntimeException("管理员不存在"));
                review.setAdmin(admin);
                applyAuthorInfo(review, admin.getId(), admin.getUsername(), "ADMIN");
            }
            default -> throw new RuntimeException("仅供应商或管理员可直接评价商品");
        }

        return reviewRepository.save(review);
    }

    @Override
    @Transactional
    public ProductReview updateReview(Long reviewId, Integer rating, String comment, CustomUserDetails author) {
        ProductReview existing = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("评价不存在"));

        assertCanEdit(existing, author);
        if (rating != null) {
            existing.setRating(normalizeRating(rating));
        }
        if (comment != null) {
            existing.setComment(normalizeComment(comment));
        }
        return reviewRepository.save(existing);
    }

    @Override
    @Transactional
    public void deleteReview(Long reviewId, CustomUserDetails actor) {
        ProductReview existing = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("评价不存在"));

        if (!isAdmin(actor) && !isOwner(existing, actor)) {
            throw new RuntimeException("没有权限删除该评价");
        }
        reviewRepository.delete(existing);
    }

    @Override
    public List<ProductReview> findByProductId(Long productId) {
        return reviewRepository.findByProductId(productId);
    }

    @Override
    public List<ProductReview> findByOrderId(Long orderId) {
        return reviewRepository.findByOrderId(orderId);
    }

    private void applyAuthorInfo(ProductReview review, Long authorId, String authorName, String role) {
        review.setAuthorId(authorId);
        review.setAuthorName(authorName);
        review.setAuthorRole(role);
    }

    private String normalizeComment(String comment) {
        if (comment == null) {
            return null;
        }
        String trimmed = comment.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private int normalizeRating(Integer rating) {
        int value = rating == null ? 5 : rating;
        if (value < 1 || value > 5) {
            throw new RuntimeException("评分必须在1到5之间");
        }
        return value;
    }

    private String normalizeRole(CustomUserDetails user) {
        if (user == null || user.getUserType() == null) {
            return "";
        }
        return user.getUserType().toLowerCase();
    }

    private boolean isAdmin(CustomUserDetails user) {
        return "admin".equalsIgnoreCase(user != null ? user.getUserType() : null);
    }

    private void assertCanEdit(ProductReview review, CustomUserDetails user) {
        if (user == null || user.getUserType() == null) {
            throw new RuntimeException("未登录或登录已过期");
        }
        String role = normalizeRole(user);
        if ("admin".equals(role)) {
            if (review.getAdmin() != null && Objects.equals(review.getAdmin().getId(), user.getId())) {
                return;
            }
            throw new RuntimeException("管理员只能编辑自己的评价");
        }
        if (!isOwner(review, user)) {
            throw new RuntimeException("仅能编辑自己的评价");
        }
    }

    private boolean isOwner(ProductReview review, CustomUserDetails user) {
        if (user == null || user.getUserType() == null) {
            return false;
        }
        String role = normalizeRole(user);
        return switch (role) {
            case "consumer" -> review.getConsumer() != null && Objects.equals(review.getConsumer().getId(), user.getId());
            case "supplier" -> review.getSupplier() != null && Objects.equals(review.getSupplier().getId(), user.getId());
            case "admin" -> review.getAdmin() != null && Objects.equals(review.getAdmin().getId(), user.getId());
            default -> false;
        };
    }
}

package com.example.silkmall.service;

import com.example.silkmall.common.OrderStatuses;
import com.example.silkmall.entity.Admin;
import com.example.silkmall.entity.Consumer;
import com.example.silkmall.entity.Order;
import com.example.silkmall.entity.OrderItem;
import com.example.silkmall.entity.Product;
import com.example.silkmall.entity.ProductReview;
import com.example.silkmall.repository.AdminRepository;
import com.example.silkmall.repository.ConsumerRepository;
import com.example.silkmall.repository.OrderItemRepository;
import com.example.silkmall.repository.OrderRepository;
import com.example.silkmall.repository.ProductRepository;
import com.example.silkmall.repository.ProductReviewRepository;
import com.example.silkmall.security.CustomUserDetails;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:reviewsdb;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.main.allow-circular-references=true"
})
class ProductReviewServiceIntegrationTest {

    @Autowired
    private ProductReviewService productReviewService;

    @Autowired
    private ProductReviewRepository productReviewRepository;

    @Autowired
    private ConsumerRepository consumerRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Test
    void consumerCanSubmitReviewAfterAdminReviewForSameOrderItem() {
        Consumer consumer = new Consumer();
        consumer.setUsername("consumerUser");
        consumer.setPassword("secret");
        consumer.setEmail("consumer@example.com");
        consumer.setPhone("18800000000");
        consumer.setRole("consumer");
        consumer.setEnabled(true);
        consumer.setWalletBalance(BigDecimal.valueOf(1000));
        consumer = consumerRepository.save(consumer);

        Admin admin = new Admin();
        admin.setUsername("adminUser");
        admin.setPassword("secret");
        admin.setEmail("admin@example.com");
        admin.setPhone("19900000000");
        admin.setRole("admin");
        admin.setEnabled(true);
        admin = adminRepository.save(admin);

        Product product = new Product();
        product.setName("Silk Scarf");
        product.setPrice(BigDecimal.valueOf(299));
        product.setStock(50);
        product.setStatus("ON_SALE");
        product = productRepository.save(product);

        Order order = new Order();
        order.setOrderNo("ORD-001");
        order.setStatus(OrderStatuses.DELIVERED);
        order.setTotalAmount(BigDecimal.valueOf(299));
        order.setTotalQuantity(1);
        order.setConsumer(consumer);
        order = orderRepository.save(order);

        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(order);
        orderItem.setProduct(product);
        orderItem.setQuantity(1);
        orderItem.setUnitPrice(BigDecimal.valueOf(299));
        orderItem.setTotalPrice(BigDecimal.valueOf(299));
        orderItem = orderItemRepository.save(orderItem);

        CustomUserDetails adminDetails = new CustomUserDetails(
                admin.getId(),
                admin.getUsername(),
                admin.getPassword(),
                admin.getEmail(),
                admin.getPhone(),
                "admin",
                true,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        ProductReview adminReview = new ProductReview();
        adminReview.setRating(4);
        adminReview.setComment("Admin feedback");

        productReviewService.createReview(orderItem.getId(), adminReview, adminDetails);
        productReviewRepository.flush();

        CustomUserDetails consumerDetails = new CustomUserDetails(
                consumer.getId(),
                consumer.getUsername(),
                consumer.getPassword(),
                consumer.getEmail(),
                consumer.getPhone(),
                "consumer",
                true,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_CONSUMER"))
        );

        ProductReview consumerReview = new ProductReview();
        consumerReview.setRating(5);
        consumerReview.setComment("Great product");

        productReviewService.createReview(orderItem.getId(), consumerReview, consumerDetails);
        productReviewRepository.flush();

        List<ProductReview> reviews = productReviewRepository.findAll();
        assertThat(reviews).hasSize(2);
        assertThat(reviews.stream().map(ProductReview::getAuthorRole))
                .containsExactlyInAnyOrder("ADMIN", "CONSUMER");

        ProductReview savedConsumerReview = reviews.stream()
                .filter(review -> "CONSUMER".equals(review.getAuthorRole()))
                .findFirst()
                .orElseThrow();
        assertThat(savedConsumerReview.getConsumer()).isNotNull();
        assertThat(savedConsumerReview.getConsumer().getId()).isEqualTo(consumer.getId());
    }
}


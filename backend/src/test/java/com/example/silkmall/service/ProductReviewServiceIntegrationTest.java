package com.example.silkmall.service;

import com.example.silkmall.config.ProductReviewSchemaFixer;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

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

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ProductReviewSchemaFixer schemaFixer;

    @Test
    void consumerCanSubmitReviewAfterAdminReviewForSameOrderItem() {
        productReviewRepository.deleteAll();
        productReviewRepository.flush();

        ReviewFixture fixture = prepareReviewFixture("DELIVERED");

        ProductReview adminReview = new ProductReview();
        adminReview.setRating(4);
        adminReview.setComment("Admin feedback");

        productReviewService.createReview(fixture.orderItem().getId(), adminReview, fixture.adminDetails());
        productReviewRepository.flush();

        ProductReview consumerReview = new ProductReview();
        consumerReview.setRating(5);
        consumerReview.setComment("Great product");

        productReviewService.createReview(fixture.orderItem().getId(), consumerReview, fixture.consumerDetails());
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
        assertThat(savedConsumerReview.getConsumer().getId()).isEqualTo(fixture.consumer().getId());
    }

    @Test
    void consumerCanSubmitReviewAfterOrderMarkedReceived() {
        productReviewRepository.deleteAll();
        productReviewRepository.flush();

        ReviewFixture fixture = prepareReviewFixture("RECEIVED");

        ProductReview consumerReview = new ProductReview();
        consumerReview.setRating(5);
        consumerReview.setComment("Confirmed delivery review");

        ProductReview saved = productReviewService.createReview(
                fixture.orderItem().getId(),
                consumerReview,
                fixture.consumerDetails()
        );
        productReviewRepository.flush();

        ProductReview persisted = productReviewRepository.findById(saved.getId()).orElseThrow();
        assertThat(persisted.getConsumer()).isNotNull();
        assertThat(persisted.getConsumer().getId()).isEqualTo(fixture.consumer().getId());
    }

    @Test
    void schemaFixerDropsLegacyOrderItemUniqueIndex() {
        productReviewRepository.deleteAll();
        productReviewRepository.flush();

        jdbcTemplate.execute("CREATE UNIQUE INDEX legacy_pr_order_item ON product_reviews(order_item_id)");

        Integer before = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.INDEXES WHERE UPPER(INDEX_NAME) = 'LEGACY_PR_ORDER_ITEM'",
                Integer.class);
        assertThat(before).isEqualTo(1);

        schemaFixer.ensureFlexibleReviewSchema();

        Integer after = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.INDEXES WHERE UPPER(INDEX_NAME) = 'LEGACY_PR_ORDER_ITEM'",
                Integer.class);
        assertThat(after).isZero();
    }

    private ReviewFixture prepareReviewFixture(String orderStatus) {
        String suffix = orderStatus.toLowerCase(Locale.ROOT);

        Consumer consumer = new Consumer();
        consumer.setUsername("consumerUser-" + suffix);
        consumer.setPassword("secret");
        consumer.setEmail("consumer-" + suffix + "@example.com");
        consumer.setPhone("18800000000");
        consumer.setRole("consumer");
        consumer.setEnabled(true);
        consumer.setWalletBalance(BigDecimal.valueOf(1000));
        consumer = consumerRepository.save(consumer);

        Admin admin = new Admin();
        admin.setUsername("adminUser-" + suffix);
        admin.setPassword("secret");
        admin.setEmail("admin-" + suffix + "@example.com");
        admin.setPhone("19900000000");
        admin.setRole("admin");
        admin.setEnabled(true);
        admin = adminRepository.save(admin);

        Product product = new Product();
        product.setName("Silk Scarf " + orderStatus);
        product.setPrice(BigDecimal.valueOf(299));
        product.setStock(50);
        product.setStatus("ON_SALE");
        product = productRepository.save(product);

        Order order = new Order();
        order.setOrderNo("ORD-" + suffix + "-" + UUID.randomUUID());
        order.setStatus(orderStatus);
        order.setTotalAmount(product.getPrice());
        order.setTotalQuantity(1);
        order.setConsumer(consumer);
        order = orderRepository.save(order);

        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(order);
        orderItem.setProduct(product);
        orderItem.setQuantity(1);
        orderItem.setUnitPrice(product.getPrice());
        orderItem.setTotalPrice(product.getPrice());
        orderItem = orderItemRepository.save(orderItem);

        CustomUserDetails consumerDetails = buildUserDetails(
                consumer.getId(),
                consumer.getUsername(),
                consumer.getPassword(),
                consumer.getEmail(),
                consumer.getPhone(),
                "consumer"
        );

        CustomUserDetails adminDetails = buildUserDetails(
                admin.getId(),
                admin.getUsername(),
                admin.getPassword(),
                admin.getEmail(),
                admin.getPhone(),
                "admin"
        );

        return new ReviewFixture(consumer, admin, order, orderItem, consumerDetails, adminDetails);
    }

    private CustomUserDetails buildUserDetails(Long id, String username, String password,
                                              String email, String phone, String role) {
        return new CustomUserDetails(
                id,
                username,
                password,
                email,
                phone,
                role,
                true,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase(Locale.ROOT)))
        );
    }

    private record ReviewFixture(Consumer consumer,
                                 Admin admin,
                                 Order order,
                                 OrderItem orderItem,
                                 CustomUserDetails consumerDetails,
                                 CustomUserDetails adminDetails) {
    }
}


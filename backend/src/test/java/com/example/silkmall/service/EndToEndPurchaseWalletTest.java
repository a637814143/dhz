package com.example.silkmall.service;

import com.example.silkmall.entity.Consumer;
import com.example.silkmall.entity.Order;
import com.example.silkmall.entity.OrderItem;
import com.example.silkmall.entity.Product;
import com.example.silkmall.entity.Supplier;
import com.example.silkmall.security.CustomUserDetails;
import com.example.silkmall.service.impl.NewConsumerServiceImpl;
import com.example.silkmall.service.impl.NewSupplierServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class EndToEndPurchaseWalletTest {

    @Autowired
    private NewConsumerServiceImpl newConsumerService;

    @Autowired
    private NewSupplierServiceImpl newSupplierService;

    @Autowired
    private ProductService productService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private WalletService walletService;

    @Test
    void consumerWalletReflectsPurchaseDeductionThroughServices() {
        Consumer consumer = new Consumer();
        consumer.setUsername("e2e-consumer");
        consumer.setPassword("password");
        consumer.setEmail("e2e-consumer@example.com");
        consumer.setPhone("18800000001");
        consumer.setRole("consumer");
        consumer = newConsumerService.register(consumer);

        Supplier supplier = new Supplier();
        supplier.setUsername("e2e-supplier");
        supplier.setPassword("password");
        supplier.setEmail("e2e-supplier@example.com");
        supplier.setPhone("18800000002");
        supplier.setRole("supplier");
        supplier.setCompanyName("E2E Supplier");
        supplier = newSupplierService.register(supplier);

        Product product = new Product();
        product.setName("End-to-End Product");
        product.setDescription("E2E flow product");
        product.setPrice(BigDecimal.valueOf(100L));
        product.setStock(50);
        product.setSales(0);
        product.setStatus("ON_SALE");
        product.setSupplier(supplier);
        product = productService.save(product);

        Order order = new Order();
        order.setConsumer(consumer);
        order.setShippingAddress("E2E Address");
        order.setRecipientName("E2E Buyer");
        order.setRecipientPhone("18800000000");

        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setQuantity(1);
        order.setOrderItems(List.of(item));

        Order created = orderService.createOrder(order);
        assertThat(created.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(100L));

        CustomUserDetails principal = new CustomUserDetails(
                consumer.getId(),
                consumer.getUsername(),
                consumer.getPassword(),
                consumer.getEmail(),
                consumer.getPhone(),
                "consumer",
                true,
                Collections.emptyList()
        );

        assertThat(walletService.getBalance(principal)).isEqualByComparingTo(BigDecimal.valueOf(1000L));

        orderService.payOrder(created.getId(), "WALLET");

        assertThat(walletService.getBalance(principal)).isEqualByComparingTo(BigDecimal.valueOf(900L));
    }
}

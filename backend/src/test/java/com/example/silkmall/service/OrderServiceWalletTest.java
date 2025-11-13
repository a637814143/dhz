package com.example.silkmall.service;

import com.example.silkmall.entity.Consumer;
import com.example.silkmall.entity.Order;
import com.example.silkmall.entity.OrderItem;
import com.example.silkmall.entity.Product;
import com.example.silkmall.entity.Supplier;
import com.example.silkmall.repository.ConsumerRepository;
import com.example.silkmall.repository.ProductRepository;
import com.example.silkmall.repository.SupplierRepository;
import com.example.silkmall.security.CustomUserDetails;
import com.example.silkmall.service.WalletService;
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
class OrderServiceWalletTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ConsumerRepository consumerRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private WalletService walletService;

    @Test
    void payingOrderDeductsConsumerWalletByOrderTotal() {
        Consumer consumer = new Consumer();
        consumer.setUsername("wallet-customer");
        consumer.setPassword("password");
        consumer.setEmail("wallet@example.com");
        consumer.setPhone("1234567890");
        consumer.setRole("consumer");
        consumer = consumerRepository.save(consumer);

        Supplier supplier = new Supplier();
        supplier.setUsername("wallet-supplier");
        supplier.setPassword("password");
        supplier.setEmail("supplier@example.com");
        supplier.setPhone("0987654321");
        supplier.setRole("supplier");
        supplier.setCompanyName("Wallet Supplier Co.");
        supplier = supplierRepository.save(supplier);

        Product product = new Product();
        product.setName("Wallet Test Product");
        product.setDescription("Test product for wallet deduction");
        product.setPrice(BigDecimal.valueOf(100L));
        product.setStock(100);
        product.setSales(0);
        product.setStatus("ON_SALE");
        product.setSupplier(supplier);
        product = productRepository.save(product);

        Order order = new Order();
        order.setConsumer(consumer);
        order.setShippingAddress("Test Address");
        order.setRecipientName("Test Recipient");
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

        BigDecimal initialBalance = walletService.getBalance(principal);
        assertThat(initialBalance).isEqualByComparingTo(BigDecimal.valueOf(1000L));

        orderService.payOrder(created.getId(), "WALLET");

        Consumer reloaded = consumerRepository.findById(consumer.getId()).orElseThrow();
        assertThat(reloaded.getWalletBalance()).isEqualByComparingTo(BigDecimal.valueOf(900L));

        BigDecimal updatedBalance = walletService.getBalance(principal);
        assertThat(updatedBalance).isEqualByComparingTo(BigDecimal.valueOf(900L));

        // Subsequent purchase should continue deducting from the remaining balance
        Order anotherOrder = new Order();
        anotherOrder.setConsumer(consumer);
        anotherOrder.setShippingAddress("Test Address");
        anotherOrder.setRecipientName("Test Recipient");
        anotherOrder.setRecipientPhone("18800000000");

        OrderItem anotherItem = new OrderItem();
        anotherItem.setProduct(product);
        anotherItem.setQuantity(1);
        anotherOrder.setOrderItems(List.of(anotherItem));

        Order savedOrder = orderService.createOrder(anotherOrder);
        orderService.payOrder(savedOrder.getId(), "WALLET");

        Consumer twiceCharged = consumerRepository.findById(consumer.getId()).orElseThrow();
        assertThat(twiceCharged.getWalletBalance()).isEqualByComparingTo(BigDecimal.valueOf(800L));

        BigDecimal finalBalance = walletService.getBalance(principal);
        assertThat(finalBalance).isEqualByComparingTo(BigDecimal.valueOf(800L));
    }
}

package com.example.silkmall.service.impl;

import com.example.silkmall.entity.CartItem;
import com.example.silkmall.entity.Consumer;
import com.example.silkmall.entity.Order;
import com.example.silkmall.entity.OrderItem;
import com.example.silkmall.entity.Product;
import com.example.silkmall.repository.CartItemRepository;
import com.example.silkmall.repository.ConsumerRepository;
import com.example.silkmall.repository.ProductRepository;
import com.example.silkmall.service.CartService;
import com.example.silkmall.service.OrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CartServiceImpl extends BaseServiceImpl<CartItem, Long> implements CartService {

    private final CartItemRepository cartItemRepository;
    private final ConsumerRepository consumerRepository;
    private final ProductRepository productRepository;
    private final OrderService orderService;

    public CartServiceImpl(CartItemRepository cartItemRepository,
                           ConsumerRepository consumerRepository,
                           ProductRepository productRepository,
                           OrderService orderService) {
        super(cartItemRepository);
        this.cartItemRepository = cartItemRepository;
        this.consumerRepository = consumerRepository;
        this.productRepository = productRepository;
        this.orderService = orderService;
    }

    @Override
    public List<CartItem> getCartItems(Long consumerId) {
        return cartItemRepository.findByConsumerIdOrderByCreatedAtDesc(consumerId);
    }

    @Override
    @Transactional
    public CartItem addItem(Long consumerId, Long productId, int quantity) {
        if (quantity <= 0) {
            throw new RuntimeException("加入购物车的数量必须大于0");
        }

        Consumer consumer = consumerRepository.findById(consumerId)
                .orElseThrow(() -> new RuntimeException("消费者不存在"));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("商品不存在"));

        if (!"ON_SALE".equalsIgnoreCase(product.getStatus())) {
            throw new RuntimeException("该商品暂不可加入购物车");
        }

        Integer stock = product.getStock();
        if (stock == null || stock <= 0) {
            throw new RuntimeException("商品库存不足");
        }

        CartItem item = cartItemRepository.findByConsumerIdAndProductId(consumerId, productId)
                .orElse(null);

        int baseQuantity = item != null && item.getQuantity() != null ? item.getQuantity() : 0;
        int newQuantity = baseQuantity + quantity;
        if (newQuantity > stock) {
            throw new RuntimeException("商品库存不足");
        }

        if (item == null) {
            item = new CartItem();
            item.setConsumer(consumer);
            item.setProduct(product);
        }

        item.setQuantity(newQuantity);
        return cartItemRepository.save(item);
    }

    @Override
    @Transactional
    public CartItem updateQuantity(Long consumerId, Long itemId, int quantity) {
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("购物车商品不存在"));

        if (!item.getConsumer().getId().equals(consumerId)) {
            throw new RuntimeException("无权操作该购物车商品");
        }

        if (quantity <= 0) {
            cartItemRepository.delete(item);
            return null;
        }

        Product product = productRepository.findById(item.getProduct().getId())
                .orElseThrow(() -> new RuntimeException("商品不存在"));

        Integer stock = product.getStock();
        if (stock == null || quantity > stock) {
            throw new RuntimeException("商品库存不足");
        }

        item.setQuantity(quantity);
        return cartItemRepository.save(item);
    }

    @Override
    @Transactional
    public void removeItem(Long consumerId, Long itemId) {
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("购物车商品不存在"));

        if (!item.getConsumer().getId().equals(consumerId)) {
            throw new RuntimeException("无权操作该购物车商品");
        }

        cartItemRepository.delete(item);
    }

    @Override
    @Transactional
    public void clearCart(Long consumerId) {
        cartItemRepository.deleteByConsumerId(consumerId);
    }

    @Override
    @Transactional
    public Order checkout(Long consumerId, List<Long> itemIds, String paymentMethod) {
        if (itemIds == null || itemIds.isEmpty()) {
            throw new RuntimeException("请选择要结算的商品");
        }

        Consumer consumer = consumerRepository.findById(consumerId)
                .orElseThrow(() -> new RuntimeException("消费者不存在"));

        List<CartItem> items = cartItemRepository.findByIdInAndConsumerId(itemIds, consumerId);
        if (items.isEmpty()) {
            throw new RuntimeException("未找到要结算的购物车商品");
        }

        Order order = new Order();
        order.setConsumer(consumer);
        order.setPaymentMethod(paymentMethod);

        List<OrderItem> orderItems = items.stream().map(cartItem -> {
            Product product = productRepository.findById(cartItem.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("商品不存在"));

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            int quantity = cartItem.getQuantity() == null ? 0 : cartItem.getQuantity();
            orderItem.setQuantity(quantity);
            BigDecimal price = product.getPrice() == null ? BigDecimal.ZERO : product.getPrice();
            orderItem.setUnitPrice(price);
            orderItem.setTotalPrice(price.multiply(BigDecimal.valueOf(quantity)));
            orderItem.setSizeLabel(null);
            return orderItem;
        }).toList();

        order.setOrderItems(orderItems);
        Order created = orderService.createOrder(order);

        cartItemRepository.deleteAll(items);
        return created;
    }
}


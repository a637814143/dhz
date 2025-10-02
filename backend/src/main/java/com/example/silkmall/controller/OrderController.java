package com.example.silkmall.controller;

import com.example.silkmall.entity.Order;
import com.example.silkmall.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Optional;

@RestController
@RequestMapping("/api/orders")
public class OrderController extends BaseController {
    private final OrderService orderService;
    
    @Autowired
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<?> getOrderById(@PathVariable Long id) {
        Optional<Order> order = orderService.findById(id);
        if (order.isPresent()) {
            return success(order.get());
        } else {
            return notFound("订单不存在");
        }
    }
    
    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody Order order) {
        return created(orderService.createOrder(order));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Order> updateOrder(@PathVariable Long id, @RequestBody Order order) {
        order.setId(id);
        return success(orderService.save(order));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        orderService.deleteById(id);
        return success();
    }
    
    @GetMapping("/consumer/{consumerId}")
    public ResponseEntity<Page<Order>> getOrdersByConsumerId(@PathVariable Long consumerId, Pageable pageable) {
        return success(orderService.findByConsumerId(consumerId, pageable));
    }
    
    @GetMapping("/status/{status}")
    public ResponseEntity<Page<Order>> getOrdersByStatus(@PathVariable String status, Pageable pageable) {
        return success(orderService.findByStatus(status, pageable));
    }
    
    @GetMapping("/order-no/{orderNo}")
    public ResponseEntity<?> getOrderByOrderNo(@PathVariable String orderNo) {
        Optional<Order> order = orderService.findByOrderNo(orderNo).stream().findFirst();
        if (order.isPresent()) {
            return success(order.get());
        } else {
            return notFound("订单不存在");
        }
    }
    
    @PutMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelOrder(@PathVariable Long id) {
        orderService.cancelOrder(id);
        return success();
    }
    
    @PutMapping("/{id}/pay")
    public ResponseEntity<Void> payOrder(@PathVariable Long id, @RequestParam String paymentMethod) {
        orderService.payOrder(id, paymentMethod);
        return success();
    }
    
    @PutMapping("/{id}/ship")
    public ResponseEntity<Void> shipOrder(@PathVariable Long id) {
        orderService.shipOrder(id);
        return success();
    }
    
    @PutMapping("/{id}/deliver")
    public ResponseEntity<Void> deliverOrder(@PathVariable Long id) {
        orderService.deliverOrder(id);
        return success();
    }
    
    @GetMapping("/{id}/detail")
    public ResponseEntity<Order> getOrderDetail(@PathVariable Long id) {
        return success(orderService.findOrderDetail(id));
    }
}
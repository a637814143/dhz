package com.example.silkmall.controller;

import com.example.silkmall.dto.OrderDetailDTO;
import com.example.silkmall.dto.OrderItemDetailDTO;
import com.example.silkmall.dto.UpdateOrderContactDTO;
import com.example.silkmall.entity.Consumer;
import com.example.silkmall.entity.Order;
import com.example.silkmall.service.OrderService;
import com.example.silkmall.security.CustomUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
public class OrderController extends BaseController {
    private final OrderService orderService;
    
    @Autowired
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CONSUMER')")
    public ResponseEntity<?> getOrderById(@PathVariable Long id,
                                          @AuthenticationPrincipal CustomUserDetails currentUser) {
        Optional<Order> order = orderService.findById(id);
        if (order.isPresent()) {
            if (isOwnerOrAdmin(currentUser, order.get())) {
                return success(order.get());
            }
            return redirectForUser(currentUser);
        } else {
            return notFound("订单不存在");
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('CONSUMER')")
    public ResponseEntity<?> createOrder(@RequestBody Order order,
                                         @AuthenticationPrincipal CustomUserDetails currentUser) {
        if (order.getConsumer() == null && currentUser != null
                && "consumer".equalsIgnoreCase(currentUser.getUserType())) {
            Consumer consumer = new Consumer();
            consumer.setId(currentUser.getId());
            order.setConsumer(consumer);
        }
        if (!isOwnerOrAdmin(currentUser, order)) {
            return redirectForUser(currentUser);
        }
        return created(orderService.createOrder(order));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Order> updateOrder(@PathVariable Long id, @RequestBody Order order) {
        order.setId(id);
        return success(orderService.save(order));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        orderService.deleteById(id);
        return success();
    }

    @GetMapping("/consumer/{consumerId}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('CONSUMER') and #consumerId == principal.id)")
    public ResponseEntity<Page<Order>> getOrdersByConsumerId(@PathVariable Long consumerId, Pageable pageable) {
        return success(orderService.findByConsumerId(consumerId, pageable));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<Order>> getOrdersByStatus(@PathVariable String status, Pageable pageable) {
        return success(orderService.findByStatus(status, pageable));
    }

    @GetMapping("/order-no/{orderNo}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CONSUMER')")
    public ResponseEntity<?> getOrderByOrderNo(@PathVariable String orderNo,
                                               @AuthenticationPrincipal CustomUserDetails currentUser) {
        Optional<Order> order = orderService.findByOrderNo(orderNo).stream().findFirst();
        if (order.isPresent()) {
            if (isOwnerOrAdmin(currentUser, order.get())) {
                return success(order.get());
            }
            return redirectForUser(currentUser);
        } else {
            return notFound("订单不存在");
        }
    }

    @GetMapping("/lookup/{lookupId}")
    public ResponseEntity<?> getOrderByLookupId(@PathVariable String lookupId,
                                                @AuthenticationPrincipal CustomUserDetails currentUser) {
        List<Order> orders = orderService.findByConsumerLookupId(lookupId);
        if (orders.isEmpty()) {
            return notFound("订单不存在");
        }

        if (currentUser != null && "consumer".equalsIgnoreCase(currentUser.getUserType())) {
            boolean ownsAny = orders.stream().anyMatch(order -> isOwnerOrAdmin(currentUser, order));
            if (!ownsAny) {
                return redirectForUser(currentUser);
            }
        }

        return success(orders);
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CONSUMER')")
    public ResponseEntity<?> cancelOrder(@PathVariable Long id,
                                         @AuthenticationPrincipal CustomUserDetails currentUser) {
        Optional<Order> order = orderService.findById(id);
        if (order.isPresent() && isOwnerOrAdmin(currentUser, order.get())) {
            orderService.cancelOrder(id);
            return success();
        }
        return redirectForUser(currentUser);
    }

    @PutMapping("/{id}/pay")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CONSUMER')")
    public ResponseEntity<?> payOrder(@PathVariable Long id, @RequestParam String paymentMethod,
                                      @AuthenticationPrincipal CustomUserDetails currentUser) {
        Optional<Order> order = orderService.findById(id);
        if (order.isPresent() && isOwnerOrAdmin(currentUser, order.get())) {
            orderService.payOrder(id, paymentMethod);
            return success();
        }
        return redirectForUser(currentUser);
    }

    @PutMapping("/{id}/revoke")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> revokeOrder(@PathVariable Long id) {
        orderService.revokeOrder(id);
        return success();
    }

    @PutMapping("/{id}/ship")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> shipOrder(@PathVariable Long id) {
        orderService.shipOrder(id);
        return success();
    }

    @PutMapping("/{id}/in-transit")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> markInTransit(@PathVariable Long id) {
        orderService.markInTransit(id);
        return success();
    }

    @PutMapping("/{id}/deliver")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deliverOrder(@PathVariable Long id) {
        orderService.deliverOrder(id);
        return success();
    }

    @PutMapping("/{id}/confirm")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CONSUMER')")
    public ResponseEntity<?> confirmReceipt(@PathVariable Long id,
                                            @AuthenticationPrincipal CustomUserDetails currentUser) {
        Optional<Order> order = orderService.findById(id);
        if (order.isPresent() && isOwnerOrAdmin(currentUser, order.get())) {
            orderService.confirmReceipt(id);
            return success();
        }
        return redirectForUser(currentUser);
    }

    @PutMapping("/{id}/approve-payment")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> approvePayout(@PathVariable Long id) {
        orderService.approvePayout(id);
        return success();
    }

    @PutMapping("/{id}/contact")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CONSUMER')")
    public ResponseEntity<?> updateOrderContact(@PathVariable Long id,
                                                @RequestBody UpdateOrderContactDTO request,
                                                @AuthenticationPrincipal CustomUserDetails currentUser) {
        Optional<Order> existing = orderService.findById(id);
        if (existing.isEmpty()) {
            return notFound("订单不存在");
        }

        Order order = existing.get();
        if (!isOwnerOrAdmin(currentUser, order)) {
            return redirectForUser(currentUser);
        }

        String address = request.getShippingAddress() == null ? null : request.getShippingAddress().trim();
        String name = request.getRecipientName() == null ? null : request.getRecipientName().trim();
        String phone = request.getRecipientPhone() == null ? null : request.getRecipientPhone().trim();

        Order updated = orderService.updateContactInfo(id, address, name, phone);
        Order reloaded = orderService.findOrderDetail(updated.getId());
        return success(toOrderDetailDto(reloaded));
    }

    @GetMapping("/{id}/detail")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CONSUMER')")
    public ResponseEntity<?> getOrderDetail(@PathVariable Long id,
                                            @AuthenticationPrincipal CustomUserDetails currentUser) {
        Order detail = orderService.findOrderDetail(id);
        if (detail == null) {
            return notFound("订单不存在");
        }
        if (isOwnerOrAdmin(currentUser, detail)) {
            return success(toOrderDetailDto(detail));
        }
        return redirectForUser(currentUser);
    }

    private OrderDetailDTO toOrderDetailDto(Order order) {
        OrderDetailDTO dto = new OrderDetailDTO();
        dto.setId(order.getId());
        dto.setOrderNo(order.getOrderNo());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setTotalQuantity(order.getTotalQuantity());
        dto.setStatus(order.getStatus());
        dto.setPaymentMethod(order.getPaymentMethod());
        dto.setShippingAddress(order.getShippingAddress());
        dto.setRecipientName(order.getRecipientName());
        dto.setRecipientPhone(order.getRecipientPhone());
        dto.setOrderTime(order.getOrderTime());
        dto.setPaymentTime(order.getPaymentTime());
        dto.setShippingTime(order.getShippingTime());
        dto.setDeliveryTime(order.getDeliveryTime());
        dto.setInTransitTime(order.getInTransitTime());
        dto.setConsumerConfirmationTime(order.getConsumerConfirmationTime());
        dto.setAdminApprovalTime(order.getAdminApprovalTime());
        dto.setPayoutStatus(order.getPayoutStatus());
        dto.setAdminHoldingAmount(order.getAdminHoldingAmount());
        if (order.getManagingAdmin() != null) {
            dto.setManagingAdminId(order.getManagingAdmin().getId());
            dto.setManagingAdminName(order.getManagingAdmin().getUsername());
        }

        List<OrderItemDetailDTO> items = Optional.ofNullable(order.getOrderItems())
                .orElse(List.of())
                .stream()
                .map(item -> {
                    OrderItemDetailDTO itemDto = new OrderItemDetailDTO();
                    itemDto.setId(item.getId());
                    itemDto.setQuantity(item.getQuantity());
                    itemDto.setUnitPrice(item.getUnitPrice());
                    itemDto.setTotalPrice(item.getTotalPrice());
                    itemDto.setCreatedAt(item.getCreatedAt());

                    OrderItemDetailDTO.OrderedProductDTO productDto = new OrderItemDetailDTO.OrderedProductDTO();
                    if (item.getProduct() != null) {
                        productDto.setId(item.getProduct().getId());
                        productDto.setName(item.getProduct().getName());
                        productDto.setMainImage(item.getProduct().getMainImage());
                    }
                    itemDto.setProduct(productDto);
                    return itemDto;
                })
                .collect(Collectors.toList());
        dto.setOrderItems(items);
        return dto;
    }

    private boolean isOwnerOrAdmin(CustomUserDetails currentUser, Order order) {
        if (currentUser == null) {
            return false;
        }
        if ("admin".equalsIgnoreCase(currentUser.getUserType())) {
            return true;
        }
        if (order.getConsumer() == null) {
            return false;
        }
        return order.getConsumer().getId() != null && order.getConsumer().getId().equals(currentUser.getId());
    }

    private ResponseEntity<?> redirectForUser(CustomUserDetails currentUser) {
        String target = "/login";
        if (currentUser != null) {
            switch (currentUser.getUserType().toLowerCase()) {
                case "admin" -> target = "/admin/overview";
                case "supplier" -> target = "/supplier/workbench";
                case "consumer" -> target = "/consumer/dashboard";
                default -> target = "/login";
            }
        }
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.LOCATION, target);
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }
}
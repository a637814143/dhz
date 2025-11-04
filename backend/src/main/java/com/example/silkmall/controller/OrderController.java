package com.example.silkmall.controller;

import com.example.silkmall.dto.AdminOrderItemDTO;
import com.example.silkmall.dto.AdminOrderSummaryDTO;
import com.example.silkmall.dto.ConsumerOrderSummaryDTO;
import com.example.silkmall.dto.OrderDetailDTO;
import com.example.silkmall.dto.OrderItemDetailDTO;
import com.example.silkmall.dto.SupplierOrderItemDTO;
import com.example.silkmall.dto.SupplierOrderSummaryDTO;
import com.example.silkmall.dto.UpdateOrderContactDTO;
import com.example.silkmall.entity.Consumer;
import com.example.silkmall.entity.Order;
import com.example.silkmall.entity.OrderItem;
import com.example.silkmall.entity.Product;
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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.example.silkmall.common.OrderStatuses.CANCELLED;
import static com.example.silkmall.common.OrderStatuses.PENDING_SHIPMENT;

@RestController
@RequestMapping("/api/orders")
public class OrderController extends BaseController {
    private final OrderService orderService;
    private static final BigDecimal ADMIN_COMMISSION_RATE = new BigDecimal("0.05");
    private static final String RECEIPT_UNCONFIRMED_LABEL = "未收货";
    private static final String RECEIPT_CONFIRMED_LABEL = "已收货";
    private static final String CANCELLED_BILL_LABEL = "账单已取消";
    
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
    @PreAuthorize("hasRole('CONSUMER')")
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
    public ResponseEntity<Page<ConsumerOrderSummaryDTO>> getOrdersByConsumerId(@PathVariable Long consumerId, Pageable pageable) {
        Page<Order> orders = orderService.findByConsumerId(consumerId, pageable);
        Page<ConsumerOrderSummaryDTO> dtoPage = orders.map(this::toConsumerOrderSummary);
        return success(dtoPage);
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<Order>> getOrdersByStatus(@PathVariable String status, Pageable pageable) {
        return success(orderService.findByStatus(status, pageable));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<AdminOrderSummaryDTO>> getOrdersForAdmin(
            @RequestParam(value = "consumerConfirmed", required = false) Boolean consumerConfirmed,
            @RequestParam(value = "orderNo", required = false) String orderNo,
            Pageable pageable) {
        Page<Order> orders = orderService.findAllForAdmin(consumerConfirmed, orderNo, pageable);
        Page<AdminOrderSummaryDTO> dtoPage = orders.map(this::toAdminOrderSummary);
        return success(dtoPage);
    }

    @GetMapping("/supplier/{supplierId}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('SUPPLIER') and #supplierId == principal.id)")
    public ResponseEntity<Page<SupplierOrderSummaryDTO>> getOrdersBySupplier(@PathVariable Long supplierId,
                                                                             Pageable pageable) {
        Page<Order> orders = orderService.findBySupplierId(supplierId, pageable);
        Page<SupplierOrderSummaryDTO> dtoPage = orders.map(order -> toSupplierOrderSummary(order, supplierId));
        return success(dtoPage);
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

    @PutMapping("/{id}/supplier-ship")
    @PreAuthorize("hasRole('SUPPLIER')")
    public ResponseEntity<?> supplierShipOrder(@PathVariable Long id,
                                               @AuthenticationPrincipal CustomUserDetails currentUser) {
        if (currentUser == null) {
            return redirectForUser(null);
        }
        orderService.supplierShipOrder(id, currentUser.getId());
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
    @PreAuthorize("hasRole('CONSUMER')")
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

    private SupplierOrderSummaryDTO toSupplierOrderSummary(Order order, Long supplierId) {
        SupplierOrderSummaryDTO dto = new SupplierOrderSummaryDTO();
        dto.setId(order.getId());
        dto.setOrderNo(order.getOrderNo());
        dto.setStatus(order.getStatus());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setTotalQuantity(order.getTotalQuantity());
        dto.setRecipientName(order.getRecipientName());
        dto.setRecipientPhone(order.getRecipientPhone());
        dto.setShippingAddress(order.getShippingAddress());
        dto.setOrderTime(order.getOrderTime());
        dto.setPaymentTime(order.getPaymentTime());
        dto.setShippingTime(order.getShippingTime());
        dto.setDeliveryTime(order.getDeliveryTime());
        dto.setInTransitTime(order.getInTransitTime());

        List<OrderItem> items = Optional.ofNullable(order.getOrderItems()).orElse(List.of());
        List<SupplierOrderItemDTO> itemDtos = new ArrayList<>();
        BigDecimal supplierAmount = BigDecimal.ZERO;
        int supplierQuantity = 0;
        boolean mixedSuppliers = false;

        for (OrderItem item : items) {
            Product product = item.getProduct();
            Long itemSupplierId = null;
            if (product != null && product.getSupplier() != null) {
                itemSupplierId = product.getSupplier().getId();
            }

            if (itemSupplierId != null && itemSupplierId.equals(supplierId)) {
                itemDtos.add(toSupplierOrderItemDto(item));
                if (item.getQuantity() != null) {
                    supplierQuantity += item.getQuantity();
                }
                if (item.getTotalPrice() != null) {
                    supplierAmount = supplierAmount.add(item.getTotalPrice());
                }
            } else if (itemSupplierId != null && !itemSupplierId.equals(supplierId)) {
                mixedSuppliers = true;
            }
        }

        dto.setItems(itemDtos);
        dto.setSupplierTotalQuantity(supplierQuantity);
        dto.setSupplierTotalAmount(supplierAmount);
        dto.setMixedSuppliers(mixedSuppliers);
        boolean supplierOwnsAllItems = !mixedSuppliers && !itemDtos.isEmpty();
        dto.setCanShip(supplierOwnsAllItems && PENDING_SHIPMENT.equals(order.getStatus()));

        return dto;
    }

    private ConsumerOrderSummaryDTO toConsumerOrderSummary(Order order) {
        ConsumerOrderSummaryDTO dto = new ConsumerOrderSummaryDTO();
        dto.setId(order.getId());
        dto.setOrderNo(order.getOrderNo());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setTotalQuantity(order.getTotalQuantity());
        dto.setStatus(order.getStatus());
        dto.setOrderTime(order.getOrderTime());

        List<String> productNames = Optional.ofNullable(order.getOrderItems()).orElse(List.of()).stream()
                .map(OrderItem::getProduct)
                .filter(product -> product != null && product.getName() != null && !product.getName().isBlank())
                .map(Product::getName)
                .distinct()
                .collect(Collectors.toList());
        dto.setProductNames(productNames);

        return dto;
    }

    private SupplierOrderItemDTO toSupplierOrderItemDto(OrderItem item) {
        SupplierOrderItemDTO dto = new SupplierOrderItemDTO();
        dto.setId(item.getId());
        dto.setQuantity(item.getQuantity());
        dto.setUnitPrice(item.getUnitPrice());
        dto.setTotalPrice(item.getTotalPrice());
        dto.setCreatedAt(item.getCreatedAt());
        if (item.getProduct() != null) {
            dto.setProductId(item.getProduct().getId());
            dto.setProductName(item.getProduct().getName());
            dto.setProductMainImage(item.getProduct().getMainImage());
        }
        return dto;
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

    private AdminOrderSummaryDTO toAdminOrderSummary(Order order) {
        AdminOrderSummaryDTO dto = new AdminOrderSummaryDTO();
        dto.setId(order.getId());
        dto.setOrderNo(order.getOrderNo());
        dto.setStatus(order.getStatus());
        dto.setTotalQuantity(order.getTotalQuantity());

        BigDecimal totalAmount = Optional.ofNullable(order.getTotalAmount()).orElse(BigDecimal.ZERO);
        if (totalAmount.compareTo(BigDecimal.ZERO) < 0) {
            totalAmount = BigDecimal.ZERO;
        }
        dto.setTotalAmount(totalAmount);

        BigDecimal commission = totalAmount.multiply(ADMIN_COMMISSION_RATE).setScale(2, RoundingMode.HALF_UP);
        if (commission.compareTo(totalAmount) > 0) {
            commission = totalAmount;
        }
        BigDecimal supplierAmount = totalAmount.subtract(commission).setScale(2, RoundingMode.HALF_UP);
        if (supplierAmount.compareTo(BigDecimal.ZERO) < 0) {
            supplierAmount = BigDecimal.ZERO;
        }
        dto.setCommissionAmount(commission);
        dto.setSupplierPayoutAmount(supplierAmount);

        boolean consumerConfirmed = order.getConsumerConfirmationTime() != null;
        dto.setConsumerConfirmed(consumerConfirmed);
        dto.setReceiptStatus(consumerConfirmed ? RECEIPT_CONFIRMED_LABEL : RECEIPT_UNCONFIRMED_LABEL);

        boolean cancelled = CANCELLED.equals(order.getStatus());
        dto.setCancelled(cancelled);
        if (cancelled) {
            dto.setCancellationLabel(CANCELLED_BILL_LABEL);
        }

        dto.setPayoutStatus(order.getPayoutStatus());
        dto.setAdminHoldingAmount(order.getAdminHoldingAmount());

        if (order.getManagingAdmin() != null) {
            dto.setManagingAdminName(order.getManagingAdmin().getUsername());
        }
        if (order.getConsumer() != null) {
            dto.setConsumerName(order.getConsumer().getUsername());
        }

        dto.setRecipientName(order.getRecipientName());
        dto.setRecipientPhone(order.getRecipientPhone());
        dto.setShippingAddress(order.getShippingAddress());
        dto.setOrderTime(order.getOrderTime());
        dto.setPaymentTime(order.getPaymentTime());
        dto.setShippingTime(order.getShippingTime());
        dto.setDeliveryTime(order.getDeliveryTime());
        dto.setInTransitTime(order.getInTransitTime());
        dto.setConsumerConfirmationTime(order.getConsumerConfirmationTime());
        dto.setAdminApprovalTime(order.getAdminApprovalTime());

        List<OrderItem> items = Optional.ofNullable(order.getOrderItems()).orElse(List.of());
        List<AdminOrderItemDTO> itemDtos = items.stream()
                .map(this::toAdminOrderItemDto)
                .sorted(Comparator.comparing(AdminOrderItemDTO::getId, Comparator.nullsLast(Long::compareTo)))
                .collect(Collectors.toList());
        dto.setItems(itemDtos);

        return dto;
    }

    private AdminOrderItemDTO toAdminOrderItemDto(OrderItem item) {
        AdminOrderItemDTO dto = new AdminOrderItemDTO();
        dto.setId(item.getId());
        dto.setQuantity(item.getQuantity());
        dto.setUnitPrice(item.getUnitPrice());
        dto.setTotalPrice(item.getTotalPrice());

        Product product = item.getProduct();
        if (product != null) {
            dto.setProductId(product.getId());
            dto.setProductName(product.getName());
            dto.setProductMainImage(product.getMainImage());
            if (product.getSupplier() != null) {
                dto.setSupplierId(product.getSupplier().getId());
                dto.setSupplierName(product.getSupplier().getCompanyName());
            }
        }

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
package com.styliste.controller;

import com.razorpay.RazorpayException;
import com.styliste.dto.*;
import com.styliste.entity.Order;
import com.styliste.entity.OrderStatus;
import com.styliste.entity.PaymentStatus;
import com.styliste.entity.User;
import com.styliste.exception.BadRequestException;
import com.styliste.exception.ResourceNotFoundException;
import com.styliste.repository.OrderRepository;
import com.styliste.repository.UserRepository;
import com.styliste.service.OrderService;
import com.styliste.service.RazorpayService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private RazorpayService razorpayService;


    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<OrderDTO> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            Authentication authentication) {
        log.info("Creating order for authenticated user");

        // Extract user ID from authentication (you may need to customize this based on your implementation)
        Long userId = extractUserIdFromAuth(authentication);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.createOrder(userId, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDTO> getOrderById(@PathVariable Long id) {
        log.info("Fetching order with ID: {}", id);
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderDTO> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        log.info("Updating order status for ID: {}", id);
        return ResponseEntity.ok(orderService.updateOrderStatus(id, request));
    }

    @GetMapping("/{orderId}/timeline")
    public ResponseEntity<List<OrderTimelineDTO>> getTimeline(@PathVariable Long orderId) {
        log.info("Fetching timeline for order ID: {}", orderId);
        return ResponseEntity.ok(orderService.getOrderTimeline(orderId));
    }

    @PostMapping("/{orderId}/pay")
    public ResponseEntity<?> initiatePayment(@PathVariable Long orderId) throws RazorpayException {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getPaymentStatus() != PaymentStatus.PENDING) {
            throw new BadRequestException("Payment already processed");
        }

        String razorpayOrderId = razorpayService.createRazorpayOrder(order);

        return ResponseEntity.ok(Map.of(
                "razorpayOrderId", razorpayOrderId,
                "amount", order.getTotalAmount(),
                "currency", "INR"
        ));
    }

    @PostMapping("/{orderId}/verify-payment")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> verifyPayment(
            @PathVariable Long orderId,
            @Valid @RequestBody PaymentVerificationRequest request) {

        razorpayService.verifyPayment(orderId, request);

        return ResponseEntity.ok(Map.of(
                "message", "Payment verified successfully"
        ));
    }
    @PostMapping("/webhook/razorpay")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Razorpay-Signature") String signature) {

        razorpayService.handleWebhook(payload, signature);
        return ResponseEntity.ok("OK");
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<Page<OrderDTO>> getUserOrders(
            @PathVariable Long userId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize) {
        log.info("Fetching orders for user: {}", userId);
        return ResponseEntity.ok(orderService.getUserOrders(userId, page, pageSize));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<OrderDTO>> getAllOrders(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize) {
        log.info("Fetching all orders");
        return ResponseEntity.ok(orderService.getAllOrders(page, pageSize));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OrderDTO>> getOrdersByStatus(@PathVariable String status) {
        log.info("Fetching orders by status: {}", status);
        try {
            OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
            return ResponseEntity.ok(orderService.getOrdersByStatus(orderStatus));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/track/{trackingNumber}")
    public ResponseEntity<OrderDTO> trackOrder(@PathVariable String trackingNumber) {
        log.info("Tracking order with number: {}", trackingNumber);
        return ResponseEntity.ok(orderService.getOrderByTrackingNumber(trackingNumber));
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderStatisticsDTO> getOrderStatistics() {
        log.info("Fetching order statistics");
        return ResponseEntity.ok(orderService.getOrderStatistics());
    }

    private Long extractUserIdFromAuth(Authentication authentication) {
        // Placeholder - customize based on your token/UserDetails implementation
        String email = authentication.getName();
        User user=userRepository.findByEmail(email)
                .orElseThrow(()-> new ResourceNotFoundException("User Not Found"));

        return user.getId();
    }
}

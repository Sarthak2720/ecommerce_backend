package com.styliste.service;

import com.styliste.dto.*;
import com.styliste.entity.*;
import com.styliste.exception.BadRequestException;
import com.styliste.exception.ResourceNotFoundException;
import com.styliste.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    public OrderDTO createOrder(Long userId, CreateOrderRequest request) {
        log.info("Creating order for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        if (request.getItems().isEmpty()) {
            throw new BadRequestException("Order must contain at least one item");
        }

        Order order = Order.builder()
                .user(user)
                .status(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.PENDING)
                .shippingAddress(request.getShippingAddress())
                .build();

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new java.util.ArrayList<>();

        for (CartItemDTO cartItem : request.getItems()) {
            Product product = productRepository.findById(cartItem.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Product not found with ID: " + cartItem.getProductId()));

            if (product.getStock() < cartItem.getQuantity()) {
                throw new BadRequestException("Insufficient stock for product: " + product.getName());
            }

            BigDecimal effectivePrice = product.getSalePrice() != null ?
                    product.getSalePrice() : product.getPrice();
            BigDecimal itemTotal = effectivePrice.multiply(new BigDecimal(cartItem.getQuantity()));

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(cartItem.getQuantity())
                    .unitPrice(effectivePrice)
                    .totalPrice(itemTotal)
                    .selectedSize(cartItem.getSelectedSize())
                    .selectedColor(cartItem.getSelectedColor())
                    .build();

            orderItems.add(orderItem);
            totalAmount = totalAmount.add(itemTotal);

            // Reduce stock
            product.setStock(product.getStock() - cartItem.getQuantity());
            productRepository.save(product);
        }

        order.setItems(orderItems);
        order.setTotalAmount(totalAmount);

        Order savedOrder = orderRepository.save(order);
        log.info("Order created with ID: {}", savedOrder.getId());

        return mapToDTO(savedOrder);
    }

    public OrderDTO getOrderById(Long id) {
        log.debug("Fetching order with ID: {}", id);
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + id));
        return mapToDTO(order);
    }

    public OrderDTO updateOrderStatus(Long id, UpdateOrderStatusRequest request) {
        log.info("Updating order status for ID: {}", id);

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + id));

        try {
            OrderStatus newStatus = OrderStatus.valueOf(request.getStatus().toUpperCase());
            order.setStatus(newStatus);

            if (request.getTrackingNumber() != null) {
                order.setTrackingNumber(request.getTrackingNumber());
            }
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid order status: " + request.getStatus());
        }

        Order updatedOrder = orderRepository.save(order);
        log.info("Order status updated successfully");
        return mapToDTO(updatedOrder);
    }

    public void updatePaymentStatus(Long id, String paymentStatus) {
        log.info("Updating payment status for order ID: {}", id);

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + id));

        try {
            PaymentStatus status = PaymentStatus.valueOf(paymentStatus.toUpperCase());
            order.setPaymentStatus(status);
            orderRepository.save(order);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid payment status: " + paymentStatus);
        }
    }

    public Page<OrderDTO> getUserOrders(Long userId, Integer page, Integer pageSize) {
        log.debug("Fetching orders for user: {}", userId);

        int pageNum = page != null ? page : 0;
        int size = pageSize != null ? pageSize : 10;

        Pageable pageable = PageRequest.of(pageNum, size, Sort.by("createdAt").descending());
        return orderRepository.findByUserId(userId, pageable).map(this::mapToDTO);
    }

    public List<OrderDTO> getOrdersByStatus(OrderStatus status) {
        log.debug("Fetching orders by status: {}", status);
        return orderRepository.findByStatus(status).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public Page<OrderDTO> getAllOrders(Integer page, Integer pageSize) {
        log.debug("Fetching all orders");

        int pageNum = page != null ? page : 0;
        int size = pageSize != null ? pageSize : 10;

        Pageable pageable = PageRequest.of(pageNum, size, Sort.by("createdAt").descending());
        return orderRepository.findAll(pageable).map(this::mapToDTO);
    }

    public OrderDTO getOrderByTrackingNumber(String trackingNumber) {
        log.debug("Fetching order by tracking number: {}", trackingNumber);
        Order order = orderRepository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with tracking number: " + trackingNumber));
        return mapToDTO(order);
    }

    public OrderStatisticsDTO getOrderStatistics() {
        log.debug("Calculating order statistics");

        long totalOrders = orderRepository.count();
        long pendingOrders = orderRepository.countByStatus(OrderStatus.PENDING);
        long shippedOrders = orderRepository.countByStatus(OrderStatus.SHIPPED);
        long deliveredOrders = orderRepository.countByStatus(OrderStatus.DELIVERED);
        long cancelledOrders = orderRepository.countByStatus(OrderStatus.CANCELLED);
        long processingOrders = orderRepository.countByStatus(OrderStatus.PROCESSING);

        return OrderStatisticsDTO.builder()
                .totalOrders(totalOrders)
                .pendingOrders(pendingOrders)
                .shippedOrders(shippedOrders)
                .deliveredOrders(deliveredOrders)
                .cancelledOrders(cancelledOrders)
                .processingOrders(processingOrders)
                .build();
    }

    private OrderDTO mapToDTO(Order order) {
        List<OrderItemDTO> items = order.getItems().stream()
                .map(item -> OrderItemDTO.builder()
                        .id(item.getId())
                        .productId(item.getProduct().getId())
                        .productName(item.getProduct().getName())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .totalPrice(item.getTotalPrice())
                        .selectedSize(item.getSelectedSize())
                        .selectedColor(item.getSelectedColor())
                        .build())
                .collect(Collectors.toList());

        return OrderDTO.builder()
                .id(order.getId())
                .userId(order.getUser().getId())
                .status(order.getStatus().name())
                .paymentStatus(order.getPaymentStatus().name())
                .totalAmount(order.getTotalAmount())
                .discount(order.getDiscount())
                .tax(order.getTax())
                .trackingNumber(order.getTrackingNumber())
                .shippingAddress(order.getShippingAddress())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .items(items)
                .build();
    }
}

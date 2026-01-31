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

    @Autowired
    private  OrderTimelineRepository timelineRepository;

    public OrderDTO createOrder(Long userId, CreateOrderRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // 1. Extract the specific address the user chose for this order
        AddressDTO addr = request.getShippingAddress();

        // 2. Format the address string for the snapshot
        String addressSnapshot = String.format("%s, %s, %s, %s - %s, %s",
                addr.getAddressLine1(),
                addr.getAddressLine2() != null ? addr.getAddressLine2() : "",
                addr.getCity(),
                addr.getState(),
                addr.getPostalCode(),
                addr.getCountry());

        // 3. Build the order using the Snapshot data
        Order order = Order.builder()
                .user(user)
                .status(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.PENDING)
                .shippingAddress(addressSnapshot) // The full text address
                .userPhone(addr.getContactPhone()) // 👈 This will now work!
                .totalAmount(BigDecimal.ZERO)      // Will be updated below
                .build();

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new java.util.ArrayList<>();

        for (CartItemDTO cartItem : request.getItems()) {
            Product product = productRepository.findById(cartItem.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Product not found with ID: " + cartItem.getProductId()));

            if (product.getStock() <= 0) {
                throw new BadRequestException("The item '" + product.getName() + "' is sold out. Please remove it from your cart.");
            }

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
        order.addTimelineStep(OrderStatus.PENDING, "Order placed successfully.");

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

    @Transactional
    public OrderDTO updateOrderStatus(Long id, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        try {
            String formattedStatus = request.getStatus().trim().toUpperCase();
            OrderStatus newStatus = OrderStatus.valueOf(formattedStatus);
//            OrderStatus newStatus = OrderStatus.valueOf(request.getStatus().toUpperCase());

            // 1. Update Tracking Number if provided
            if (request.getTrackingNumber() != null) {
                order.setTrackingNumber(request.getTrackingNumber());
            }

            // 2. Add to Timeline with a professional message
            String message = (request.getTimelineMessage() != null)
                    ? request.getTimelineMessage()
                    : getDefaultMessage(newStatus);

            order.addTimelineStep(newStatus, message);

        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid status: " + request.getStatus());
        }

        return mapToDTO(orderRepository.save(order));
    }

    public List<OrderTimelineDTO> getOrderTimeline(Long orderId) {
        // Check if order exists first to return a proper error if it doesn't
        if (!orderRepository.existsById(orderId)) {
            throw new ResourceNotFoundException("Order not found with ID: " + orderId);
        }

        List<OrderTimeline> timeline = timelineRepository.findByOrderIdOrderByTimestampAsc(orderId);

        return timeline.stream()
                .map(this::mapToTimelineDTO)
                .collect(Collectors.toList());
    }

    private OrderTimelineDTO mapToTimelineDTO(OrderTimeline entity) {
        return OrderTimelineDTO.builder()
                .status(entity.getStatus().name())
                .message(entity.getMessage())
                .timestamp(entity.getTimestamp())
                .build();
    }

    private String getDefaultMessage(OrderStatus status) {
        return switch (status) {
            case PENDING -> "Order placed successfully.";
            case PROCESSING -> "Your order is under proceeding from seller side by the seller.";
            case SHIPPED -> "Item has been dispatched.";
            case OUT_FOR_DELIVERY -> "Our delivery partner is on the way to your location!";
            case DELIVERED -> "Package delivered successfully.";
            case CANCELLED -> "Order was cancelled.";
            case RETURNED -> "Order got returned to the seller";
            default -> "Order status updated to " + status;
        };
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
                .map(item -> {
                    // Safely get the product and its image list
                    Product product = item.getProduct();
                    List<String> productImages = product.getImages();

                    // Extract the first image URL if it exists
                    String firstImageUrl = (productImages != null && !productImages.isEmpty())
                            ? productImages.get(0)
                            : null;

                    return OrderItemDTO.builder()
                            .id(item.getId())
                            .productId(product.getId())
                            .productName(product.getName())
                            .productImage(firstImageUrl) // 👈 Updated to handle List<String>
                            .quantity(item.getQuantity())
                            .unitPrice(item.getUnitPrice())
                            .totalPrice(item.getTotalPrice())
                            .selectedSize(item.getSelectedSize())
                            .selectedColor(item.getSelectedColor())
                            .build();
                })
                .collect(Collectors.toList());

        return OrderDTO.builder()
                .id(order.getId())
                .userId(order.getUser().getId())
                .userName(order.getUser().getName())
                .userEmail(order.getUser().getEmail())
                .userPhone(order.getUser().getPhone())
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

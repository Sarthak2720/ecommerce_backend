package com.styliste.service;

import com.razorpay.Order; // 👈 Razorpay Order
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.styliste.repository.OrderRepository;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.styliste.dto.PaymentVerificationRequest;
import com.styliste.entity.OrderStatus;
import com.styliste.entity.PaymentStatus;
import com.styliste.exception.BadRequestException;
import com.styliste.exception.ResourceNotFoundException;
import org.apache.commons.codec.digest.HmacUtils;
import org.springframework.beans.factory.annotation.Value;


import java.math.BigDecimal;

@Service
public class RazorpayService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private RazorpayClient razorpayClient;

    @Value("${razorpay.key.secret}")
    private String razorpaySecret;


    public String createRazorpayOrder(com.styliste.entity.Order order)
            throws RazorpayException {

        JSONObject options = new JSONObject();
        options.put("amount", order.getTotalAmount().multiply(new BigDecimal(100))); // paise
        options.put("currency", "INR");
        options.put("receipt", "order_rcpt_" + order.getId());

        Order razorpayOrder = razorpayClient.orders.create(options);

        String razorpayOrderId = razorpayOrder.get("id");

        order.setRazorpayOrderId(razorpayOrderId);
        orderRepository.save(order);

        return razorpayOrderId;
    }

    public void verifyPayment(Long orderId, PaymentVerificationRequest request) {

        com.styliste.entity.Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.getRazorpayOrderId().equals(request.getRazorpayOrderId())) {
            throw new BadRequestException("Razorpay order ID mismatch");
        }

        String payload = request.getRazorpayOrderId() + "|" + request.getRazorpayPaymentId();

        String expectedSignature = HmacUtils.hmacSha256Hex(
                razorpaySecret, payload
        );

        if (!expectedSignature.equals(request.getRazorpaySignature())) {
            order.setPaymentStatus(PaymentStatus.FAILED);
            orderRepository.save(order);
            throw new BadRequestException("Payment verification failed");
        }

        // ✅ PAYMENT VERIFIED
        order.setPaymentStatus(PaymentStatus.COMPLETED);
        order.setStatus(OrderStatus.PROCESSING);
        order.setRazorpayPaymentId(request.getRazorpayPaymentId());
        order.setRazorpaySignature(request.getRazorpaySignature());

        order.addTimelineStep(
                OrderStatus.PROCESSING,
                "Payment successful. Order is now being processed."
        );

        orderRepository.save(order);
    }
    public void handleWebhook(String payload, String signature) {

        String expectedSignature = HmacUtils.hmacSha256Hex(
                razorpaySecret, payload
        );

        if (!expectedSignature.equals(signature)) {
            throw new BadRequestException("Invalid webhook signature");
        }

        JSONObject event = new JSONObject(payload);
        String eventType = event.getString("event");

        if ("payment.captured".equals(eventType)) {
            JSONObject payment = event
                    .getJSONObject("payload")
                    .getJSONObject("payment")
                    .getJSONObject("entity");

            String razorpayOrderId = payment.getString("order_id");
            String paymentId = payment.getString("id");

            com.styliste.entity.Order order =
                    orderRepository.findByRazorpayOrderId(razorpayOrderId)
                            .orElseThrow();

            if (order.getPaymentStatus() != PaymentStatus.COMPLETED) {
                order.setPaymentStatus(PaymentStatus.COMPLETED);
                order.setRazorpayPaymentId(paymentId);
                order.setStatus(OrderStatus.PROCESSING);
                order.addTimelineStep(
                        OrderStatus.PROCESSING,
                        "Payment captured via Razorpay (webhook)"
                );
                orderRepository.save(order);
            }
        }
    }


}

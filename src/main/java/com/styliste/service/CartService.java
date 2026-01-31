package com.styliste.service;

import com.styliste.dto.CartDTO;
import com.styliste.dto.CartItemDTO;
import com.styliste.entity.Cart;
import com.styliste.entity.CartItem;
import com.styliste.entity.Product;
import com.styliste.entity.User;
import com.styliste.exception.BadRequestException;
import com.styliste.exception.ResourceNotFoundException;
import com.styliste.repository.CartRepository;
import com.styliste.repository.ProductRepository;
import com.styliste.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CartService {
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final WishlistService wishlistService; // 👈 Inject WishlistService

    public CartDTO addItemToCart(Long userId, CartItemDTO itemDto) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> createNewCart(userId));

        // 1. Check if same product/size/color combination exists in cart
        Optional<CartItem> existing = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(itemDto.getProductId()) &&
                        Objects.equals(item.getSelectedSize(), itemDto.getSelectedSize()) &&
                        Objects.equals(item.getSelectedColor(), itemDto.getSelectedColor()))
                .findFirst();

        // 2. Fetch product details to check actual current stock
        Product product = productRepository.findById(itemDto.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (existing.isPresent()) {
            // Validation for existing item: check if (current in cart + new request) > stock
            int totalRequested = existing.get().getQuantity() + itemDto.getQuantity();
            if (product.getStock() < totalRequested) {
                throw new BadRequestException("Only " + product.getStock() + " units available. You already have " + existing.get().getQuantity() + " in your cart.");
            }
            existing.get().setQuantity(totalRequested);
        } else {
            // Validation for new item: check if request > stock
            if (product.getStock() < itemDto.getQuantity()) {
                throw new BadRequestException("Only " + product.getStock() + " units available in stock.");
            }

            cart.getItems().add(CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(itemDto.getQuantity())
                    .selectedSize(itemDto.getSelectedSize())
                    .selectedColor(itemDto.getSelectedColor())
                    .build());
        }
        wishlistService.removeFromWishlistInternal(userId, itemDto.getProductId());

        return mapToDTO(cartRepository.save(cart));
    }

    public CartDTO updateItemQuantity(Long userId, Long itemId, Integer quantity) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        CartItem item = cart.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Item not found in cart"));

        // Real-time stock check before updating
        Product product = item.getProduct();
        if (product.getStock() < quantity) {
            throw new BadRequestException("Only " + product.getStock() + " units available in stock.");
        }

        if (quantity <= 0) {
            // If user sets quantity to 0, treat it as a removal
            cart.getItems().remove(item);
        } else {
            item.setQuantity(quantity);
        }

        return mapToDTO(cartRepository.save(cart));
    }
    public CartDTO getCartByUserId(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> createNewCart(userId));
        return mapToDTO(cart);
    }

    public CartDTO mergeCart(Long userId, List<CartItemDTO> guestItems) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> createNewCart(userId));

        for (CartItemDTO guestItem : guestItems) {
            // 1. Find if the exact item already exists in the user's DB cart
            Optional<CartItem> existing = cart.getItems().stream()
                    .filter(item -> item.getProduct().getId().equals(guestItem.getProductId()) &&
                            Objects.equals(item.getSelectedSize(), guestItem.getSelectedSize()) &&
                            Objects.equals(item.getSelectedColor(), guestItem.getSelectedColor()))
                    .findFirst();

            // 2. Fetch real-time product data to check stock
            Product product = productRepository.findById(guestItem.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

            if (existing.isPresent()) {
                // Logic: Existing in DB + Guest Quantity
                int totalRequested = existing.get().getQuantity() + guestItem.getQuantity();

                // 3. Stock Check for Merging
                if (product.getStock() < totalRequested) {
                    // Adjust to max available instead of throwing error to improve UX
                    existing.get().setQuantity(product.getStock());
//                    log.warn("Stock limit reached for product {} during merge. Adjusted to max available: {}",
//                            product.getName(), product.getStock());
                } else {
                    existing.get().setQuantity(totalRequested);
                }
            } else {
                // 4. Stock Check for New Guest Item
                int quantityToAdd = guestItem.getQuantity();
                if (product.getStock() < quantityToAdd) {
                    quantityToAdd = product.getStock(); // Cap it at available stock
                }

                if (quantityToAdd > 0) {
                    cart.getItems().add(CartItem.builder()
                            .cart(cart)
                            .product(product)
                            .quantity(quantityToAdd)
                            .selectedSize(guestItem.getSelectedSize())
                            .selectedColor(guestItem.getSelectedColor())
                            .build());
                }
            }
        }
        return mapToDTO(cartRepository.save(cart));
    }

    public void removeItem(Long userId, Long itemId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));
        cart.getItems().removeIf(item -> item.getId().equals(itemId));
        cartRepository.save(cart);
    }

    public void clearCart(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));
        cart.getItems().clear();
        cartRepository.save(cart);
    }

    // --- PRIVATE HELPER METHODS ---

    private Cart createNewCart(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Cart cart = Cart.builder()
                .user(user)
                .items(new ArrayList<>())
                .build();
        return cartRepository.save(cart);
    }
    private CartDTO mapToDTO(Cart cart) {
        List<CartItemDTO> itemDTOs = cart.getItems().stream()
                .map(item -> {
                    Product product = item.getProduct();
                    int currentStock = product.getStock();
                    boolean adjusted = false;
                    boolean soldOut = false;

                    // 1. Audit logic (as implemented before)
                    if (currentStock <= 0) {
                        if (item.getQuantity() != 0) {
                            item.setQuantity(0);
                            cartRepository.save(cart);
                        }
                        soldOut = true;
                    } else if (currentStock < item.getQuantity()) {
                        item.setQuantity(currentStock);
                        cartRepository.save(cart);
                        adjusted = true;
                    }

                    // 2. Map to DTO
                    CartItemDTO dto = mapItemToDTO(item);
                    dto.setSoldOut(soldOut);
                    dto.setQuantityAdjusted(adjusted);

                    // 3. Force total price to 0 if sold out
                    if (soldOut) {
                        dto.setTotalPrice(BigDecimal.ZERO);
                    }

                    return dto;
                })
                .collect(Collectors.toList());

        // 4. Sum up only valid items
        BigDecimal totalAmount = itemDTOs.stream()
                .map(CartItemDTO::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartDTO.builder()
                .id(cart.getId())
                .userId(cart.getUser().getId())
                .items(itemDTOs)
                .totalAmount(totalAmount) // 👈 This now automatically excludes sold-out items
                .build();
    }
    private CartItemDTO mapItemToDTO(CartItem item) {
        Product product = item.getProduct();
        BigDecimal unitPrice = product.getSalePrice() != null ? product.getSalePrice() : product.getPrice();

        return CartItemDTO.builder()
                .id(item.getId())
                .productId(product.getId())
                .productName(product.getName())
                .productImage(product.getImages() != null && !product.getImages().isEmpty() ? product.getImages().get(0) : null)
                .quantity(item.getQuantity())
                .unitPrice(unitPrice)
                .totalPrice(unitPrice.multiply(BigDecimal.valueOf(item.getQuantity())))
                .selectedSize(item.getSelectedSize())
                .selectedColor(item.getSelectedColor())
                .build();
    }
}
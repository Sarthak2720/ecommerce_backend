package com.styliste.controller;

import com.styliste.dto.CartDTO;
import com.styliste.dto.CartItemDTO;
import com.styliste.entity.User;
import com.styliste.exception.ResourceNotFoundException;
import com.styliste.repository.UserRepository;
import com.styliste.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
@CrossOrigin(origins = "*") // Added to match your other controllers
public class CartController {

    @Autowired
    private CartService cartService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/merge")
    public ResponseEntity<CartDTO> mergeCart(@RequestBody List<CartItemDTO> guestItems, Authentication auth) {
        Long userId = extractUserId(auth);
        return ResponseEntity.ok(cartService.mergeCart(userId, guestItems));
    }

    @GetMapping
    public ResponseEntity<CartDTO> getCart(Authentication auth) {
        return ResponseEntity.ok(cartService.getCartByUserId(extractUserId(auth)));
    }

    @PostMapping("/item")
    public ResponseEntity<CartDTO> addItem(@RequestBody CartItemDTO itemDto, Authentication auth) {
        return ResponseEntity.ok(cartService.addItemToCart(extractUserId(auth), itemDto));
    }

    @DeleteMapping("/item/{itemId}")
    public ResponseEntity<Void> removeItem(@PathVariable Long itemId, Authentication auth) {
        cartService.removeItem(extractUserId(auth), itemId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/item/{itemId}")
    public ResponseEntity<CartDTO> updateQuantity(
            @PathVariable Long itemId,
            @RequestParam Integer quantity,
            Authentication auth) {
        return ResponseEntity.ok(cartService.updateItemQuantity(extractUserId(auth), itemId, quantity));
    }

    // 👈 Added the missing method
    private Long extractUserId(Authentication authentication) {
        String email = authentication.getName(); // Extracts email from JWT/Session
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User Not Found"));
        return user.getId();
    }
}
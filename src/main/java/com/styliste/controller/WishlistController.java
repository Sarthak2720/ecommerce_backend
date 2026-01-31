package com.styliste.controller;

import com.styliste.dto.WishlistDTO;
import com.styliste.exception.ResourceNotFoundException;
import com.styliste.repository.UserRepository;
import com.styliste.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class WishlistController {
    private final WishlistService wishlistService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<WishlistDTO> getWishlist(Authentication auth) {
        return ResponseEntity.ok(wishlistService.getWishlist(extractUserId(auth)));
    }

    @PostMapping("/toggle/{productId}")
    public ResponseEntity<WishlistDTO> toggleItem(@PathVariable Long productId, Authentication auth) {
        return ResponseEntity.ok(wishlistService.toggleWishlist(extractUserId(auth), productId));
    }

    private Long extractUserId(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found")).getId();
    }
}
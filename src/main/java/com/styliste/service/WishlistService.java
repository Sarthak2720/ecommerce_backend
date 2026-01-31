package com.styliste.service;

import com.styliste.dto.ProductDTO;
import com.styliste.dto.WishlistDTO;
import com.styliste.entity.Product;
import com.styliste.entity.User;
import com.styliste.entity.Wishlist;
import com.styliste.exception.ResourceNotFoundException;
import com.styliste.repository.ProductRepository;
import com.styliste.repository.UserRepository;
import com.styliste.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;

@Service
@RequiredArgsConstructor
@Transactional
public class WishlistService {
    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public WishlistDTO getWishlist(Long userId) {
        Wishlist wishlist = wishlistRepository.findByUserId(userId)
                .orElseGet(() -> createNewWishlist(userId));
        return mapToDTO(wishlist);
    }

    public WishlistDTO toggleWishlist(Long userId, Long productId) {
        Wishlist wishlist = wishlistRepository.findByUserId(userId)
                .orElseGet(() -> createNewWishlist(userId));

        // Safety: Ensure a product set is not null
        if (wishlist.getProducts() == null) {
            wishlist.setProducts(new HashSet<>());
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (wishlist.getProducts().contains(product)) {
            wishlist.getProducts().remove(product);
        } else {
            wishlist.getProducts().add(product);
        }

        return mapToDTO(wishlistRepository.save(wishlist));
    }

    // New: Specific method to remove item after it's added to cart
    public void removeFromWishlistInternal(Long userId, Long productId) {
        wishlistRepository.findByUserId(userId).ifPresent(wishlist -> {
            wishlist.getProducts().removeIf(p -> p.getId().equals(productId));
            wishlistRepository.save(wishlist);
        });
    }

    private Wishlist createNewWishlist(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return wishlistRepository.save(Wishlist.builder().user(user).build());
    }

    private WishlistDTO mapToDTO(Wishlist wishlist) {
        return WishlistDTO.builder()
                .id(wishlist.getId())
                .userId(wishlist.getUser().getId())
                .products(wishlist.getProducts().stream()
                        .map(this::mapToProductDTO) // Converts Product Entity -> ProductDTO
                        .toList())
                .build();
    }

    private ProductDTO mapToProductDTO(Product product) {
        // This should match the structure your ProductService uses
        return ProductDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .salePrice(product.getSalePrice())
                .images(product.getImages())
                .build();
    }
}

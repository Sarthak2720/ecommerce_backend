package com.styliste.repository;

import com.styliste.entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
    // Find a wishlist by the User ID
    Optional<Wishlist> findByUserId(Long userId);
}
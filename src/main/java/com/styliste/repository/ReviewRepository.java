package com.styliste.repository;

import com.styliste.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    // Optimized Fetch: Finds non-deleted reviews for a product
    // Pageable is crucial here for performance (e.g., loading 10 reviews at a time)
    Page<Review> findByProductIdAndIsDeletedFalse(Long productId, Pageable pageable);

    // For Admin: Finds all reviews (including deleted ones)
    Page<Review> findByProductId(Long productId, Pageable pageable);

    // Check if user already reviewed this product (prevent spam)
    boolean existsByUserIdAndProductIdAndIsDeletedFalse(Long userId, Long productId);
}
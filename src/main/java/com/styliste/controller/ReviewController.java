package com.styliste.controller;

import com.styliste.dto.ReviewRequestDTO;
import com.styliste.dto.ReviewResponseDTO;
import com.styliste.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    // POST: Add a new review
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReviewResponseDTO> addReview(
            @RequestPart("review") ReviewRequestDTO reviewDTO, // The JSON part
            @RequestPart(value = "images", required = false) List<MultipartFile> images, // The Images
            @RequestPart(value = "videos", required = false) List<MultipartFile> videos  // The Videos
    ) {
        return ResponseEntity.ok(reviewService.createReview(reviewDTO, images, videos));
    }


    // PUT: Update an existing review
    @PutMapping(value = "/{reviewId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReviewResponseDTO> updateReview(
            @PathVariable Long reviewId,
            @RequestPart("review") ReviewRequestDTO reviewDTO,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @RequestPart(value = "videos", required = false) List<MultipartFile> videos
    ) {
        return ResponseEntity.ok(reviewService.updateReview(reviewId, reviewDTO, images, videos));
    }

    // GET: Get reviews for a product (Pagination is built-in!)
    // Example: /api/reviews/product/101?page=0&size=10
    @GetMapping("/product/{productId}")
    public ResponseEntity<Page<ReviewResponseDTO>> getReviews(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        // Sort by Newest First by default
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(reviewService.getProductReviews(productId, pageRequest));
    }

    // DELETE: Admin soft delete review
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long reviewId) {
        reviewService.softDeleteReview(reviewId);
        return ResponseEntity.noContent().build();
    }
}

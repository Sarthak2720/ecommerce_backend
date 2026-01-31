package com.styliste.service;

import com.styliste.dto.ReviewRequestDTO;
import com.styliste.dto.ReviewResponseDTO;
import com.styliste.entity.Review;
import com.styliste.entity.ReviewMedia;
import com.styliste.repository.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private FileStorageService fileStorageService; // Inject your service

    @Transactional
    public ReviewResponseDTO createReview(ReviewRequestDTO request,
                                          List<MultipartFile> images,
                                          List<MultipartFile> videos) {

        // 1. Validation
        if (reviewRepository.existsByUserIdAndProductIdAndIsDeletedFalse(request.getUserId(), request.getProductId())) {
            throw new RuntimeException("You have already reviewed this product.");
        }

        // 2. Build Parent Entity
        Review review = Review.builder()
                .userId(request.getUserId())
                .productId(request.getProductId())
                .orderId(request.getOrderId())
                .rating(request.getRating())
                .title(request.getTitle())
                .body(request.getBody())
                .isDeleted(false)
                .build();

        // 3. Handle File Uploads & Link Entities
        List<ReviewMedia> mediaList = new ArrayList<>();

        // Process Images
        if (images != null && !images.isEmpty()) {
            for (MultipartFile file : images) {
                if (!file.isEmpty()) {
                    String path = fileStorageService.saveFile(file, "image"); // Use your service
                    mediaList.add(ReviewMedia.builder()
                            .review(review)
                            .mediaType(ReviewMedia.MediaType.IMAGE)
                            .url(path) // Stores "/uploads/images/uuid_name.jpg"
                            .build());
                }
            }
        }

        // Process Videos
        if (videos != null && !videos.isEmpty()) {
            for (MultipartFile file : videos) {
                if (!file.isEmpty()) {
                    String path = fileStorageService.saveFile(file, "video"); // Use your service
                    mediaList.add(ReviewMedia.builder()
                            .review(review)
                            .mediaType(ReviewMedia.MediaType.VIDEO)
                            .url(path) // Stores "/uploads/videos/uuid_name.mp4"
                            .build());
                }
            }
        }

        review.setMedia(mediaList);

        // 4. Save to DB
        Review savedReview = reviewRepository.save(review);

        return mapToResponse(savedReview);
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponseDTO> getProductReviews(Long productId, Pageable pageable) {
        // Fetch only active reviews
        return reviewRepository.findByProductIdAndIsDeletedFalse(productId, pageable)
                .map(this::mapToResponse);
    }

    @Transactional
    public void softDeleteReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));

        review.setDeleted(true); // Soft delete
        reviewRepository.save(review);
    }

    // Helper: Map Entity to DTO
    private ReviewResponseDTO mapToResponse(Review review) {
        List<String> imgUrls = new ArrayList<>();
        List<String> vidUrls = new ArrayList<>();

        for (ReviewMedia m : review.getMedia()) {
            if (m.getMediaType() == ReviewMedia.MediaType.IMAGE) imgUrls.add(m.getUrl());
            else vidUrls.add(m.getUrl());
        }

        return ReviewResponseDTO.builder()
                .id(review.getId())
                .title(review.getTitle())
                .body(review.getBody())
                .rating(review.getRating())
                .createdAt(review.getCreatedAt())
                .imageUrls(imgUrls)
                .videoUrls(vidUrls)
                .build();
    }
}
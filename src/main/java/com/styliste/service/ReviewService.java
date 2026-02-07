package com.styliste.service;

import com.styliste.dto.ReviewRequestDTO;
import com.styliste.dto.ReviewResponseDTO;
import com.styliste.entity.Review;
import com.styliste.entity.ReviewMedia;
import com.styliste.entity.User;
import com.styliste.repository.ReviewRepository;
import com.styliste.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private UserRepository userRepository; // Inject this

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

        String reviewerName = userRepository.findById(request.getUserId())
                .map(User::getName)
                .orElse("Anonymous");

        // 6. Return with BOTH arguments
        return mapToResponse(savedReview, reviewerName);
    }

    @Transactional
    public ReviewResponseDTO updateReview(Long reviewId,
                                          ReviewRequestDTO request,
                                          List<MultipartFile> newImages,
                                          List<MultipartFile> newVideos) {

        // 1. Fetch existing review
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));

        // 2. Validation: Ensure the user editing owns the review
        if (!review.getUserId().equals(request.getUserId())) {
            throw new RuntimeException("You are not authorized to edit this review");
        }

        // 3. Update Text Fields (only if provided)
        if (request.getRating() != null) review.setRating(request.getRating());
        if (request.getTitle() != null) review.setTitle(request.getTitle());
        if (request.getBody() != null) review.setBody(request.getBody());

        // Update the timestamp
        review.setUpdatedAt(LocalDateTime.now());

        // 4. Handle Media Deletion (Removing old photos)
        if (request.getMediaIdsToDelete() != null && !request.getMediaIdsToDelete().isEmpty()) {
            review.getMedia().removeIf(media -> {
                if (request.getMediaIdsToDelete().contains(media.getId())) {

                    return true; // Remove from list

                }
                return false;
            });
        }

        // 5. Handle New Media Uploads (Appending new photos/videos)
        List<ReviewMedia> mediaList = review.getMedia(); // Get existing list

        // Add New Images
        if (newImages != null && !newImages.isEmpty()) {
            for (MultipartFile file : newImages) {
                if (!file.isEmpty()) {
                    String path = fileStorageService.saveFile(file, "image");
                    mediaList.add(ReviewMedia.builder()
                            .review(review)
                            .mediaType(ReviewMedia.MediaType.IMAGE)
                            .url(path)
                            .build());
                }
            }
        }

        // Add New Videos
        if (newVideos != null && !newVideos.isEmpty()) {
            for (MultipartFile file : newVideos) {
                if (!file.isEmpty()) {
                    String path = fileStorageService.saveFile(file, "video");
                    mediaList.add(ReviewMedia.builder()
                            .review(review)
                            .mediaType(ReviewMedia.MediaType.VIDEO)
                            .url(path)
                            .build());
                }
            }
        }

        // 6. Save and Return
        Review updatedReview = reviewRepository.save(review);
        String reviewerName = userRepository.findById(updatedReview.getUserId())
                .map(User::getName)
                .orElse("Anonymous");

        // 8. Return with BOTH arguments
        return mapToResponse(updatedReview, reviewerName);
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponseDTO> getProductReviews(Long productId, Pageable pageable) {
        // 1. Fetch the page of reviews
        Page<Review> reviewPage = reviewRepository.findByProductIdAndIsDeletedFalse(productId, pageable);

        // 2. Extract all User IDs from these reviews
        Set<Long> userIds = reviewPage.getContent().stream()
                .map(Review::getUserId)
                .collect(Collectors.toSet());

        // 3. Fetch all names in ONE query (Bulk Fetch)
        // Map<UserId, FullName>
        Map<Long, String> userNames = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(
                        User::getId,
                        User::getName
                ));

        // 4. Map the reviews, passing the looked-up name
        return reviewPage.map(review -> {
            String name = userNames.getOrDefault(review.getUserId(), "Anonymous User");
            return mapToResponse(review, name);
        });
    }

    @Transactional
    public void softDeleteReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));

        review.setDeleted(true); // Soft delete
        reviewRepository.save(review);
    }

    // Helper: Map Entity to DTO
    private ReviewResponseDTO mapToResponse(Review review, String reviewerName) {
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
                .username(reviewerName) // Use the passed string, not the Long ID
                .userId(review.getUserId())
                .orderId(review.getOrderId())
                .rating(review.getRating())
                .createdAt(review.getCreatedAt())
                .imageUrls(imgUrls)
                .videoUrls(vidUrls)
                .build();
    }
}
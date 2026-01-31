package com.styliste.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ReviewResponseDTO {
    private Long id;
    private String title;
    private String body;
    private Integer rating;
    private String username; // You might fetch this from a User Service later
    private LocalDateTime createdAt;
    private List<String> imageUrls;
    private List<String> videoUrls;
}
package com.styliste.dto;

import lombok.Data;
import java.util.List;

@Data
public class ReviewRequestDTO {
    private Long userId;     // In real app, get this from Security Context/Session
    private Long productId;
    private Long orderId;
    private Integer rating;
    private String title;
    private String body;
//    private List<MediaDTO> media; // List of URLs

    @Data
    public static class MediaDTO {
        private String type; // "IMAGE" or "VIDEO"
        private String url;
    }
}
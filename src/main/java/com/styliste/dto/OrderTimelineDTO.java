package com.styliste.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class OrderTimelineDTO {
    private String status;
    private String message;
    private LocalDateTime timestamp;
}
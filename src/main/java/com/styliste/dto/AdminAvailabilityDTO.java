package com.styliste.dto;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
public class AdminAvailabilityDTO {
    private Long id;
    private LocalDate blockedDate;
    private LocalTime blockedTimeStart;
    private LocalTime blockedTimeEnd;
    private Boolean isFullDayBlocked;
    private String reason;
    private String createdByName;
}

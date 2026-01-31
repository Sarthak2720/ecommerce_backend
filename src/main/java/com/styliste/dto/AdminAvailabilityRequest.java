package com.styliste.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminAvailabilityRequest {
    @NotNull(message = "Blocked date is required")
    private LocalDate blockedDate;

    private LocalTime blockedTimeStart;
    private LocalTime blockedTimeEnd;

    @NotNull(message = "Full day blocked flag is required")
    private Boolean isFullDayBlocked;

    private String reason;
}

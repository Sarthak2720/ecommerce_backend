package com.styliste.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentStatisticsDTO {
    private Long totalAppointments;
    private Long pendingAppointments;
    private Long confirmedAppointments;
    private Long completedAppointments;
}

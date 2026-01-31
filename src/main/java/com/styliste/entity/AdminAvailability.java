package com.styliste.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;

@Entity
@Table(name = "admin_availability")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminAvailability {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate blockedDate; // date the block applies to

    // null = whole day blocked
    private LocalTime blockedTimeStart;
    private LocalTime blockedTimeEnd;

    @Column(nullable = false)
    private Boolean isFullDayBlocked = false;

    private String reason;

    @CreationTimestamp
    private ZonedDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;
}

package com.styliste.repository;

import com.styliste.entity.Appointment;
import com.styliste.entity.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    Page<Appointment> findByUserId(Long userId, Pageable pageable);

    List<Appointment> findByAppointmentDate(LocalDate date);

    List<Appointment> findByStatus(AppointmentStatus status);

    List<Appointment> findByGuestEmail(String guestEmail);

    long countByStatus(AppointmentStatus status);

    @Query("""
    select a.appointmentTime 
    from Appointment a 
    where a.appointmentDate = :date 
      and a.status in ('PENDING', 'CONFIRMED')
""")
List<LocalTime> findBookedSlotsByDate(@Param("date") LocalDate date);

    @Modifying
    @Query("""
        UPDATE Appointment a 
        SET a.status = :status,
            a.cancelledBy = :cancelledBy
        WHERE a.id IN :ids
    """)
    void updateStatusAndCancelledBy(
            @Param("ids") List<Long> ids,
            @Param("status") AppointmentStatus status,
            @Param("cancelledBy") String cancelledBy
    );

    // NEW: restore only appointments cancelled due to admin unavailability
    @Modifying
    @Query("""
        UPDATE Appointment a
        SET a.status = :status,
            a.cancelledBy = null
        WHERE a.appointmentDate = :date
          AND a.status = 'CANCELLED'
          AND a.cancelledBy = 'ADMIN_UNAVAILABLE'
    """)
    int restoreAppointmentsForDate(
            @Param("date") LocalDate date,
            @Param("status") AppointmentStatus status
    );
    
}

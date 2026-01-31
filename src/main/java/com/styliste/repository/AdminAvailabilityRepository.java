package com.styliste.repository;

import com.styliste.entity.AdminAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface AdminAvailabilityRepository extends JpaRepository<AdminAvailability, Long> {
    List<AdminAvailability> findByBlockedDate(LocalDate date);
    List<AdminAvailability> findByBlockedDateBetween(LocalDate start, LocalDate end);

    @Query("""
        SELECT COUNT(a) > 0 FROM AdminAvailability a
         WHERE a.blockedDate = :date
         AND (
            a.isFullDayBlocked = true
             OR (
                a.blockedTimeStart <= :time
                 AND a.blockedTimeEnd > :time
            )
        )
    """)
    boolean isTimeSlotBlocked(
            @Param("date") LocalDate date,
            @Param("time") LocalTime time
    );

    void deleteByBlockedDateBefore(LocalDate date);
}

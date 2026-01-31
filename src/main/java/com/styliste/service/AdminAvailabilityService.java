package com.styliste.service;

import com.styliste.dto.AdminAvailabilityDTO;
import com.styliste.dto.AdminAvailabilityRequest;
import com.styliste.dto.AffectedAppointmentDTO;
import com.styliste.entity.AdminAvailability;
import com.styliste.entity.Appointment;
import com.styliste.entity.User;
import com.styliste.exception.BadRequestException;
import com.styliste.exception.ResourceNotFoundException;
import com.styliste.repository.AdminAvailabilityRepository;
import com.styliste.repository.AppointmentRepository;
import com.styliste.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.styliste.entity.AppointmentStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AdminAvailabilityService {

    @Autowired
    private AdminAvailabilityRepository availabilityRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Transactional
    public AdminAvailabilityDTO createUnavailability(
            AdminAvailabilityRequest request,
            Long adminId
    ) {
        log.info("Creating unavailability for date: {}", request.getBlockedDate());

        LocalDate maxAllowedDate = LocalDate.now().plusDays(15);

        if (request.getBlockedDate().isAfter(maxAllowedDate)) {
            throw new BadRequestException(
                    "You can block availability only up to the next 15 days"
            );
        }

        // Validate date is not in the past
        if (request.getBlockedDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("Cannot block dates in the past");
        }

        // Validate time range if not full day
        if (!request.getIsFullDayBlocked()) {
            if (request.getBlockedTimeStart() == null || request.getBlockedTimeEnd() == null) {
                throw new BadRequestException("Time range required when not blocking full day");
            }
            if (request.getBlockedTimeEnd().isBefore(request.getBlockedTimeStart())) {
                throw new BadRequestException("End time must be after start time");
            }
        }

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));

        AdminAvailability availability = AdminAvailability.builder()
                .blockedDate(request.getBlockedDate())
                .blockedTimeStart(request.getBlockedTimeStart())
                .blockedTimeEnd(request.getBlockedTimeEnd())
                .isFullDayBlocked(request.getIsFullDayBlocked())
                .reason(request.getReason())
                .createdBy(admin)
                .build();

        AdminAvailability saved = availabilityRepository.save(availability);

        // Find affected appointment ENTITIES (so we can both notify and cancel them)
        List<Appointment> affectedEntities = findAffectedAppointmentEntities(saved);

        // Map entities to DTOs for emails
        List<AffectedAppointmentDTO> affectedAppointments = mapToAffectedDTOs(affectedEntities);

        if (!affectedAppointments.isEmpty()) {
            // Send apology email first (keeps user notified)
            notifyAffectedCustomers(affectedAppointments, saved);

            // Then cancel their appointments in DB (bulk update)
            cancelAffectedAppointments(affectedEntities);
        }

        return mapToDTO(saved);
    }

    /**
     * Find appointment entities affected by this availability block.
     */
    private List<Appointment> findAffectedAppointmentEntities(AdminAvailability availability) {
        List<Appointment> appointments = appointmentRepository
                .findByAppointmentDate(availability.getBlockedDate());

        return appointments.stream()
                .filter(apt -> {
                    // If full day blocked, all appointments affected
                    if (availability.getIsFullDayBlocked()) {
                        return true;
                    }

                    // Check if appointment time falls in blocked range
                    LocalTime aptTime = apt.getAppointmentTime();
                    return !aptTime.isBefore(availability.getBlockedTimeStart())
                            && aptTime.isBefore(availability.getBlockedTimeEnd());
                })
                .collect(Collectors.toList());
    }

    /**
     * Map appointment entities to the DTO used for email notification.
     */
    private List<AffectedAppointmentDTO> mapToAffectedDTOs(List<Appointment> appointments) {
        return appointments.stream()
                .map(apt -> AffectedAppointmentDTO.builder()
                        .appointmentId(apt.getId())
                        .customerName(apt.getUser() != null
                                ? apt.getUser().getName()
                                : apt.getGuestName())
                        .customerEmail(apt.getUser() != null
                                ? apt.getUser().getEmail()
                                : apt.getGuestEmail())
                        .appointmentDate(apt.getAppointmentDate())
                        .appointmentTime(apt.getAppointmentTime())
                        .serviceType(apt.getServiceType().name())
                        .build())
                .collect(Collectors.toList());
    }

    private void notifyAffectedCustomers(
            List<AffectedAppointmentDTO> affected,
            AdminAvailability availability
    ) {
        // Find next 3 available slots as alternatives
        List<String> alternativeSlots = findAlternativeSlots(
                availability.getBlockedDate()
        );

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");

        for (AffectedAppointmentDTO apt : affected) {
            String dateStr = apt.getAppointmentDate().format(dateFormatter);
            String timeStr = apt.getAppointmentTime().format(timeFormatter);

            emailService.sendUnavailabilityApologyEmail(
                    apt.getCustomerEmail(),
                    apt.getCustomerName(),
                    dateStr,
                    timeStr,
                    alternativeSlots
            );

            log.info("Sent apology email to {} for affected appointment {}",
                    apt.getCustomerEmail(), apt.getAppointmentId());
        }
    }

    /**
     * Cancel affected appointments in bulk.
     * We perform a bulk update using appointmentRepository.updateStatusForIds(...)
     */
    private void cancelAffectedAppointments(List<Appointment> affected) {
        if (affected == null || affected.isEmpty()) return;

        List<Long> ids = affected.stream()
                .map(Appointment::getId)
                .toList();

        appointmentRepository.updateStatusAndCancelledBy(
                ids,
                AppointmentStatus.CANCELLED,
                "ADMIN_UNAVAILABLE"
        );

        log.warn("Cancelled {} appointments due to admin unavailability", ids.size());
    }


    private List<String> findAlternativeSlots(LocalDate blockedDate) {
        List<String> alternatives = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a");

        // Check next 7 days
        for (int i = 1; i <= 7; i++) {
            LocalDate checkDate = blockedDate.plusDays(i);

            // Get available slots for this date
            List<LocalTime> slots = getAvailableSlotsForDate(checkDate);

            for (LocalTime slot : slots) {
                if (alternatives.size() >= 5) break; // Limit to 5 suggestions

                String slotStr = checkDate.atTime(slot).format(formatter);
                alternatives.add(slotStr);
            }

            if (alternatives.size() >= 5) break;
        }

        return alternatives;
    }

    private List<LocalTime> getAvailableSlotsForDate(LocalDate date) {
        List<LocalTime> allSlots = List.of(
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                LocalTime.of(12, 0),
                LocalTime.of(14, 0),
                LocalTime.of(15, 0),
                LocalTime.of(16, 0),
                LocalTime.of(17, 0),
                LocalTime.of(18, 0)
        );

        // Get booked slots
        List<LocalTime> bookedSlots = appointmentRepository.findBookedSlotsByDate(date);

        // Get blocked slots
        List<AdminAvailability> blocks = availabilityRepository.findByBlockedDate(date);

        return allSlots.stream()
                .filter(slot -> !bookedSlots.contains(slot))
                .filter(slot -> {
                    for (AdminAvailability block : blocks) {
                        if (block.getIsFullDayBlocked()) return false;

                        if (!slot.isBefore(block.getBlockedTimeStart())
                                && slot.isBefore(block.getBlockedTimeEnd())) {
                            return false;
                        }
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    public List<AdminAvailabilityDTO> getUpcomingUnavailability() {
        LocalDate today = LocalDate.now();
        LocalDate fifteenDaysLater  = today.plusDays(15);

        return availabilityRepository
                .findByBlockedDateBetween(today, fifteenDaysLater )
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<AdminAvailabilityDTO> getUnavailabilityForDate(LocalDate date) {
        return availabilityRepository
                .findByBlockedDate(date)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteUnavailability(Long id) {
        AdminAvailability availability = availabilityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Unavailability not found"));

        LocalDate date = availability.getBlockedDate();

        availabilityRepository.delete(availability);
        log.info("Deleted unavailability for date: {}", date);

        // Restore appointments cancelled due to this block
        int restoredCount = appointmentRepository.restoreAppointmentsForDate(
                date,
                AppointmentStatus.CONFIRMED
        );

        if (restoredCount > 0) {
            notifyRestoredAppointments(date, restoredCount);
        }
    }

    private void notifyRestoredAppointments(LocalDate date, int count) {
        List<Appointment> restoredAppointments =
                appointmentRepository.findByAppointmentDate(date)
                        .stream()
                        .filter(a ->
                                a.getStatus() == AppointmentStatus.CONFIRMED &&
                                        a.getCancelledBy() == null
                        )
                        .toList();

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");

        for (Appointment apt : restoredAppointments) {
            String name = apt.getUser() != null
                    ? apt.getUser().getName()
                    : apt.getGuestName();

            String email = apt.getUser() != null
                    ? apt.getUser().getEmail()
                    : apt.getGuestEmail();

            emailService.sendAvailabilityRestoredEmail(
                    email,
                    name,
                    apt.getAppointmentDate().format(dateFormatter),
                    apt.getAppointmentTime().format(timeFormatter),
                    apt.getServiceType().name()
            );
        }

        log.info("Notified {} customers about restored availability on {}", count, date);
    }


    private AdminAvailabilityDTO mapToDTO(AdminAvailability entity) {
        return AdminAvailabilityDTO.builder()
                .id(entity.getId())
                .blockedDate(entity.getBlockedDate())
                .blockedTimeStart(entity.getBlockedTimeStart())
                .blockedTimeEnd(entity.getBlockedTimeEnd())
                .isFullDayBlocked(entity.getIsFullDayBlocked())
                .reason(entity.getReason())
                .createdByName(entity.getCreatedBy() != null
                        ? entity.getCreatedBy().getName()
                        : null)
                .build();
    }
}

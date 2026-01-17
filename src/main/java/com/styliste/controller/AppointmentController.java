package com.styliste.controller;

import com.styliste.dto.*;
import com.styliste.entity.ServiceType;
import com.styliste.entity.User;
import com.styliste.exception.ResourceNotFoundException;
import com.styliste.repository.UserRepository;
import com.styliste.service.AppointmentService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/appointments")
@CrossOrigin(origins = "*")
public class AppointmentController {

    @Autowired
    private AppointmentService appointmentService;
    @Autowired
    private UserRepository userRepository;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<AppointmentDTO>> createAppointment(
            @Valid @RequestBody CreateAppointmentRequest request,
            Authentication authentication) {

        Long userId = extractUserIdFromAuth(authentication);

        AppointmentDTO dto = appointmentService.createAppointment(userId, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<AppointmentDTO>builder()
                        .success(true)
                        .message("Appointment request submitted. You will receive confirmation via email.")
                        .data(dto)
                        .build());
    }

    @GetMapping("/available-slots")
    public ResponseEntity<List<LocalTime>> getAvailableSlots(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        return ResponseEntity.ok(
                appointmentService.getAvailableSlots(date)
        );
    }



    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AppointmentDTO> approveAppointment(@PathVariable Long id) {
        return ResponseEntity.ok(appointmentService.approveAppointment(id));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AppointmentDTO> rejectAppointment(@PathVariable Long id) {
        return ResponseEntity.ok(appointmentService.rejectAppointment(id));
    }

    @PostMapping("/guest")
    public ResponseEntity<ApiResponse<AppointmentDTO>> bookGuestAppointment(
            @Valid @RequestBody CreateGuestAppointmentRequest request) {

        AppointmentDTO dto = appointmentService.createGuestAppointment(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<AppointmentDTO>builder()
                        .success(true)
                        .message("Your appointment request has been received. You will get a confirmation email after admin approval.")
                        .data(dto)
                        .build());
    }


    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')") // 1. Role Check
    public ResponseEntity<AppointmentDTO> getAppointmentById(
            @PathVariable Long id,
            Authentication authentication) { // 2. Inject Auth

        log.info("Fetching appointment with ID: {}", id);

        // Step 1: Fetch the appointment details
        AppointmentDTO appointment = appointmentService.getAppointmentById(id);

        // Step 2: Who is asking?
        Long currentUserId = extractUserIdFromAuth(authentication);
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        // Step 3: The "Ownership" Check
        // If I am NOT an Admin AND the appointment is NOT mine...
        if (!isAdmin && !appointment.getUserId().equals(currentUserId)) {
            log.warn("User {} tried to access someone else's appointment {}", currentUserId, id);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build(); // 🛑 BLOCK THEM
        }

        return ResponseEntity.ok(appointment);
    }

    @GetMapping("/types")
    public ResponseEntity<List<ServiceType>> getAllServiceTypes() {
        return ResponseEntity.ok(Arrays.asList(ServiceType.values()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AppointmentDTO> updateAppointment(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAppointmentRequest request) {
        log.info("Updating appointment with ID: {}", id);
        return ResponseEntity.ok(appointmentService.updateAppointment(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<String> cancelAppointment(@PathVariable Long id) {
        log.info("Cancelling appointment with ID: {}", id);
        appointmentService.cancelAppointment(id);
        return ResponseEntity.ok("Appointment cancelled successfully");
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<Page<AppointmentDTO>> getUserAppointments(
            @PathVariable Long userId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize,
            Authentication authentication) { // 👈 Add Authentication here

        // Security Check: If they are NOT admin, enforce that they can only see THEIR OWN data
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        Long currentUserId = extractUserIdFromAuth(authentication);

        if (!isAdmin && !currentUserId.equals(userId)) {
            // If I am User 1 trying to see User 2's data -> BLOCK THEM
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        log.info("Fetching appointments for user: {}", userId);
        return ResponseEntity.ok(appointmentService.getUserAppointments(userId, page, pageSize));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<AppointmentDTO>> getAllAppointments(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize) {
        log.info("Fetching all appointments");
        return ResponseEntity.ok(appointmentService.getAllAppointments(page, pageSize));
    }

    @GetMapping("/date/{date}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AppointmentDTO>> getAppointmentsByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        log.info("Fetching appointments for date: {}", date);
        return ResponseEntity.ok(appointmentService.getAppointmentsByDate(date));
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AppointmentStatisticsDTO> getAppointmentStatistics() {
        log.info("Fetching appointment statistics");
        return ResponseEntity.ok(appointmentService.getAppointmentStatistics());
    }

    private Long extractUserIdFromAuth(Authentication authentication) {
        // Placeholder - customize based on your token/UserDetails implementation
        String email = authentication.getName();
        User user=userRepository.findByEmail(email)
                .orElseThrow(()-> new ResourceNotFoundException("User Not Found"));

        return user.getId();
    }
}

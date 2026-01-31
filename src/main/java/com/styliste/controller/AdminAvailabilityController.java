package com.styliste.controller;

import com.styliste.dto.AdminAvailabilityDTO;
import com.styliste.dto.AdminAvailabilityRequest;
import com.styliste.dto.ApiResponse;
import com.styliste.entity.User;
import com.styliste.exception.ResourceNotFoundException;
import com.styliste.repository.UserRepository;
import com.styliste.service.AdminAvailabilityService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin/availability")
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAvailabilityController {

    @Autowired
    private AdminAvailabilityService availabilityService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping
    public ResponseEntity<ApiResponse<AdminAvailabilityDTO>> createUnavailability(
            @Valid @RequestBody AdminAvailabilityRequest request,
            Authentication authentication
    ) {
        Long adminId = extractUserIdFromAuth(authentication);

        AdminAvailabilityDTO dto = availabilityService.createUnavailability(request, adminId);

        return ResponseEntity.ok(
                ApiResponse.<AdminAvailabilityDTO>builder()
                        .success(true)
                        .message("Unavailability created successfully")
                        .data(dto)
                        .build()
        );
    }

    @GetMapping("/upcoming")
    public ResponseEntity<List<AdminAvailabilityDTO>> getUpcomingUnavailability() {
        return ResponseEntity.ok(availabilityService.getUpcomingUnavailability());
    }

    @GetMapping("/date/{date}")
    public ResponseEntity<List<AdminAvailabilityDTO>> getUnavailabilityForDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(availabilityService.getUnavailabilityForDate(date));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUnavailability(@PathVariable Long id) {
        availabilityService.deleteUnavailability(id);

        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Unavailability removed successfully")
                        .build()
        );
    }

    private Long extractUserIdFromAuth(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User Not Found"));
        return user.getId();
    }
}
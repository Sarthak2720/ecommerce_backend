package com.styliste.service;

import com.styliste.dto.*;
import com.styliste.entity.Appointment;
import com.styliste.entity.AppointmentStatus;
import com.styliste.entity.ServiceType;
import com.styliste.entity.User;
import com.styliste.exception.BadRequestException;
import com.styliste.exception.ResourceNotFoundException;
import com.styliste.repository.AppointmentRepository;
import com.styliste.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class AppointmentService {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private EmailService emailService;


    @Autowired
    private UserRepository userRepository;

    public AppointmentDTO createAppointment(Long userId, CreateAppointmentRequest request) {
        log.info("Creating appointment for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        if (request.getAppointmentDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("Appointment date cannot be in the past");
        }

        try {
            ServiceType serviceType = ServiceType.valueOf(request.getServiceType().toUpperCase());

            Appointment appointment = Appointment.builder()
                    .user(user)
                    .guestName(user.getName())
                    .guestEmail(user.getEmail())
                    .guestPhone(user.getPhone())
                    .appointmentDate(request.getAppointmentDate())
                    .appointmentTime(request.getAppointmentTime())
                    .serviceType(serviceType)
                    .notes(request.getNotes())
                    .status(AppointmentStatus.PENDING)
                    .build();

            Appointment savedAppointment = appointmentRepository.save(appointment);
            log.info("Appointment created with ID: {}", savedAppointment.getId());

            return mapToDTO(savedAppointment);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid service type: " + request.getServiceType());
        }
    }

    public AppointmentDTO approveAppointment(Long id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        if (appointment.getStatus() != AppointmentStatus.PENDING) {
            throw new BadRequestException("Only pending appointments can be approved");
        }

        appointment.setStatus(AppointmentStatus.CONFIRMED);
        Appointment saved = appointmentRepository.save(appointment);

        // 📧 email user
        emailService.sendAppointmentApprovedEmail(saved);

        return mapToDTO(saved);
    }

    public List<LocalTime> getAvailableSlots(LocalDate date) {

        // 🔹 Define all working slots (single source of truth)
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

        // 🔹 Fetch already booked slots
        List<LocalTime> bookedSlots =
                appointmentRepository.findBookedSlotsByDate(date);

        // 🔹 Remove booked ones
        return allSlots.stream()
                .filter(slot -> !bookedSlots.contains(slot))
                .toList();
    }

    @Transactional
    public AppointmentDTO createGuestAppointment(CreateGuestAppointmentRequest request) {

        if (request.getAppointmentDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("Appointment date cannot be in the past");
        }

        ServiceType serviceType;
        try {
            serviceType = ServiceType.valueOf(request.getServiceType().toUpperCase());
        } catch (Exception e) {
            throw new BadRequestException("Invalid service type");
        }

        Appointment appointment = Appointment.builder()
                .guestName(request.getGuestName())
                .guestEmail(request.getGuestEmail())
                .guestPhone(request.getGuestPhone())
                .appointmentDate(request.getAppointmentDate())
                .appointmentTime(request.getAppointmentTime())
                .serviceType(serviceType)
                .notes(request.getNotes())
                .status(AppointmentStatus.PENDING)
                .build();

        Appointment saved = appointmentRepository.save(appointment);

        // 📧 Send email: "Request received"
//        emailService.sendAppointmentPendingEmail(saved);

        return mapToDTO(saved);
    }


    public AppointmentDTO rejectAppointment(Long id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        if (appointment.getStatus() != AppointmentStatus.PENDING) {
            throw new BadRequestException("Only pending appointments can be rejected");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        Appointment saved = appointmentRepository.save(appointment);

        // 📧 email user
        emailService.sendAppointmentRejectedEmail(saved);

        return mapToDTO(saved);
    }

    @Transactional
    public void linkAppointmentsToUser(User user) {

        List<Appointment> appointments =
                appointmentRepository.findByGuestEmail(user.getEmail());

        for (Appointment appointment : appointments) {
            appointment.setUser(user);
        }

        appointmentRepository.saveAll(appointments);
    }



    public AppointmentDTO getAppointmentById(Long id) {
        log.debug("Fetching appointment with ID: {}", id);
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + id));
        return mapToDTO(appointment);
    }

    public AppointmentDTO updateAppointment(Long id, UpdateAppointmentRequest request) {
        log.info("Updating appointment with ID: {}", id);

        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + id));

        if (request.getStatus() != null) {
            try {
                AppointmentStatus status = AppointmentStatus.valueOf(request.getStatus().toUpperCase());
                appointment.setStatus(status);
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException("Invalid appointment status: " + request.getStatus());
            }
        }

        if (request.getNotes() != null) {
            appointment.setNotes(request.getNotes());
        }

        Appointment updatedAppointment = appointmentRepository.save(appointment);
        log.info("Appointment updated successfully");
        return mapToDTO(updatedAppointment);
    }

    public void cancelAppointment(Long id) {
        log.info("Cancelling appointment with ID: {}", id);

        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + id));

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointmentRepository.save(appointment);
    }

    public Page<AppointmentDTO> getUserAppointments(Long userId, Integer page, Integer pageSize) {
        log.debug("Fetching appointments for user: {}", userId);

        int pageNum = page != null ? page : 0;
        int size = pageSize != null ? pageSize : 10;

        Pageable pageable = PageRequest.of(pageNum, size, Sort.by("appointmentDate").ascending());
        return appointmentRepository.findByUserId(userId, pageable).map(this::mapToDTO);
    }

    public List<AppointmentDTO> getAppointmentsByDate(LocalDate date) {
        log.debug("Fetching appointments for date: {}", date);
        return appointmentRepository.findByAppointmentDate(date).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public Page<AppointmentDTO> getAllAppointments(Integer page, Integer pageSize) {
        log.debug("Fetching all appointments");

        int pageNum = page != null ? page : 0;
        int size = pageSize != null ? pageSize : 10;

        Pageable pageable = PageRequest.of(pageNum, size, Sort.by("appointmentDate").ascending());
        return appointmentRepository.findAll(pageable).map(this::mapToDTO);
    }

    public AppointmentStatisticsDTO getAppointmentStatistics() {
        log.debug("Calculating appointment statistics");

        long totalAppointments = appointmentRepository.count();
        long pendingAppointments = appointmentRepository.countByStatus(AppointmentStatus.PENDING);
        long confirmedAppointments = appointmentRepository.countByStatus(AppointmentStatus.CONFIRMED);
        long completedAppointments = appointmentRepository.countByStatus(AppointmentStatus.COMPLETED);

        return AppointmentStatisticsDTO.builder()
                .totalAppointments(totalAppointments)
                .pendingAppointments(pendingAppointments)
                .confirmedAppointments(confirmedAppointments)
                .completedAppointments(completedAppointments)
                .build();
    }

    private AppointmentDTO mapToDTO(Appointment appointment) {

        boolean isGuest = appointment.getUser() == null;

        return AppointmentDTO.builder()
                .id(appointment.getId())

                // ✅ Name: user OR guest
                .name(isGuest
                        ? appointment.getGuestName()
                        : appointment.getUser().getName())

                // ✅ userId: null for guest
                .userId(isGuest
                        ? null
                        : appointment.getUser().getId())

                .appointmentDate(appointment.getAppointmentDate())
                .appointmentTime(appointment.getAppointmentTime())
                .serviceType(appointment.getServiceType().name())
                .notes(appointment.getNotes())
                .status(appointment.getStatus().name())
                .createdAt(appointment.getCreatedAt())
                .build();
    }

}

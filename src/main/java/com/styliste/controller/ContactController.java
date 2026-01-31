package com.styliste.controller;

import com.styliste.entity.ContactMessage;
import com.styliste.entity.MessageStatus;
import com.styliste.repository.ContactMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contact")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ContactController {

    private final ContactMessageRepository contactRepository;

    // 1. PUBLIC: Send a message
    @PostMapping
    public ResponseEntity<String> sendMessage(@RequestBody ContactMessage message) {
        contactRepository.save(message);
        return ResponseEntity.ok("Message sent successfully!");
    }

    // 2. ADMIN: View all enquiries
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ContactMessage>> getAllMessages() {
        return ResponseEntity.ok(contactRepository.findAll());
    }

    // 3. ADMIN: Mark as read/resolved
    @PatchMapping("/admin/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ContactMessage> updateStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        ContactMessage msg = contactRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Message not found"));
        msg.setStatus(MessageStatus.valueOf(status.toUpperCase()));
        return ResponseEntity.ok(contactRepository.save(msg));
    }
}
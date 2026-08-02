package com.example.demo.tutor.controller;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.example.demo.tutor.dto.TutorBillingSummaryDTO;
import com.example.demo.tutor.service.TutorBillingService;

@RestController
@RequestMapping("/api/tutor/billing")
public class TutorBillingController {

    private final TutorBillingService billingService;

    @Autowired
    public TutorBillingController(TutorBillingService billingService) {
        this.billingService = billingService;
    }

    /**
     * Get billing summary for current tutor or specific tutorId
     */
    @GetMapping("/summary")
    public ResponseEntity<TutorBillingSummaryDTO> getSummary(
            @RequestParam(required = false) String tutorId,
            Authentication authentication) {

        if (tutorId != null && !tutorId.isBlank()) {
            return ResponseEntity.ok(billingService.getBillingSummaryByTutorId(tutorId));
        }

        String tutorEmail = authentication.getName();
        return ResponseEntity.ok(billingService.getBillingSummary(tutorEmail));
    }

    /**
     * Process payment for tutor subscription access
     */
    @PostMapping("/pay")
    public ResponseEntity<TutorBillingSummaryDTO> processPayment(
            @RequestBody Map<String, Object> body,
            Authentication authentication) {

        String tutorEmail = authentication.getName();
        String paymentMethod = (String) body.getOrDefault("paymentMethod", "CARD");
        String paymentReference = (String) body.get("paymentReference");
        Double amount = body.get("amount") != null ? Double.parseDouble(body.get("amount").toString()) : 0.0;

        TutorBillingSummaryDTO updated = billingService.processPayment(tutorEmail, paymentMethod, paymentReference, amount);
        return ResponseEntity.ok(updated);
    }

    /**
     * Get billing overview for all tutors (Admin / Super Admin view)
     */
    @GetMapping("/all")
    public ResponseEntity<List<TutorBillingSummaryDTO>> getAllTutorsBilling() {
        return ResponseEntity.ok(billingService.getAllTutorsBillingSummary());
    }
}

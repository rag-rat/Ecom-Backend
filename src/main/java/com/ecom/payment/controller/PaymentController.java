package com.ecom.payment.controller;

import com.ecom.common.dto.ApiResponse;
import com.ecom.payment.dto.CreatePaymentOrderRequest;
import com.ecom.payment.dto.CreatePaymentOrderResponse;
import com.ecom.payment.dto.PaymentResponse;
import com.ecom.payment.dto.VerifyPaymentRequest;
import com.ecom.payment.service.PaymentService;
import com.ecom.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payments", description = "Razorpay payment integration")
public class PaymentController {

    private final PaymentService paymentService;


    // createPaymentOrder()

    @PostMapping("/create-order")
    @Operation(summary = "Create a Razorpay payment session for a pending order")
    public ResponseEntity<ApiResponse<CreatePaymentOrderResponse>> createPaymentOrder(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreatePaymentOrderRequest request) {

        CreatePaymentOrderResponse response =
                paymentService.createPaymentOrder(userDetails.getUsername(), request);

        return ResponseEntity.ok(ApiResponse.success("Payment session created", response));
    }



    // verifyPayment()

    @PostMapping("/verify")
    @Operation(summary = "Verify a completed payment (client-side callback path)")
    public ResponseEntity<ApiResponse<PaymentResponse>> verifyPayment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody VerifyPaymentRequest request) {

        PaymentResponse response =
                paymentService.verifyPayment(userDetails.getUsername(), request);

        return ResponseEntity.ok(ApiResponse.success("Payment verified successfully", response));
    }



    // handleWebhook()

    @PostMapping("/webhook")
    @Operation(summary = "Razorpay webhook endpoint (called by Razorpay's servers only)")
    public ResponseEntity<Void> handleWebhook(
            HttpServletRequest request,
            @RequestHeader("X-Razorpay-Signature") String signature) {

        try {
            String rawBody = readRawBody(request);
            paymentService.processWebhook(rawBody, signature);
            return ResponseEntity.ok().build();
        } catch (IOException e) {
            log.error("Failed to read webhook request body: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }



    // readRawBody()

    private String readRawBody(HttpServletRequest request) throws IOException {

        try (BufferedReader reader = request.getReader()) {
            return reader.lines().collect(Collectors.joining(System.lineSeparator()));
        }
    }

}

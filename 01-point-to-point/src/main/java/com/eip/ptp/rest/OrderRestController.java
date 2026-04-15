package com.eip.ptp.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderRestController {

    @PostMapping
    public ResponseEntity<Map<String, Object>> createOrder(@RequestBody Map<String, Object> order) {
        String orderId = UUID.randomUUID().toString();
        return ResponseEntity.ok(Map.of(
            "orderId", orderId,
            "status", "CREATED",
            "message", "Order received via REST"
        ));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<Map<String, Object>> getOrder(@PathVariable String orderId) {
        return ResponseEntity.ok(Map.of(
            "orderId", orderId,
            "status", "PROCESSING",
            "message", "Order found"
        ));
    }
}
package com.example.demo.dto;

import java.time.Instant;

public record ErrorResponse(String code, String message, String path, Instant timestamp) {
}

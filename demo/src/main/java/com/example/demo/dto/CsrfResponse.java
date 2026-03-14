package com.example.demo.dto;

public record CsrfResponse(
    String csrfToken,
    String headerName,
    String parameterName
) {}

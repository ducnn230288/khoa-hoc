package com.example.demo.dto;

import java.util.List;

public record LoginResponse(
        boolean authenticated,
        String username,
        List<String> roles
) {
}

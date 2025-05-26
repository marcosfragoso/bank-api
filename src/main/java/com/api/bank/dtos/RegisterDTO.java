package com.api.bank.dtos;

import com.api.bank.enums.UserRole;

public record RegisterDTO(String email, String password, UserRole role) {
}

package com.example.olineaqspring.config;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserPrincipal {
    private Integer userId;
    private String role;
}

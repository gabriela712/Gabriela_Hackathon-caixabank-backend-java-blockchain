package com.hackathon.blockchain.dto;

import lombok.Data;
import java.time.LocalDateTime;
import jakarta.validation.constraints.Pattern;

@Data
public class UserResponseDto {
    private Long id;
    private String username;
    private String email;
    private LocalDateTime registrationDate;
    @Pattern(regexp = "^0x[a-fA-F0-9]{40}$", message = "Dirección de wallet inválida")
    private String walletAddress;

    public UserResponseDto() {}
}

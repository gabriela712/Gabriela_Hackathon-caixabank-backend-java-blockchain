package com.hackathon.blockchain.dto;

import lombok.Data;

@Data
public class BuyRequestDto {
    private String asset; // El activo a comprar
    private double amount; // La cantidad a comprar
}

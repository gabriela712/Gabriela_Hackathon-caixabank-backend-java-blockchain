package com.hackathon.blockchain.dto;

import lombok.Data;

@Data
public class SellRequestDto {
    private String asset; // El activo a vender
    private double amount; // La cantidad a vender
}
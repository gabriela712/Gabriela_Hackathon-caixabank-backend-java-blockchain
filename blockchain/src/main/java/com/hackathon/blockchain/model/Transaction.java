package com.hackathon.blockchain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    private String id; // Identificador único de la transacción
    private String sender; // Dirección del remitente
    private String recipient; // Dirección del destinatario
    private BigDecimal amount; // Monto de la transacción
    private LocalDateTime timestamp; // Fecha y hora de la transacción
    private String status; // Estado de la transacción (por ejemplo, "pendiente", "completada", "fallida")
}

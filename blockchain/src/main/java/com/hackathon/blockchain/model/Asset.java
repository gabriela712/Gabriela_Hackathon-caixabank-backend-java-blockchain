package com.hackathon.blockchain.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "assets")
public class Asset {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String symbol;
    
    @Column(nullable = false)
    private Double quantity;
    
    @ManyToOne
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    // Método para actualizar la cantidad
    public void updateQuantity(double delta) {
        this.quantity += delta;
        if (this.quantity <= 0) {
            this.quantity = 0.0;
        }
    }
}

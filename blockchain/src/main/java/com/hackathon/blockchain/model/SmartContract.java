package com.hackathon.blockchain.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "smart_contracts")
public class SmartContract {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String conditionExpression;
    
    private String action;
    private Double actionValue;
    private Long issuerWalletId;
    
    @Column(columnDefinition = "TEXT")
    private String digitalSignature;
}

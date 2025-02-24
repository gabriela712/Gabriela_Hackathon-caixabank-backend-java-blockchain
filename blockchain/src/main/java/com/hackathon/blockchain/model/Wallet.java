package com.hackathon.blockchain.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "wallets")
public class Wallet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String address;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private Double cashBalance;

    public String Status;

} 
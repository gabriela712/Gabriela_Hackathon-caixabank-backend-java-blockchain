package com.hackathon.blockchain.controller;

import com.hackathon.blockchain.model.Transaction;
import com.hackathon.blockchain.service.WalletService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import com.hackathon.blockchain.dto.BuyRequestDto;
import com.hackathon.blockchain.dto.SellRequestDto;
import com.hackathon.blockchain.dto.UserResponseDto;

@RestController
@RequestMapping("/wallet")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping("/create")
    public ResponseEntity<String> createWallet() {
        // Lógica para crear una billetera
        String walletAddress = walletService.createWallet(); // Implementa este método en WalletService
        return ResponseEntity.ok("{\"message\": \"✅ Wallet successfully created! Address: " + walletAddress + "\"}");
    }

    @PostMapping("/generate-keys")
    public ResponseEntity<String> generateKeys(@AuthenticationPrincipal UserResponseDto user) {
        // Lógica para generar claves
        String keysInfo = walletService.generateKeys(user.getId()); // Implementa este método en WalletService
        return ResponseEntity.ok(keysInfo);
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<Transaction>> getTransactions(@AuthenticationPrincipal UserResponseDto user) {
        // Lógica para obtener transacciones
        List<Transaction> transactions = walletService.getTransactions(user.getId()); // Implementa este método en WalletService
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/balance")
    public ResponseEntity<Map<String, Object>> getBalance(@AuthenticationPrincipal UserResponseDto user) {
        Map<String, Object> balanceInfo = walletService.getWalletBalance(user.getId());
        return ResponseEntity.ok(balanceInfo);
    }

    @PostMapping("/buy")
    public ResponseEntity<String> buyAsset(@AuthenticationPrincipal UserResponseDto user, @RequestBody BuyRequestDto buyRequest) {
        // Lógica para comprar un activo
        try {
            walletService.buyAsset(user.getId(), buyRequest); // Implementa este método en WalletService
            return ResponseEntity.ok("{\"message\": \"✅ Asset purchased successfully!\"}");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("{\"message\": \"❌ Transaction blocked for " + buyRequest.getAsset() + "\"}");
        }
    }

    @PostMapping("/sell")
    public ResponseEntity<String> sellAsset(@AuthenticationPrincipal UserResponseDto user, @RequestBody SellRequestDto sellRequest) {
        // Lógica para vender un activo
        try {
            walletService.sellAsset(user.getId(), sellRequest); // Implementa este método en WalletService
            return ResponseEntity.ok("{\"message\": \"✅ Asset sold successfully!\"}");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("{\"message\": \"❌ Transaction blocked for " + sellRequest.getAsset() + "\"}");
        }
    }
}

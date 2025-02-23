package com.hackathon.blockchain.service;

import com.hackathon.blockchain.model.User;
import com.hackathon.blockchain.model.Wallet;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class DashboardService {
    private final WalletService walletService;

    public DashboardService(WalletService walletService) {
        this.walletService = walletService;
    }

    public Map<String, Object> getDashboardInfo(User user) {
        Map<String, Object> dashboardInfo = new HashMap<>();
        dashboardInfo.put("username", user.getUsername());
        dashboardInfo.put("email", user.getEmail());

        walletService.getWalletByUserId(user.getId()).ifPresent(wallet -> {
            dashboardInfo.put("walletAddress", wallet.getAddress());
            dashboardInfo.put("balance", wallet.getBalance());
        });

        return dashboardInfo;
    }
}

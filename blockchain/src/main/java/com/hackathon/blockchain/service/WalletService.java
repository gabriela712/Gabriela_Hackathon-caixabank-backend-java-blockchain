package com.hackathon.blockchain.service;

import com.hackathon.blockchain.model.Asset;
import com.hackathon.blockchain.model.Transaction;
import com.hackathon.blockchain.model.User;
import com.hackathon.blockchain.model.Wallet;
import com.hackathon.blockchain.repository.TransactionRepository;
import com.hackathon.blockchain.repository.WalletRepository;
import com.hackathon.blockchain.repository.AssetRepository;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;
import java.util.List;
import java.util.Map;
import java.util.Date;
import java.util.HashMap;
import java.util.Collections;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;

@Slf4j
@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final MarketDataService marketDataService;
    private final AssetRepository assetRepository;
    private static final double TRANSACTION_FEE_PERCENT = 0.01; // 1% de fee

    public WalletService(WalletRepository walletRepository, 
                         TransactionRepository transactionRepository, 
                         MarketDataService marketDataService,
                         AssetRepository assetRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.marketDataService = marketDataService;
        this.assetRepository = assetRepository;
    }

    public Optional<Wallet> getWalletByUserId(Long userId) {
        // Implementación correcta para producción usando la relación JPA
        return walletRepository.findByUserId(userId);
    }

    public Optional<Wallet> getWalletByAddress(String address) {
        return walletRepository.findByAddress(address);
    }
     // Implementa este método en WalletService createWallet para Wallet Controle funcione

    @Transactional
    public void initializeLiquidityPools(Map<String, Double> initialAssets) {
        for (Map.Entry<String, Double> entry : initialAssets.entrySet()) {
            String symbol = entry.getKey();
            double initialQuantity = entry.getValue();

            String liquidityWalletAddress = "LP-" + symbol;
            Optional<Wallet> existingWallet = walletRepository.findByAddress(liquidityWalletAddress);

            if (existingWallet.isEmpty()) {
                Wallet liquidityWallet = new Wallet();
                liquidityWallet.setAddress(liquidityWalletAddress);
                // Eliminado setBalance ya que Wallet no maneja balance directamente
                walletRepository.save(liquidityWallet);

                Asset asset = new Asset();
                asset.setSymbol(symbol);
                asset.setQuantity(initialQuantity);
                asset.setWallet(liquidityWallet);
                assetRepository.save(asset);
            } 
        }
    }
     * Los usuarios deben comprar primero USDT para poder cambiar por tokens
     * El dinero fiat no vale para comprar tokens
     * Cuando se intercambia USDT por cualquier moneda, no se añade USDT a los assets de otras monedas
     */
    @Transactional
    public String buyAsset(Long userId, String symbol, double quantity) {
        Optional<Wallet> optionalWallet = walletRepository.findByUserId(userId);
        Optional<Wallet> liquidityWalletOpt = walletRepository.findByAddress("LP-" + symbol);
        Optional<Wallet> usdtLiquidityWalletOpt = walletRepository.findByAddress("LP-USDT");
    
        if (optionalWallet.isEmpty()) return "❌ Wallet not found!";
        if (liquidityWalletOpt.isEmpty()) return "❌ Liquidity pool for " + symbol + " not found!";
        if (usdtLiquidityWalletOpt.isEmpty()) return "❌ Liquidity pool for USDT not found!";
    
        Wallet userWallet = optionalWallet.get();
        Wallet liquidityWallet = liquidityWalletOpt.get();
        Wallet usdtLiquidityWallet = usdtLiquidityWalletOpt.get();
    
        double price = marketDataService.fetchLivePriceForAsset(symbol);
        double totalCost = quantity * price;
    
        if (symbol.equals("USDT")) {
            if (userWallet.getAssets().stream()
                    .filter(a -> a.getSymbol().equals("USDT"))
                    .map(Asset::getQuantity)
                    .findFirst()
                    .orElse(0.0) < totalCost) {
                return "❌ Insufficient fiat balance to buy USDT!";
            }
    
            updateWalletAssets(userWallet, "USDT", -totalCost);
            updateWalletAssets(usdtLiquidityWallet, "USDT", quantity);
    
            walletRepository.save(userWallet);
            walletRepository.save(usdtLiquidityWallet);
    
            recordTransaction(usdtLiquidityWallet, userWallet, "USDT", quantity, price, "BUY");
            return "✅ USDT purchased successfully!";
        }
    
        Optional<Asset> usdtAssetOpt = userWallet.getAssets().stream()
                .filter(a -> a.getSymbol().equals("USDT"))
                .findFirst();
    
        if (usdtAssetOpt.isEmpty() || usdtAssetOpt.get().getQuantity() < totalCost) {
            return "❌ Insufficient USDT balance! You must buy USDT first.";
        }
    
        updateWalletAssets(userWallet, "USDT", -totalCost);
        updateWalletAssets(usdtLiquidityWallet, "USDT", totalCost);
    
        updateWalletAssets(userWallet, symbol, quantity);
        updateWalletAssets(liquidityWallet, symbol, -quantity);
    
        walletRepository.save(userWallet);
        walletRepository.save(liquidityWallet);
        walletRepository.save(usdtLiquidityWallet);
    
        recordTransaction(liquidityWallet, userWallet, symbol, quantity, price, "BUY");
    
        return "✅ Asset purchased successfully!";
    }

    /*
     * La venta siempre se hace por USDT
     * Los usuarios después pueden cambiar USDT por la moneda fiat
     */
    @Transactional
    public String sellAsset(Long userId, String symbol, double quantity) {
        Optional<Wallet> optionalWallet = walletRepository.findByUserId(userId);
        Optional<Wallet> liquidityWalletOpt = walletRepository.findByAddress("LP-" + symbol);
    
        if (optionalWallet.isEmpty()) return "❌ Wallet not found!";
        if (liquidityWalletOpt.isEmpty()) return "❌ Liquidity pool for " + symbol + " not found!";
    
        Wallet userWallet = optionalWallet.get();
        Wallet liquidityWallet = liquidityWalletOpt.get();
    
        double price = marketDataService.fetchLivePriceForAsset(symbol);
        double totalRevenue = quantity * price;
    
        Optional<Asset> existingAsset = userWallet.getAssets().stream()
                .filter(a -> a.getSymbol().equals(symbol))
                .findFirst();
    
        if (existingAsset.isEmpty() || existingAsset.get().getQuantity() < quantity) {
            return "❌ Not enough assets to sell!";
        }
    
        // CASO 1: Venta de USDT (Recibo dinero fiat)
        if (symbol.equals("USDT")) {
            if (liquidityWallet.getAssets().stream().anyMatch(a -> a.getSymbol().equals("USDT") && a.getQuantity() < quantity)) {
                return "❌ Not enough USDT liquidity!";
            }
    
            updateWalletAssets(userWallet, symbol, -quantity);
            updateWalletAssets(liquidityWallet, symbol, quantity);
    
        } else {
            // CASO 2: Venta de otros assets (Recibo USDT)
            Optional<Wallet> usdtLiquidityWalletOpt = walletRepository.findByAddress("LP-USDT");
            if (usdtLiquidityWalletOpt.isEmpty()) return "❌ USDT liquidity pool not found!";
            Wallet usdtLiquidityWallet = usdtLiquidityWalletOpt.get();
    
            Optional<Asset> usdtAssetOpt = usdtLiquidityWallet.getAssets().stream()
                    .filter(a -> a.getSymbol().equals("USDT"))
                    .findFirst();
    
            if (usdtAssetOpt.isEmpty() || usdtAssetOpt.get().getQuantity() < totalRevenue) {
                return "❌ Not enough USDT in liquidity pool!";
            }
    
            updateWalletAssets(userWallet, "USDT", totalRevenue);
            updateWalletAssets(userWallet, symbol, -quantity);
            updateWalletAssets(usdtLiquidityWallet, "USDT", -totalRevenue);
            updateWalletAssets(liquidityWallet, symbol, quantity);
    
            walletRepository.save(usdtLiquidityWallet);
        }
    
        recordTransaction(userWallet, liquidityWallet, symbol, quantity, price, "SELL");
    
        walletRepository.save(userWallet);
        walletRepository.save(liquidityWallet);
    
        return "✅ Asset sold successfully!";
    }

    /*
     * Esta versión ya no almacena purchasePrice en Assets
     */
    private void updateWalletAssets(Wallet wallet, String assetSymbol, double amount) {
        Optional<Asset> assetOpt = wallet.getAssets().stream()
                .filter(asset -> asset.getSymbol().equalsIgnoreCase(assetSymbol))
                .findFirst();
    
        if (assetOpt.isPresent()) {
            Asset asset = assetOpt.get();
            asset.setQuantity(asset.getQuantity() + amount);
            if (asset.getQuantity() <= 0) {
                wallet.getAssets().remove(asset);
            }
        } else if (amount > 0) {
            Asset newAsset = new Asset();
            newAsset.setSymbol(assetSymbol);
            newAsset.setQuantity(amount);
            newAsset.setWallet(wallet);
            wallet.getAssets().add(newAsset);
        }
    }     

    private void recordTransaction(Wallet sender, Wallet receiver, String assetSymbol, double quantity, double price, String type) {
        Transaction transaction = new Transaction(
            null, sender, receiver, assetSymbol, quantity, price, 
            type, new Date(), "PENDING", 0.0, null
        );
        
        // Calcular y aplicar fee solo para transacciones de COMPRA/VENTA
        if ("BUY".equals(type) || "SELL".equals(type)) {
            double amount = quantity * price;
            transferFee(transaction, amount);
        }
        
        transactionRepository.save(transaction);
    }
    
        public String createWalletForUser(User user) {
        Optional<Wallet> existingWallet = walletRepository.findByUserId(user.getId());
        if (existingWallet.isPresent()) {
            return "❌ You already have a wallet created.";
        }

        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setAddress(generateWalletAddress());
        wallet.setBalance(10000.0);
        wallet.setAccountStatus("ACTIVE");

        walletRepository.save(wallet);

        return "✅ Wallet successfully created! Address: " + wallet.getAddress();
    }

    private String generateWalletAddress() {
        return DigestUtils.sha256Hex(UUID.randomUUID().toString());
    }


    // Ejecuto esta función para tener patrimonios de carteras actualizados continuamente y que no contenga valores estáticos
    @Scheduled(fixedRate = 30000) // Se ejecuta cada 30 segundos
    @Transactional
    public void updateWalletBalancesScheduled() {
        log.info("🔄 Updating wallet net worths based on live market prices...");
    
        List<Wallet> wallets = walletRepository.findAll();
        for (Wallet wallet : wallets) {
            double totalValue = 0.0;
    
            for (Asset asset : wallet.getAssets()) {
                double marketPrice = marketDataService.fetchLivePriceForAsset(asset.getSymbol());
                double assetValue = asset.getQuantity() * marketPrice;
                totalValue += assetValue;
    
                log.info("💰 Asset {} - Quantity: {} - Market Price: {} - Total Value: {}",
                        asset.getSymbol(), asset.getQuantity(), marketPrice, assetValue);
            }
    
            if (wallet.getUser() != null) {
                totalValue += wallet.getAssets().stream()
                        .filter(a -> a.getSymbol().equals("USDT"))
                        .map(Asset::getQuantity)
                        .findFirst()
                        .orElse(0.0);
            }
    
            double previousNetWorth = wallet.getNetWorth();
            wallet.setNetWorth(totalValue);
            walletRepository.save(wallet);
    
            log.info("📊 Wallet [{}] - Previous Net Worth: {} - Updated Net Worth: {}",
                    wallet.getAddress(), previousNetWorth, totalValue);
    
            Wallet savedWallet = walletRepository.findById(wallet.getId()).orElse(null);
            if (savedWallet != null) {
                log.info("✅ Confirmed DB Update - Wallet [{}] New Net Worth: {}", savedWallet.getAddress(), savedWallet.getNetWorth());
            } else {
                log.error("❌ Failed to fetch wallet [{}] after update!", wallet.getAddress());
            }
        }
    
        log.info("✅ All wallet net worths updated successfully!");
    }
    
    public Map<String, Object> getWalletBalance(Long userId) {
        Optional<Wallet> walletOpt = walletRepository.findByUserId(userId);
        
        return walletOpt.map(wallet -> {
            Map<String, Object> response = new HashMap<>();
            response.put("address", wallet.getAddress());
            response.put("fiatBalance", wallet.getAssets().stream()
                    .filter(a -> a.getSymbol().equals("USDT"))
                    .map(Asset::getQuantity)
                    .findFirst()
                    .orElse(0.0));
            response.put("assets", wallet.getAssets().stream()
                .collect(Collectors.toMap(Asset::getSymbol, Asset::getQuantity)));
            return response;
        }).orElse(Collections.singletonMap("error", "Wallet not found"));
    }
    
    /**
     * Devuelve un mapa con dos listas de transacciones:
     * - "sent": transacciones enviadas (donde la wallet es remitente)
     * - "received": transacciones recibidas (donde la wallet es destinataria)
     */
    public Map<String, List<Transaction>> getWalletTransactions(Long walletId) {
        Optional<Wallet> walletOpt = walletRepository.findById(walletId);
        if (walletOpt.isEmpty()) {
            return Map.of("error", List.of());
        }
        Wallet wallet = walletOpt.get();
        List<Transaction> sentTransactions = transactionRepository.findBySenderWallet(wallet);
        List<Transaction> receivedTransactions = transactionRepository.findByReceiverWallet(wallet);
        Map<String, List<Transaction>> result = new HashMap<>();
        result.put("sent", sentTransactions);
        result.put("received", receivedTransactions);
        return result;
    }

    // RETO BACKEND

    // Método para transferir el fee: deducirlo del wallet del emisor y sumarlo a la wallet de fees.
    public void transferFee(Transaction tx, double amount) {
        Optional<Wallet> senderOpt = walletRepository.findById(tx.getSenderWallet().getId());
        Optional<Wallet> feeWalletOpt = walletRepository.findByAddress("FEES-USDT");
        
        if (senderOpt.isPresent() && feeWalletOpt.isPresent()) {
            Wallet sender = senderOpt.get();
            Wallet feeWallet = feeWalletOpt.get();
            
            double fee = amount * TRANSACTION_FEE_PERCENT;
            
            // Aplicar fee solo si hay suficiente saldo
            if (sender.getAssets().stream()
                    .filter(a -> a.getSymbol().equals("USDT"))
                    .map(Asset::getQuantity)
                    .findFirst()
                    .orElse(0.0) >= fee) {
                updateWalletAssets(sender, "USDT", -fee);
                updateWalletAssets(feeWallet, "USDT", fee);
                
                tx.setFee(fee);
                log.info("💰 Fee applied: {} USDT", fee);
            } else {
                log.warn("❌ Insufficient funds for fee: {}", sender.getAddress());
            }
        }
    }

    // Método para crear una wallet para fees (solo USDT)
    public String createFeeWallet() {
        String feeWalletAddress = "FEES-USDT";
        Optional<Wallet> existing = walletRepository.findByAddress(feeWalletAddress);
        if (existing.isPresent()) {
            return "Fee wallet already exists with address: " + feeWalletAddress;
        }
        Wallet feeWallet = new Wallet();
        feeWallet.setAddress(feeWalletAddress);
        feeWallet.setBalance(0.0);
        feeWallet.setAccountStatus("ACTIVE");
        // Al no estar asociada a un usuario, se deja user en null
        walletRepository.save(feeWallet);
        return "Fee wallet created successfully with address: " + feeWalletAddress;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeFeeWallet() {
        String feeWalletAddress = "FEES-USDT";
        Optional<Wallet> existing = walletRepository.findByAddress(feeWalletAddress);
        if (existing.isEmpty()) {
            Wallet feeWallet = new Wallet();
            feeWallet.setAddress(feeWalletAddress);
            feeWallet.setBalance(0.0);
            feeWallet.setAccountStatus("ACTIVE");
            walletRepository.save(feeWallet);
            log.info("✅ Fee wallet creada al iniciar la aplicación");
        }
    }
}
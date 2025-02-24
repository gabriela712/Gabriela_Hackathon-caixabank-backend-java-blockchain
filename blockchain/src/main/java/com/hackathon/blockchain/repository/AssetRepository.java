package com.hackathon.blockchain.repository;

import com.hackathon.blockchain.model.Asset;
import com.hackathon.blockchain.model.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssetRepository extends JpaRepository<Asset, Long> {

    // Busca assets por símbolo y wallet
    Optional<Asset> findBySymbolAndWallet(String symbol, Wallet wallet);

    // Encuentra todos los assets de una wallet específica
    List<Asset> findByWallet(Wallet wallet);

    // Actualiza la cantidad de un asset específico
    @Modifying
    @Query("UPDATE Asset a SET a.quantity = a.quantity + :delta WHERE a.id = :id")
    void updateQuantity(@Param("id") Long id, @Param("delta") Double delta);

    // Elimina assets con cantidad <= 0
    @Modifying
    @Query("DELETE FROM Asset a WHERE a.quantity <= 0")
    void purgeEmptyAssets();

    // Suma la cantidad total de un símbolo en todas las wallets
    @Query("SELECT COALESCE(SUM(a.quantity), 0) FROM Asset a WHERE a.symbol = :symbol")
    Double getTotalSupply(@Param("symbol") String symbol);

    // Encuentra todos los assets con un símbolo específico
    List<Asset> findBySymbol(String symbol);
}

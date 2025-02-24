package com.hackathon.blockchain.repository;
import com.hackathon.blockchain.model.Transaction;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByStatus(String status);
    List<Transaction> findBySenderWallet(Wallet senderWallet);
    List<Transaction> findByReceiverWallet(Wallet receiverWallet);
}

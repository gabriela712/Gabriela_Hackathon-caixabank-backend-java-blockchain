package com.hackathon.blockchain.repository;

import com.hackathon.blockchain.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {
    // Aquí puedes agregar métodos personalizados si es necesario
}

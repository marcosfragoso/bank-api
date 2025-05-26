package com.api.bank.repositories;

import com.api.bank.entities.Account;
import com.api.bank.entities.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    List<Transaction> findByFromAccountOrToAccount(Account fromAccount, Account toAccount);
}

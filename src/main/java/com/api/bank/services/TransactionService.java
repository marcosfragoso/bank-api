package com.api.bank.services;

import com.api.bank.dtos.TransactionPostDTO;
import com.api.bank.entities.Account;
import com.api.bank.entities.Transaction;
import com.api.bank.entities.user.User;
import com.api.bank.enums.TransactionStatus;
import com.api.bank.exceptions.transaction.CredentialsInvalidException;
import com.api.bank.exceptions.transaction.InsufficientBalanceException;
import com.api.bank.exceptions.transaction.SameAccountException;
import com.api.bank.exceptions.transaction.UnauthorizedTransactionException;
import com.api.bank.kafka.event.TransactionCreatedEvent;
import com.api.bank.kafka.event.TransactionEvent;
import com.api.bank.kafka.producer.TransactionProducer;
import com.api.bank.repositories.TransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountService accountService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TransactionProducer transactionProducer;

    @Autowired
    private ApplicationEventPublisher eventPublisher;


    public List<Transaction> getTransactions() {
        log.info("Fetching all transactions");
        List<Transaction> transactions = this.transactionRepository.findAll();
        log.debug("Found {} transactions", transactions.size());
        return transactions;
    }

    public List<Transaction> getTransactionsByAccount(String id) {
        log.info("Fetching transactions for account ID: {}", id);
        Account account = this.accountService.getAccount(id);
        log.debug("Account found: {} - User: {}", account.getNumber(), account.getUser().getUsername());

        List<Transaction> transactions = this.transactionRepository.findByFromAccountOrToAccount(account, account);
        log.debug("Found {} transactions related to account {}", transactions.size(), account.getNumber());
        return transactions;
    }

    @Transactional
    public Transaction createTransaction(TransactionPostDTO transactionPostDTO) {
        log.info("Starting transaction from {} to {}", transactionPostDTO.getFromAccount(), transactionPostDTO.getToAccount());

        Account fromAccount = this.accountService.getAccountByNumber(transactionPostDTO.getFromAccount());
        Account toAccount = this.accountService.getAccountByNumber(transactionPostDTO.getToAccount());

        User loggedUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        log.debug("Logged in user: {} (ID: {})", loggedUser.getUsername(), loggedUser.getId());

        boolean isOwner = fromAccount.getUser().getId().equals(loggedUser.getId());
        boolean isAdmin = loggedUser.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

        if (!isOwner && !isAdmin) {
            log.warn("User {} tried to perform transaction without permission", loggedUser.getUsername());
            throw new UnauthorizedTransactionException();
        }

        if (!isAdmin) {
            boolean passwordMatches = passwordEncoder.matches(
                    transactionPostDTO.getPasswordUser(),
                    fromAccount.getUser().getPassword()
            );

            if (!passwordMatches) {
                log.warn("Invalid password for user {}", loggedUser.getUsername());
                throw new CredentialsInvalidException();
            }
        }

        if (fromAccount.equals(toAccount)) {
            log.warn("Attempt to transfer to the same account: {}", fromAccount.getNumber());
            throw new SameAccountException();
        }

        if (fromAccount.getBalance().compareTo(transactionPostDTO.getAmount()) < 0) {
            log.warn("Insufficient balance in account {}. Current balance: {}, requested amount: {}",
                    fromAccount.getNumber(),
                    fromAccount.getBalance(),
                    transactionPostDTO.getAmount());
            throw new InsufficientBalanceException();
        }

        this.accountService.deposit(toAccount, transactionPostDTO.getAmount());
        this.accountService.withdraw(fromAccount, transactionPostDTO.getAmount());

        Transaction transaction = Transaction.builder()
                .status(TransactionStatus.COMPLETED)
                .fromAccount(fromAccount)
                .toAccount(toAccount)
                .amount(transactionPostDTO.getAmount())
                .build();

        Transaction savedTransaction = this.transactionRepository.save(transaction);
        log.info("Transaction saved successfully: ID = {}, amount = {}", savedTransaction.getId(), savedTransaction.getAmount());

        TransactionEvent event = new TransactionEvent(
                fromAccount.getNumber(),
                toAccount.getNumber(),
                transactionPostDTO.getAmount(),
                TransactionStatus.COMPLETED.name()
        );

        eventPublisher.publishEvent(new TransactionCreatedEvent(event));
        log.info("TransactionCreatedEvent published for transaction ID = {}", savedTransaction.getId());

        return savedTransaction;
    }


}

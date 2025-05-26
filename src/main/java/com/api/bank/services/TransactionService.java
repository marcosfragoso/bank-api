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
import com.api.bank.repositories.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountService accountService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public List<Transaction> getTransactions() {
        return this.transactionRepository.findAll();
    }

    public List<Transaction> getTransactionsByAccount(String id) {
        Account account = this.accountService.getAccount(id);

        return this.transactionRepository.findByFromAccountOrToAccount(account, account);
    }

    @Transactional
    public Transaction createTransaction(TransactionPostDTO transactionPostDTO) {
        Account fromAccount = this.accountService.getAccountByNumber(transactionPostDTO.getFromAccount());
        Account toAccount = this.accountService.getAccountByNumber(transactionPostDTO.getToAccount());

        User loggedUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        boolean isOwner = fromAccount.getUser().getId().equals(loggedUser.getId());
        boolean isAdmin = loggedUser.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

        if (!isOwner && !isAdmin) {
            throw new UnauthorizedTransactionException();
        }

        if (!isAdmin) {
            boolean passwordMatches = passwordEncoder.matches(
                    transactionPostDTO.getPasswordUser(),
                    fromAccount.getUser().getPassword()
            );

            if (!passwordMatches) {
                throw new CredentialsInvalidException();
            }
        }

        if (fromAccount.equals(toAccount)) {
            throw new SameAccountException();
        }

        if (fromAccount.getBalance().compareTo(transactionPostDTO.getAmount()) < 0) {
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

        return this.transactionRepository.save(transaction);
    }

}

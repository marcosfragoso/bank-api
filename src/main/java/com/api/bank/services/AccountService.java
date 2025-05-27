package com.api.bank.services;

import com.api.bank.dtos.AccountPostDTO;
import com.api.bank.dtos.AccountUpdateDTO;
import com.api.bank.entities.Account;
import com.api.bank.entities.user.User;
import com.api.bank.exceptions.account.AccountNotFoundException;
import com.api.bank.exceptions.account.UnauthorizedAccountException;
import com.api.bank.repositories.AccountRepository;
import com.api.bank.repositories.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;


import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class AccountService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    public List<Account> getAccounts() {
        log.info("Fetching all accounts");
        List<Account> accounts = this.accountRepository.findAll();
        log.debug("Found {} accounts", accounts.size());
        return accounts;
    }

    public Account createAccount(AccountPostDTO accountPostDTO) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("Creating account for user: {}", username);

        User user = (User) userRepository.findByEmail(username);

        Account account = Account.builder()
                .number(accountPostDTO.getNumber())
                .balance(accountPostDTO.getBalance())
                .user(user)
                .build();

        this.accountRepository.save(account);

        log.info("Account created with number: {} for user: {}", account.getNumber(), username);
        return account;
    }

    public Account getAccount(String id) {
        log.info("Fetching account by ID: {}", id);
        Account account = this.accountRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> {
                    log.warn("Account not found with ID: {}", id);
                    return new AccountNotFoundException();
                });

        checkAccountPermission(account);

        log.debug("Account found: {} for user: {}", account.getNumber(), account.getUser().getUsername());
        return account;
    }

    public Account getAccountByNumber(String number) {
        log.info("Fetching account by number: {}", number);
        Account account = this.accountRepository.findByNumber(number).orElseThrow(() -> {
            log.warn("Account not found with number: {}", number);
            return new AccountNotFoundException();
        });
        log.debug("Account found with ID: {}", account.getId());
        return account;
    }

    public void deleteAccount(String id) {
        log.info("Deleting account with ID: {}", id);
        Account account = this.accountRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> {
                    log.warn("Account not found with ID: {}", id);
                    return new AccountNotFoundException();
                });

        checkAccountPermission(account);

        this.accountRepository.delete(account);
        log.info("Account with ID: {} deleted successfully", id);
    }

    public Account updateAccount(String id, AccountUpdateDTO accountUpdateDTO) {
        log.info("Updating account with ID: {}", id);
        Account account = this.accountRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> {
                    log.warn("Account not found with ID: {}", id);
                    return new AccountNotFoundException();
                });

        checkAccountPermission(account);

        Optional.ofNullable(accountUpdateDTO.getNumber()).ifPresent(newNumber -> {
            log.debug("Updating account number from {} to {}", account.getNumber(), newNumber);
            account.setNumber(newNumber);
        });

        Optional.ofNullable(accountUpdateDTO.getBalance()).ifPresent(newBalance -> {
            log.debug("Updating account balance from {} to {}", account.getBalance(), newBalance);
            account.setBalance(newBalance);
        });

        Account updatedAccount = this.accountRepository.save(account);
        log.info("Account with ID: {} updated successfully", id);
        return updatedAccount;
    }

    public void deposit(Account account, BigDecimal value) {
        log.info("Depositing amount {} to account number {}", value, account.getNumber());
        account.setBalance(account.getBalance().add(value));
        this.accountRepository.save(account);
        log.debug("New balance after deposit: {}", account.getBalance());
    }

    public void withdraw(Account account, BigDecimal value) {
        log.info("Withdrawing amount {} from account number {}", value, account.getNumber());
        account.setBalance(account.getBalance().subtract(value));
        this.accountRepository.save(account);
        log.debug("New balance after withdrawal: {}", account.getBalance());
    }

    private void checkAccountPermission(Account account) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        boolean isOwner = account.getUser().getId().equals(user.getId());
        boolean isAdmin = user.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

        log.debug("Checking permissions for user: {} on account: {}", user.getUsername(), account.getNumber());

        if (!isOwner && !isAdmin) {
            log.warn("User {} unauthorized to access account {}", user.getUsername(), account.getNumber());
            throw new UnauthorizedAccountException();
        }
    }


}

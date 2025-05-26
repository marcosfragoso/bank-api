package com.api.bank.services;

import com.api.bank.dtos.AccountPostDTO;
import com.api.bank.dtos.AccountUpdateDTO;
import com.api.bank.entities.Account;
import com.api.bank.entities.user.User;
import com.api.bank.exceptions.account.AccountNotFoundException;
import com.api.bank.exceptions.account.UnauthorizedAccountException;
import com.api.bank.repositories.AccountRepository;
import com.api.bank.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;


import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AccountService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    public List<Account> getAccounts() {
        return this.accountRepository.findAll();
    }

    public Account createAccount(AccountPostDTO accountPostDTO) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = (User) userRepository.findByEmail(username);

        Account account = Account.builder()
                .number(accountPostDTO.getNumber())
                .balance(accountPostDTO.getBalance())
                .user(user)
                .build();
        this.accountRepository.save(account);
        return account;
    }

    public Account getAccount(String id) {
        Account account = this.accountRepository.findById(UUID.fromString(id))
                .orElseThrow(AccountNotFoundException::new);

        checkAccountPermission(account);

        return account;
    }

    public Account getAccountByNumber(String number) {
        return this.accountRepository.findByNumber(number).orElseThrow(AccountNotFoundException::new);
    }

    public void deleteAccount(String id) {
        Account account = this.accountRepository.findById(UUID.fromString(id))
                .orElseThrow(AccountNotFoundException::new);

        checkAccountPermission(account);

        this.accountRepository.delete(account);
    }

    public Account updateAccount(String id, AccountUpdateDTO accountUpdateDTO) {
        Account account = this.accountRepository.findById(UUID.fromString(id))
                .orElseThrow(AccountNotFoundException::new);

        checkAccountPermission(account);

        Optional.ofNullable(accountUpdateDTO.getNumber()).ifPresent(account::setNumber);
        Optional.ofNullable(accountUpdateDTO.getBalance()).ifPresent(account::setBalance);

        return this.accountRepository.save(account);
    }


    public void deposit(Account account, BigDecimal value) {
        account.setBalance(account.getBalance().add(value));
        this.accountRepository.save(account);
    }

    public void withdraw(Account account, BigDecimal value) {
        account.setBalance(account.getBalance().subtract(value));
        this.accountRepository.save(account);
    }

    private void checkAccountPermission(Account account) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        boolean isOwner = account.getUser().getId().equals(user.getId());
        boolean isAdmin = user.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

        if (!isOwner && !isAdmin) {
            throw new UnauthorizedAccountException();
        }
    }


}

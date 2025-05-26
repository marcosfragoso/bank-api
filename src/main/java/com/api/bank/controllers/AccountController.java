package com.api.bank.controllers;

import com.api.bank.dtos.AccountPostDTO;
import com.api.bank.dtos.AccountUpdateDTO;
import com.api.bank.services.AccountService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    @Autowired
    private AccountService accountService;

    @GetMapping("/")
    public ResponseEntity<?> getAccounts() {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(this.accountService.getAccounts());
    }

    @PostMapping("/")
    public ResponseEntity<?> createAccount(@RequestBody @Valid AccountPostDTO accountPostDTO) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(this.accountService.createAccount(accountPostDTO));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getAccount(@PathVariable String id) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(this.accountService.getAccount(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAccount(@PathVariable String id) {
        this.accountService.deleteAccount(id);
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .body("Account deleted successfully");
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateAccount(@PathVariable String id, @RequestBody @Valid AccountUpdateDTO accountUpdateDTO) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(this.accountService.updateAccount(id, accountUpdateDTO));
    }
}

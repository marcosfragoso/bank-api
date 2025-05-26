package com.api.bank.controllers;


import com.api.bank.dtos.TransactionPostDTO;
import com.api.bank.services.TransactionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @GetMapping("/")
    public ResponseEntity<?> getTransactions() {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(this.transactionService.getTransactions());
    }

    @PostMapping("/")
    public ResponseEntity<?> createTransaction(@RequestBody @Valid TransactionPostDTO transactionPostDTO) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(this.transactionService.createTransaction(transactionPostDTO));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getTransactionsByAccount(@PathVariable String id) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(this.transactionService.getTransactionsByAccount(id));
    }
}

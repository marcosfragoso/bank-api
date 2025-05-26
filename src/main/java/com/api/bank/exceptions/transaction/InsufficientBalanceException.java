package com.api.bank.exceptions.transaction;

import com.api.bank.exceptions.BankException;

public class InsufficientBalanceException extends BankException {
    public InsufficientBalanceException() {
        super("Insufficient balance.");
    }
}

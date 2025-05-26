package com.api.bank.exceptions.account;

import com.api.bank.exceptions.BankException;

public class AccountNotFoundException  extends BankException {
    public AccountNotFoundException() {
        super("Account not found.");
    }
}

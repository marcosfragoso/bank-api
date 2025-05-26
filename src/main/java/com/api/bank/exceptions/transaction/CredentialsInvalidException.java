package com.api.bank.exceptions.transaction;

import com.api.bank.exceptions.BankException;

public class CredentialsInvalidException extends BankException {
    public CredentialsInvalidException() {
        super("Invalid account password.");
    }
}

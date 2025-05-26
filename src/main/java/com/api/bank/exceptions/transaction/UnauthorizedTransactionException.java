package com.api.bank.exceptions.transaction;

import com.api.bank.exceptions.BankException;

public class UnauthorizedTransactionException extends BankException {
    public UnauthorizedTransactionException() {
        super("You do not have permission to perform this transaction.");
    }
}

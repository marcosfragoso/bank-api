package com.api.bank.exceptions.transaction;

import com.api.bank.exceptions.BankException;

public class SameAccountException extends BankException {
    public SameAccountException() {
        super("Transfer to the same account is not allowed.");
    }
}

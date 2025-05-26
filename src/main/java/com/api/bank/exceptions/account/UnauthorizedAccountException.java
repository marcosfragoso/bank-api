package com.api.bank.exceptions.account;

import com.api.bank.exceptions.BankException;

public class UnauthorizedAccountException extends BankException {
    public UnauthorizedAccountException() {
        super("You do not have permission to access this account.");
    }
}

package com.api.bank.exceptions;

public class BankException extends RuntimeException{

    public BankException() {
        super("Unespectecd error on Bank aplication!");
    }

    public BankException(String message) {
        super(message);
    }
}

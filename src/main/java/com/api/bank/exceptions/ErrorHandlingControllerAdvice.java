package com.api.bank.exceptions;

import com.api.bank.exceptions.account.AccountNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;

@ControllerAdvice
public class ErrorHandlingControllerAdvice {

    private CustomErrorType defaultCustomErrorTypeConstruct(String message) {
        return CustomErrorType.builder()
                .timestamp(LocalDateTime.now())
                .errors(new ArrayList<>())
                .message(message)
                .build();
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public CustomErrorType onMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        CustomErrorType customErrorType = defaultCustomErrorTypeConstruct(
                "Validation errors found"
        );
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            customErrorType.getErrors().add(fieldError.getDefaultMessage());
        }
        return customErrorType;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public CustomErrorType onConstraintViolation(ConstraintViolationException e) {
        CustomErrorType customErrorType = defaultCustomErrorTypeConstruct(
                "Validation errors found"
        );
        for (ConstraintViolation<?> violation : e.getConstraintViolations()) {
            customErrorType.getErrors().add(violation.getMessage());
        }
        return customErrorType;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public CustomErrorType handleDataIntegrityViolationException(DataIntegrityViolationException e) {
        String message = "Integrity violation: ";

        if (e.getRootCause() != null) {
            message += e.getRootCause().getMessage();
        } else {
            message += e.getMessage();
        }

        return defaultCustomErrorTypeConstruct(message);
    }

    @ExceptionHandler(BankException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public CustomErrorType onBankException(BankException e) {
        return defaultCustomErrorTypeConstruct(
                e.getMessage()
        );
    }

    @ExceptionHandler(AccountNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    public CustomErrorType accountNotFoundException(AccountNotFoundException e) {
        return defaultCustomErrorTypeConstruct(
                e.getMessage()
        );
    }

}

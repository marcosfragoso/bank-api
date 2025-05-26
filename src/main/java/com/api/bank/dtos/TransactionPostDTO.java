package com.api.bank.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;


@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@Builder
public class TransactionPostDTO {

    @NotNull(message = "Account number is required.")
    private String fromAccount;

    @NotNull(message = "Account number is required.")
    private String toAccount;

    @NotNull(message = "Transaction amount is required.")
    @Positive(message = "The transaction amount cannot be negative or zero.")
    private BigDecimal amount;

    @NotNull(message = "Owner password is required.")
    private String passwordUser;
}

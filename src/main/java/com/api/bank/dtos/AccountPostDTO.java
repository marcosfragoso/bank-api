package com.api.bank.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@Builder
public class AccountPostDTO {

    @NotNull(message = "Account number is required.")
    @Size(min = 6, max = 6, message = "Account number must have exactly 6 characters.")
    private String number;

    @NotNull(message = "Account balance is required.")
    @PositiveOrZero(message = "The account balance cannot be negative.")
    private BigDecimal balance;
}

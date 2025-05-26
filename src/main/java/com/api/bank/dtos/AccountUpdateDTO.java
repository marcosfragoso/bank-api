package com.api.bank.dtos;

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
public class AccountUpdateDTO {

    @Size(min = 6, max = 6, message = "Account number must have exactly 6 characters.")
    private String number;

    @PositiveOrZero(message = "The account balance cannot be negative.")
    private BigDecimal balance;
}

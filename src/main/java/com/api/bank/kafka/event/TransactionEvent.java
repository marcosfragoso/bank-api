package com.api.bank.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionEvent {
    private String fromAccount;
    private String toAccount;
    private BigDecimal amount;
    private String status;
}

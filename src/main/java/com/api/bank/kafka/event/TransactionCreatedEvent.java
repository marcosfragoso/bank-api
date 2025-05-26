package com.api.bank.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TransactionCreatedEvent {
    private TransactionEvent transactionEvent;
}

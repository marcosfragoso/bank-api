package com.api.bank.kafka.event;

import com.api.bank.kafka.producer.TransactionProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class TransactionEventListener {

    @Autowired
    private TransactionProducer transactionProducer;

    @TransactionalEventListener
    public void handleTransactionCreatedEvent(TransactionCreatedEvent event) {
        transactionProducer.sendTransactionEvent(event.getTransactionEvent());
    }
}


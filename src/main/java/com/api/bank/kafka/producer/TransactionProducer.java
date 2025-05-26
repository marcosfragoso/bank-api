package com.api.bank.kafka.producer;

import com.api.bank.kafka.event.TransactionEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class TransactionProducer {

    private static final String TOPIC = "transaction-created";

    @Autowired
    private KafkaTemplate<String, TransactionEvent> kafkaTemplate;

    public void sendTransactionEvent(TransactionEvent event) {
        kafkaTemplate.send(TOPIC, event);
    }
}

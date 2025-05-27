package com.api.bank.kafka.consumer;

import com.api.bank.kafka.event.TransactionEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;


@Component
@Slf4j
public class TransactionConsumer {

    @KafkaListener(topics = "transaction-created", groupId = "transaction-group")
    public void consume(TransactionEvent event) {
        log.info("Transaction event received: {}", event);
    }
}

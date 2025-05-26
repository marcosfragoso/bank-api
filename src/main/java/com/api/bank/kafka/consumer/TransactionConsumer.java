package com.api.bank.kafka.consumer;

import com.api.bank.kafka.event.TransactionEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Component
public class TransactionConsumer {

    private static final Logger logger = LoggerFactory.getLogger(TransactionConsumer.class);


    @KafkaListener(topics = "transaction-created", groupId = "transaction-group")
    public void consume(TransactionEvent event) {
        logger.info("Transaction event received: {}", event);
    }
}

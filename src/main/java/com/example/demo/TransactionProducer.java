package com.example.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class TransactionProducer {

    private static final Logger log = LoggerFactory.getLogger(TransactionProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topicName;

    public TransactionProducer(
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${transaction.topic.name:transactions-topic}") String topicName) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicName = topicName;
    }

    public void sendTransaction(TransactionDto transaction) {
        log.info("Sending transaction to topic {}: {}", topicName, transaction);
        kafkaTemplate.send(topicName, transaction.id().toString(), transaction);
    }
}

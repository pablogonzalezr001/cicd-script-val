package com.example.demo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransactionProducerTest {

    @Mock
    private KafkaTemplate kafkaTemplate;

    private TransactionProducer transactionProducer;
    
    private final String topicName = "test-topic";

    @BeforeEach
    void setUp() {
        transactionProducer = new TransactionProducer(kafkaTemplate, topicName);
    }

    @Test
    void shouldSendTransactionToKafka() {
        UUID id = UUID.randomUUID();
        TransactionDto dto = new TransactionDto(id, "ACC-999", new BigDecimal("150.00"), "COMPLETED");

        transactionProducer.sendTransaction(dto);

        verify(kafkaTemplate).send(eq(topicName), eq(id.toString()), eq(dto));
    }
}

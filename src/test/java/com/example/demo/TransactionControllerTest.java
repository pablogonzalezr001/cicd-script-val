package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class TransactionControllerTest {

    private final TransactionProducer transactionProducer = mock(TransactionProducer.class);
    private final TransactionController transactionController = new TransactionController(transactionProducer);

    @Test
    void shouldAcceptTransaction() {
        TransactionDto dto = new TransactionDto(
                UUID.randomUUID(), 
                "ACC-1234", 
                new BigDecimal("99.99"), 
                "PENDING"
        );

        ResponseEntity<Void> response = transactionController.createTransaction(dto);

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        verify(transactionProducer).sendTransaction(dto);
    }
}

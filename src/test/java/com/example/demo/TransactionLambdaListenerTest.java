package com.example.demo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TransactionLambdaListenerTest {

    private LambdaClient lambdaClient;
    private ObjectMapper objectMapper;
    private TransactionLambdaListener listener;
    private final String functionName = "test-function";

    @BeforeEach
    void setUp() {
        lambdaClient = mock(LambdaClient.class);
        objectMapper = new ObjectMapper();
        listener = new TransactionLambdaListener(lambdaClient, objectMapper, functionName);
    }

    @Test
    void testListenTransactionSuccess() throws JsonProcessingException {
        // Arrange
        TransactionDto dto = new TransactionDto(UUID.randomUUID(), "account-123", new BigDecimal("100.00"), "USD");
        String expectedPayload = objectMapper.writeValueAsString(dto);

        InvokeResponse mockResponse = InvokeResponse.builder()
                .statusCode(200)
                .payload(SdkBytes.fromUtf8String("{\"message\":\"success\"}"))
                .build();
        when(lambdaClient.invoke(any(InvokeRequest.class))).thenReturn(mockResponse);

        // Act
        listener.listenTransaction(dto);

        // Assert
        ArgumentCaptor<InvokeRequest> captor = ArgumentCaptor.forClass(InvokeRequest.class);
        verify(lambdaClient, times(1)).invoke(captor.capture());

        InvokeRequest actualRequest = captor.getValue();
        assertEquals(functionName, actualRequest.functionName());
        assertEquals(expectedPayload, actualRequest.payload().asUtf8String());
    }

    @Test
    void testListenTransactionLambdaError() throws JsonProcessingException {
        // Arrange
        TransactionDto dto = new TransactionDto(UUID.randomUUID(), "account-123", new BigDecimal("100.00"), "USD");
        when(lambdaClient.invoke(any(InvokeRequest.class))).thenThrow(new RuntimeException("Lambda failure"));

        // Act
        listener.listenTransaction(dto);

        // Assert
        verify(lambdaClient, times(1)).invoke(any(InvokeRequest.class));
        // Exception should be caught and logged
    }

    @Test
    void testListenTransactionSerializationError() throws JsonProcessingException {
        // Arrange
        ObjectMapper mockMapper = mock(ObjectMapper.class);
        when(mockMapper.writeValueAsString(any())).thenThrow(mock(JsonProcessingException.class));
        
        TransactionLambdaListener errorListener = new TransactionLambdaListener(lambdaClient, mockMapper, functionName);
        TransactionDto dto = new TransactionDto(UUID.randomUUID(), "account-123", new BigDecimal("100.00"), "USD");

        // Act
        errorListener.listenTransaction(dto);

        // Assert
        verify(lambdaClient, never()).invoke(any(InvokeRequest.class));
    }
}

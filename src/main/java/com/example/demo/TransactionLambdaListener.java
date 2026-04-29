package com.example.demo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

import java.nio.charset.StandardCharsets;

@Service
public class TransactionLambdaListener {

    private static final Logger log = LoggerFactory.getLogger(TransactionLambdaListener.class);

    private final LambdaClient lambdaClient;
    private final ObjectMapper objectMapper;
    private final String functionName;

    public TransactionLambdaListener(
            LambdaClient lambdaClient,
            ObjectMapper objectMapper,
            @Value("${aws.lambda.function-name:transaction-processor}") String functionName) {
        this.lambdaClient = lambdaClient;
        this.objectMapper = objectMapper;
        this.functionName = functionName;
    }

    @KafkaListener(topics = "${transaction.topic.name}", groupId = "${spring.kafka.consumer.group-id}")
    public void listenTransaction(TransactionDto transaction) {
        log.info("Received transaction from Kafka: {}", transaction);

        try {
            String payload = objectMapper.writeValueAsString(transaction);

            InvokeRequest invokeRequest = InvokeRequest.builder()
                    .functionName(functionName)
                    .payload(SdkBytes.fromUtf8String(payload))
                    .build();

            log.info("Invoking AWS Lambda function: {}", functionName);
            InvokeResponse response = lambdaClient.invoke(invokeRequest);

            String responsePayload = response.payload().asString(StandardCharsets.UTF_8);
            log.info("Lambda response (Status {}): {}", response.statusCode(), responsePayload);

        } catch (JsonProcessingException e) {
            log.error("Error serializing transaction payload", e);
        } catch (Exception e) {
            log.error("Error invoking Lambda function", e);
        }
    }
}

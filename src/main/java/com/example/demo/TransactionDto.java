package com.example.demo;

import java.math.BigDecimal;
import java.util.UUID;

public record TransactionDto(
    UUID id,
    String accountId,
    BigDecimal amount,
    String status
) {
}

package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.lambda.LambdaClient;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class LambdaConfigTest {

    @Test
    void testLambdaClientWithEndpointOverride() {
        LambdaConfig config = new LambdaConfig();
        ReflectionTestUtils.setField(config, "endpointOverride", "http://localhost:4566");
        ReflectionTestUtils.setField(config, "region", "us-east-1");

        LambdaClient client = config.lambdaClient();
        assertNotNull(client);
    }

    @Test
    void testLambdaClientWithoutEndpointOverride() {
        LambdaConfig config = new LambdaConfig();
        ReflectionTestUtils.setField(config, "endpointOverride", null);
        ReflectionTestUtils.setField(config, "region", "us-east-1");

        LambdaClient client = config.lambdaClient();
        assertNotNull(client);
    }
}

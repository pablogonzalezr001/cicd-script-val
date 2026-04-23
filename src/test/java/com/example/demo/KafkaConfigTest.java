package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class KafkaConfigTest {

    @Test
    void testKafkaConfigBeans() {
        KafkaConfig kafkaConfig = new KafkaConfig();
        ReflectionTestUtils.setField(kafkaConfig, "bootstrapServers", "localhost:9092");
        
        ProducerFactory<String, Object> producerFactory = kafkaConfig.producerFactory();
        assertNotNull(producerFactory, "ProducerFactory no debe ser nulo");
        
        KafkaTemplate<String, Object> kafkaTemplate = kafkaConfig.kafkaTemplate();
        assertNotNull(kafkaTemplate, "KafkaTemplate no debe ser nulo");
    }
}

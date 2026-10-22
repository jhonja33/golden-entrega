package com.fincard.audit.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConsumerConfig {

    @Bean
    public DefaultErrorHandler kafkaErrorHandler() {

        var backOff = new FixedBackOff(
                5000L,
                FixedBackOff.UNLIMITED_ATTEMPTS
        );

        var handler = new DefaultErrorHandler(backOff);

        handler.addRetryableExceptions(IllegalStateException.class);

        return handler;
    }
}
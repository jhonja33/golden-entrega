package com.fincard.audit.consumer;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.kafka.common.TopicPartition;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.ConsumerSeekAware;
import org.springframework.stereotype.Component;

import com.fincard.audit.dto.TransactionApprovedMessage;
import com.fincard.audit.service.IAuditService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionEventsConsumer implements ConsumerSeekAware {

    private final JsonMapper jsonMapper;
    private final IAuditService auditService;

    private final Set<TopicPartition> initializedPartitions =
            ConcurrentHashMap.newKeySet();

    @Override
    public void onPartitionsAssigned(
            Map<TopicPartition, Long> assignments,
            ConsumerSeekCallback callback) {

        for (TopicPartition partition : assignments.keySet()) {

            if (initializedPartitions.add(partition)) {
                callback.seekToBeginning(
                        partition.topic(),
                        partition.partition()
                );

                log.info(
                        "Revisando eventos conservados: topic={}, partition={}",
                        partition.topic(),
                        partition.partition()
                );
            }
        }
    }

    @KafkaListener(topics = "${app.kafka.transaction-topic}")
    public void consume(String json) {

        try {
            TransactionApprovedMessage message =
                    jsonMapper.readValue(
                            json,
                            TransactionApprovedMessage.class
                    );

            validate(message);

            auditService.register(message);

            

        } catch (RuntimeException exception) {
            throw new IllegalStateException(
                    "No se pudo procesar el evento de transaccion",
                    exception
            );
        }
    }

    private void validate(TransactionApprovedMessage message) {

        if (message == null
                || message.schemaVersion() != 1
                || !"PAYMENT_APPROVED".equals(message.eventType())
                || !"APPROVED".equals(message.status())
                || !"00".equals(message.responseCode())
                || message.eventId() == null
                || message.eventId().isBlank()
                || message.transactionId() == null
                || message.transactionId().isBlank()
                || message.amount() == null
                || message.currency() == null
                || message.createdAt() == null) {

            throw new IllegalArgumentException(
                    "El mensaje no cumple el contrato de pago aprobado"
            );
        }
    }
}
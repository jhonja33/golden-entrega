package com.fincard.authorization.event;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.fincard.authorization.dto.TransactionApprovedMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentKafkaPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JsonMapper jsonMapper;

    @Value("${app.kafka.transaction-topic}")
    private String topic;

    @TransactionalEventListener(
        phase = TransactionPhase.AFTER_COMMIT
    )
    public void onPaymentApproved(PaymentApprovedEvent event) {

        try {
            TransactionApprovedMessage message =
                    new TransactionApprovedMessage(
                            1,
                            event.transactionId(),
                            "PAYMENT_APPROVED",
                            event.transactionId(),
                            event.amount(),
                            event.currency(),
                            "APPROVED",
                            "00",
                            event.createdAt().toString()
                    );

            String json = jsonMapper.writeValueAsString(message);

            kafkaTemplate.send(
                    topic,
                    event.transactionId(),
                    json
            ).whenComplete((result, exception) -> {

                if (exception != null) {
                    log.error(
                            "Fallo al publicar pago aprobado en Kafka: "
                                    + "transactionId={}",
                            event.transactionId(),
                            exception
                    );

                    return;
                }

                log.info(
                        "Evento confirmado por Kafka: "
                                + "transactionId={}, topic={}, "
                                + "partition={}, offset={}",
                        event.transactionId(),
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset()
                );
            });

        } catch (Exception exception) {
            log.error(
                    "No se pudo iniciar la publicacion en Kafka: "
                            + "transactionId={}",
                    event.transactionId(),
                    exception
            );
        }
    }
}
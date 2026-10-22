package com.fincard.authorization.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fincard.authorization.dto.AuthorizationProcedureResult;
import com.fincard.authorization.dto.AuthorizationRequest;
import com.fincard.authorization.dto.AuthorizationResponse;
import com.fincard.authorization.event.PaymentApprovedEvent;
import com.fincard.authorization.model.PaymentTransaction;
import com.fincard.authorization.repository.PaymentProcedureRepository;
import com.fincard.authorization.repository.PaymentTransactionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentSinkService implements IPaymentService {

    private final PaymentProcedureRepository paymentProcedureRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public AuthorizationResponse authorize(
            AuthorizationRequest request) {

        String transactionId = UUID.randomUUID().toString();

        log.info(
                "Iniciando autorizacion: transactionId={}",
                transactionId
        );

        log.debug(
                "Ejecutando sp_authorize_payment: transactionId={}",
                transactionId
        );

        AuthorizationProcedureResult procedureResult =
                paymentProcedureRepository.authorize(
                        request.getPan(),
                        request.getAmount(),
                        request.getCurrency()
                );

        String responseCode = procedureResult.getResponseCode();
        String message = resolveMessage(responseCode);

        boolean approved = "00".equals(responseCode);
        String status = approved ? "APPROVED" : "DECLINED";

        log.info(
                "Resultado de autorizacion: transactionId={}, "
                        + "status={}, responseCode={}",
                transactionId,
                status,
                responseCode
        );

        Instant createdAt = Instant.now()
                .truncatedTo(ChronoUnit.MICROS);

        PaymentTransaction transaction = new PaymentTransaction();

        transaction.setTransactionId(transactionId);
        transaction.setCardId(procedureResult.getCardId());
        transaction.setPan(request.getPan());
        transaction.setAmount(request.getAmount());
        transaction.setCurrency(request.getCurrency());
        transaction.setStatus(status);
        transaction.setResponseCode(responseCode);

        transaction.setCreatedAt(
                LocalDateTime.ofInstant(createdAt, ZoneOffset.UTC)
        );
		
        log.debug(
                "Insertando en transactions: transactionId={}",
                transactionId
        );

        paymentTransactionRepository.saveAndFlush(transaction);

        log.info(
                "Registro enviado a MySQL: transactionId={}, "
                        + "status={}, responseCode={}, pendiente de commit",
                transactionId,
                status,
                responseCode
        );

        if (approved) {
            eventPublisher.publishEvent(
                    new PaymentApprovedEvent(
                            request.getPan(),
                            transactionId,
                            request.getAmount(),
                            request.getCurrency(),
                            createdAt
                    )
            );
        }

        return new AuthorizationResponse(
                transactionId,
                request.getPan(),
                status,
                responseCode,
                message,
                request.getAmount(),
                request.getCurrency(),
                createdAt
        );
    }

    private String resolveMessage(String responseCode) {

        String message;

        switch (responseCode) {
            case "00" -> message = "Transaccion exitosa";
            case "51" -> message = "Fondos insuficientes";
            case "54" -> message = "Tarjeta vencida";
            case "62" -> message = "Tarjeta restringida";
            case "56" -> message = "Tarjeta no registrada";
            case "14" -> message = "Numero de tarjeta invalido";
            case "13" -> message = "Monto invalido";
            case "57" -> message = "Transaccion no permitida";
            case "12" -> message = "Transaccion invalida";
            default -> throw new IllegalStateException(
                    "Codigo de respuesta no reconocido: " + responseCode
            );
        }

        return message;
    }
}
package com.fincard.authorization.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.context.ApplicationEventPublisher;

import com.fincard.authorization.dto.AuthorizationProcedureResult;
import com.fincard.authorization.dto.AuthorizationRequest;
import com.fincard.authorization.event.PaymentApprovedEvent;
import com.fincard.authorization.model.PaymentTransaction;
import com.fincard.authorization.repository.PaymentProcedureRepository;
import com.fincard.authorization.repository.PaymentTransactionRepository;

@ExtendWith(MockitoExtension.class)
class PaymentSinkServiceTest {

    @Mock
    private PaymentProcedureRepository procedureRepository;

    @Mock
    private PaymentTransactionRepository transactionRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private PaymentSinkService service;

    @BeforeEach
    void setUp() {
        service = new PaymentSinkService(
                procedureRepository,
                transactionRepository,
                eventPublisher
        );
    }

    @Test
    void approvedPaymentIsRecordedAndPublishesEvent() {

        AuthorizationRequest request = request();

        when(procedureRepository.authorize(
                request.getPan(),
                request.getAmount(),
                request.getCurrency()
        )).thenReturn(new AuthorizationProcedureResult(1L, "00"));

        var response = service.authorize(request);

        assertEquals("APPROVED", response.getStatus());
        assertEquals("00", response.getResponseCode());

        var transactionCaptor =
                ArgumentCaptor.forClass(PaymentTransaction.class);

        verify(transactionRepository)
                .saveAndFlush(transactionCaptor.capture());

        assertEquals(
                response.getTransactionId(),
                transactionCaptor.getValue().getTransactionId()
        );

        assertEquals(
                "APPROVED",
                transactionCaptor.getValue().getStatus()
        );

        var eventCaptor =
                ArgumentCaptor.forClass(PaymentApprovedEvent.class);

        verify(eventPublisher).publishEvent(eventCaptor.capture());

        assertEquals(
                response.getTransactionId(),
                eventCaptor.getValue().transactionId()
        );

        assertEquals(
                request.getAmount(),
                eventCaptor.getValue().amount()
        );
    }

    @Test
    void declinedPaymentIsRecordedWithoutPublishingEvent() {

        AuthorizationRequest request = request();

        when(procedureRepository.authorize(
                request.getPan(),
                request.getAmount(),
                request.getCurrency()
        )).thenReturn(new AuthorizationProcedureResult(1L, "51"));

        var response = service.authorize(request);

        assertEquals("DECLINED", response.getStatus());
        assertEquals("51", response.getResponseCode());

        var transactionCaptor =
                ArgumentCaptor.forClass(PaymentTransaction.class);

        verify(transactionRepository)
                .saveAndFlush(transactionCaptor.capture());

        assertEquals(
                "51",
                transactionCaptor.getValue().getResponseCode()
        );

        verifyNoInteractions(eventPublisher);
    }

    @Test
    void persistenceFailureDoesNotPublishEvent() {

        AuthorizationRequest request = request();

        when(procedureRepository.authorize(
                request.getPan(),
                request.getAmount(),
                request.getCurrency()
        )).thenReturn(new AuthorizationProcedureResult(1L, "00"));

        when(transactionRepository.saveAndFlush(
                any(PaymentTransaction.class)
        )).thenThrow(new IllegalStateException("Fallo simulado de BD"));

        assertThrows(
                IllegalStateException.class,
                () -> service.authorize(request)
        );

        verifyNoInteractions(eventPublisher);
    }

    private AuthorizationRequest request() {

        AuthorizationRequest request = new AuthorizationRequest();

        request.setPan("9905980511891515456");
        request.setAmount(new BigDecimal("10000.00"));
        request.setCurrency("COP");

        return request;
    }
}
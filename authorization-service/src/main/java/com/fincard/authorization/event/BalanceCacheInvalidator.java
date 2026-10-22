package com.fincard.authorization.event;

import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.fincard.authorization.config.CacheConfig;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class BalanceCacheInvalidator {

    private final CacheManager cacheManager;

    @TransactionalEventListener(
        phase = TransactionPhase.AFTER_COMMIT
    )
    public void onPaymentApproved(PaymentApprovedEvent event) {

        try {
            var cache = cacheManager.getCache(
                    CacheConfig.CARD_BALANCES
            );

            if (cache != null) {
                cache.evict(event.pan());

                log.info(
                        "Pago confirmado: cache de saldo invalidada"
                );
            }

        } catch (RuntimeException exception) {

            log.warn(
                    "Pago confirmado; no se pudo invalidar la cache. "
                            + "Tipo de error: {}",
                    exception.getClass().getSimpleName()
            );
        }
    }
}
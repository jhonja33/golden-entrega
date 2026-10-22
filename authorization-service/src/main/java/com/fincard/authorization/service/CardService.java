package com.fincard.authorization.service;

import java.util.Optional;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.fincard.authorization.config.CacheConfig;
import com.fincard.authorization.dto.BalanceResponse;
import com.fincard.authorization.model.Card;
import com.fincard.authorization.repository.CardRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CardService implements ICardService {

    private final CardRepository cardRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<Card> findByPan(String pan) {
        return cardRepository.findByPan(pan);
    }

    @Override
    @Cacheable(
        cacheNames = CacheConfig.CARD_BALANCES,
        key = "#p0"
    )
    public BalanceResponse getBalance(String pan) {

        log.info("Consulta de saldo: leyendo MySQL");

        Card card = cardRepository.findByPan(pan)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Tarjeta no encontrada"
                ));

        return new BalanceResponse(
                card.getCurrency(),
                card.getAvailableBalance(),
                card.getCreditLimit()
        );
    }
}
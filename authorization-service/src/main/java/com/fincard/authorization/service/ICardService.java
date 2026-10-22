package com.fincard.authorization.service;

import java.util.Optional;

import com.fincard.authorization.dto.BalanceResponse;
import com.fincard.authorization.model.Card;

public interface ICardService {

    public Optional<Card> findByPan(String pan);

    public BalanceResponse getBalance(String pan);
}
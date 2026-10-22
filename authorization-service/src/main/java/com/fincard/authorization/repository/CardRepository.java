package com.fincard.authorization.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fincard.authorization.model.Card;

public interface CardRepository extends JpaRepository<Card, Long> {
	public Optional<Card> findByPan(String pan);
}
package com.fincard.authorization.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cards")
@Data
@NoArgsConstructor
public class Card {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "pan", nullable = false, length = 19)
	private String pan;

	@Column(name = "card_type", nullable = false, length = 10)
	private String cardType;

	@Column(name = "status", nullable = false, length = 10)
	private String status;

	@Column(name = "expiration_date", nullable = false)
	private LocalDate expirationDate;

	@Column(name = "currency", nullable = false, length = 3, columnDefinition = "CHAR(3)")
	private String currency;

	@Column(name = "available_balance", nullable = false, precision = 19, scale = 2)
	private BigDecimal availableBalance;

	@Column(name = "credit_limit", precision = 19, scale = 2)
	private BigDecimal creditLimit;
}
package com.fincard.authorization.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
public class PaymentTransaction {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "transaction_id", nullable = false, length = 36)
	private String transactionId;

	@Column(name = "card_id")
	private Long cardId;

	@Column(name = "pan", nullable = false, length = 19)
	private String pan;

	@Column(name = "amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal amount;

	@Column(name = "currency", nullable = false, length = 3, columnDefinition = "CHAR(3)")
	private String currency;

	@Column(name = "status", nullable = false, length = 10)
	private String status;

	@Column(name = "response_code", nullable = false, length = 2)
	private String responseCode;

	@Column(name = "created_at", nullable = false, columnDefinition = "DATETIME(6)")
	private LocalDateTime createdAt;
}
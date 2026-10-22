package com.fincard.authorization.repository;

import java.math.BigDecimal;
import java.sql.Types;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

import com.fincard.authorization.dto.AuthorizationProcedureResult;

@Repository
public class PaymentProcedureRepository {

	private final SimpleJdbcCall authorizePaymentCall;

	public PaymentProcedureRepository(JdbcTemplate jdbcTemplate) {

		this.authorizePaymentCall = new SimpleJdbcCall(jdbcTemplate).withProcedureName("sp_authorize_payment")
				.withoutProcedureColumnMetaDataAccess().declareParameters(new SqlParameter("p_pan", Types.VARCHAR),
						new SqlParameter("p_amount", Types.DECIMAL), new SqlParameter("p_currency", Types.CHAR),
						new SqlOutParameter("p_card_id", Types.BIGINT),
						new SqlOutParameter("p_response_code", Types.VARCHAR));
	}

	public AuthorizationProcedureResult authorize(String pan, BigDecimal amount, String currency) {

		MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("p_pan", pan)
				.addValue("p_amount", amount).addValue("p_currency", currency);

		Map<String, Object> result = authorizePaymentCall.execute(parameters);

		Number cardIdValue = (Number) result.get("p_card_id");

		Long cardId = cardIdValue == null ? null : cardIdValue.longValue();

		String responseCode = (String) result.get("p_response_code");

		if (responseCode == null) {
			throw new IllegalStateException("El procedimiento no devolvio codigo de respuesta");
		}

		return new AuthorizationProcedureResult(cardId, responseCode);
	}
}
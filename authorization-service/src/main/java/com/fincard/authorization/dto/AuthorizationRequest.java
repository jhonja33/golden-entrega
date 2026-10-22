package com.fincard.authorization.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AuthorizationRequest {

    @NotBlank(message = "El PAN es obligatorio")
    @Pattern(
        regexp = "[0-9]{19}",
        message = "El PAN debe contener exactamente 19 digitos"
    )
    private String pan;

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(
        value = "0.01",
        message = "El monto debe ser mayor o igual a 0.01"
    )
    @Digits(
        integer = 17,
        fraction = 2,
        message = "El monto debe tener maximo 17 enteros y 2 decimales"
    )
    private BigDecimal amount;

    @NotBlank(message = "La moneda es obligatoria")
    @Pattern(
        regexp = "COP",
        message = "El prototipo admite unicamente COP"
    )
    private String currency;
}
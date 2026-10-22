package com.fincard.authorization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class BalanceRequest {

    @NotBlank(message = "El PAN es obligatorio")
    @Pattern(
        regexp = "[0-9]{19}",
        message = "El PAN debe contener 19 digitos"
    )
    private String pan;
}
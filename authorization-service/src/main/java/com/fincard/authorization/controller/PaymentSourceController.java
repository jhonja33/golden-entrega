package com.fincard.authorization.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fincard.authorization.dto.AuthorizationRequest;
import com.fincard.authorization.dto.AuthorizationResponse;
import com.fincard.authorization.dto.BalanceRequest;
import com.fincard.authorization.dto.BalanceResponse;
import com.fincard.authorization.dto.TokenResponse;
import com.fincard.authorization.service.ICardService;
import com.fincard.authorization.service.IPaymentService;
import com.fincard.authorization.service.ITokenService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(
    name = "FinCard",
    description = "Autenticacion, pagos y consulta de saldos"
)
public class PaymentSourceController {

    private final IPaymentService paymentService;
    private final ITokenService tokenService;
    private final ICardService cardService;

    @Operation(
        summary = "Obtener un token JWT",
        description = "Requiere HTTP Basic. "
                + "Devuelve un token con vigencia de 900 segundos.",
        security = @SecurityRequirement(name = "basicAuth")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Token generado",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = TokenResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Credenciales ausentes o incorrectas",
            content = @Content
        )
    })
    @PostMapping("/auth/token")
    public ResponseEntity<TokenResponse> generateToken(
            @Parameter(hidden = true)
            Authentication authentication) {

        TokenResponse response =
                tokenService.generateToken(authentication);

        log.info("Token JWT emitido para cliente autenticado");

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header("Pragma", "no-cache")
                .body(response);
    }

    @Operation(
        summary = "Autorizar un pago",
        description = """
                Valida la tarjeta y el saldo o cupo disponible.
                Un resultado de negocio aprobado o rechazado devuelve HTTP 200.
                El campo responseCode indica el resultado:
                00 aprobado, 51 fondos insuficientes, 54 tarjeta vencida,
                62 tarjeta restringida, 56 tarjeta no registrada,
                57 transaccion no permitida, 12 transaccion invalida.
                Los datos que no superan la validacion de entrada devuelven HTTP 400.
                """,
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Resultado de la autorizacion",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    implementation = AuthorizationResponse.class
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "JSON o campos de entrada invalidos",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Token ausente, invalido o vencido",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Usuario sin permiso de autorizacion",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Error tecnico durante el procesamiento",
            content = @Content
        )
    })
    @PostMapping("/payments/authorizations")
    public ResponseEntity<AuthorizationResponse> authorize(
            @Valid @RequestBody AuthorizationRequest request) {

        log.info(
                "Solicitud de autorizacion: amount={}, currency={}",
                request.getAmount(),
                request.getCurrency()
        );

        AuthorizationResponse response =
                paymentService.authorize(request);

        log.info(
                "Respuesta de autorizacion: transactionId={}, "
                        + "status={}, responseCode={}",
                response.getTransactionId(),
                response.getStatus(),
                response.getResponseCode()
        );

        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Consultar saldo y cupo",
        description = """
                Consulta informativa con cache Redis de 15 segundos.
                La autorizacion de pagos utiliza el saldo definitivo de MySQL.
                """,
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Saldo consultado",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = BalanceResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "PAN o JSON de entrada invalido",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Token ausente, invalido o vencido",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Usuario sin permiso de consulta",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Tarjeta no encontrada",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Error tecnico durante la consulta",
            content = @Content
        )
    })
    @PostMapping("/cards/balance")
    public ResponseEntity<BalanceResponse> getBalance(
            @Valid @RequestBody BalanceRequest request) {

        BalanceResponse response =
                cardService.getBalance(request.getPan());

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(response);
    }
}
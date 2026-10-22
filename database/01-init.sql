-- =============================================
-- Author:      Jhon Jairo Bohorquez
-- Create date: 21 Octubre 2026
-- Description: Creacion de base de datos, tablas, SP
--              y datos de prueba de FinCard Dummy.
-- Ejecutar completo con MySQL
-- Requiere usuario con permisos CREATE DATABASE/TABLE/ROUTINE.
-- PAN ficticios: solo pruebas locales.
-- =============================================


/******************************************************************************
    CREACION DE LA BASE DE DATOS
******************************************************************************/

CREATE DATABASE IF NOT EXISTS fincard_authorization
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE fincard_authorization;


/******************************************************************************
    CREACION DE LA TABLA DE TARJETAS
******************************************************************************/
CREATE TABLE IF NOT EXISTS cards (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    pan                 VARCHAR(19)     NOT NULL,
    card_type           VARCHAR(10)     NOT NULL,
    status              VARCHAR(10)     NOT NULL,
    expiration_date     DATE            NOT NULL,
    currency            CHAR(3)         NOT NULL,
    available_balance   DECIMAL(19,2)   NOT NULL,
    credit_limit        DECIMAL(19,2)   NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_cards_pan UNIQUE (pan)
) ENGINE=InnoDB;


/******************************************************************************
    CREACION DE LA TABLA DE TRANSACCIONES
******************************************************************************/

CREATE TABLE IF NOT EXISTS transactions (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    transaction_id      VARCHAR(36)     NOT NULL,
    card_id             BIGINT          NULL,
    pan                 VARCHAR(19)     NOT NULL,
    amount              DECIMAL(19,2)   NOT NULL,
    currency            CHAR(3)         NOT NULL,
    status              VARCHAR(10)     NOT NULL,
    response_code       VARCHAR(2)      NOT NULL,
    created_at          DATETIME(6)     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_transactions_id UNIQUE (transaction_id),
    CONSTRAINT fk_transactions_pan_card FOREIGN KEY (card_id) 
	REFERENCES cards (id),
	INDEX idx_transactions_card_date (card_id, created_at)
) ENGINE=InnoDB;


/******************************************************************************
    CREACION DEL SP
    El llamador debe iniciar una transaccion.
    Spring controla COMMIT y ROLLBACK con @Transactional.
******************************************************************************/

DROP PROCEDURE IF EXISTS sp_authorize_payment;

DELIMITER $$

CREATE PROCEDURE sp_authorize_payment(
    IN p_pan VARCHAR(19),
    IN p_amount DECIMAL(19,2),
    IN p_currency CHAR(3),
    OUT p_card_id BIGINT,
    OUT p_response_code VARCHAR(2)
)
BEGIN
    DECLARE v_found BOOLEAN DEFAULT TRUE;
    DECLARE v_status VARCHAR(10);
    DECLARE v_card_type VARCHAR(10);
    DECLARE v_expiration_date DATE;
    DECLARE v_currency CHAR(3);
    DECLARE v_available_balance DECIMAL(19,2);
    DECLARE v_credit_limit DECIMAL(19,2);

    DECLARE CONTINUE HANDLER FOR NOT FOUND
        SET v_found = FALSE;

    SET p_card_id = NULL;
    SET p_response_code = NULL;

    IF p_pan IS NULL
        OR CHAR_LENGTH(p_pan) <> 19
        OR p_pan REGEXP '[^0-9]' THEN

        SET p_response_code = '14';

    ELSEIF p_amount IS NULL OR p_amount <= 0 THEN

        SET p_response_code = '13';

    ELSE
        SELECT
            id,
            status,
            card_type,
            expiration_date,
            currency,
            available_balance,
            credit_limit
        INTO
            p_card_id,
            v_status,
            v_card_type,
            v_expiration_date,
            v_currency,
            v_available_balance,
            v_credit_limit
        FROM cards
        WHERE pan = p_pan
        FOR UPDATE;

        IF v_found = FALSE THEN
            SET p_response_code = '56';

        ELSEIF v_status = 'BLOCKED' THEN
            SET p_response_code = '62';

        ELSEIF v_status <> 'ACTIVE' THEN
            SET p_response_code = '57';

        ELSEIF v_card_type NOT IN ('DEBIT', 'CREDIT') THEN
            SET p_response_code = '57';

        ELSEIF v_expiration_date < UTC_DATE() THEN
            SET p_response_code = '54';

        ELSEIF p_currency IS NULL
            OR p_currency <> v_currency
            OR p_currency <> 'COP' THEN
            SET p_response_code = '12';

        ELSEIF v_available_balance < 0 THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'Invalid stored balance';

        ELSEIF v_card_type = 'CREDIT'
            AND (
                v_credit_limit IS NULL
                OR v_credit_limit < 0
                OR v_available_balance > v_credit_limit
            ) THEN

            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'Invalid stored credit limit';

        ELSEIF v_available_balance < p_amount THEN
            SET p_response_code = '51';

        ELSE
            UPDATE cards
            SET available_balance = available_balance - p_amount
            WHERE id = p_card_id;

            SET p_response_code = '00';
        END IF;
    END IF;
END$$

DELIMITER ;


/******************************************************************************
    INSERCION DE DATOS DE PRUEBA
    Insercion directa, sin validacion previa de existencia.
******************************************************************************/
INSERT INTO cards
    (pan, card_type, status, expiration_date, currency,
     available_balance, credit_limit)
VALUES
	-- Tarjeta debito activa con fondos disponibles.
    ('9905980511891515456', 'DEBIT', 'ACTIVE', '2099-12-31', 'COP', 500000.00, NULL),
    -- Tarjeta bloqueada.-- Tarjeta bloqueada.
	('9908472615306841927', 'DEBIT', 'BLOCKED', '2099-12-31', 'COP', 500000.00, NULL),
    -- Tarjeta vencida.
	('9903628157490261834', 'DEBIT', 'ACTIVE', '2020-12-31', 'COP', 500000.00, NULL),
    -- Tarjeta con fondos insuficientes para compras mayores a 1000.
	('9907154830269518472', 'DEBIT', 'ACTIVE', '2099-12-31', 'COP', 1000.00, NULL),
    -- Tarjeta credito con cupo total y cupo disponible.
	('9902846175039281648', 'CREDIT', 'ACTIVE', '2099-12-31', 'COP', 300000.00, 500000.00);



/******************************************************************************
    CONSULTAS DE VERIFICACION
******************************************************************************/

SELECT DATABASE() AS current_database;

SHOW TABLES;

SELECT
    id,
    pan,
    card_type,
    status,
    expiration_date,
    currency,
    available_balance,
    credit_limit
FROM cards
ORDER BY id;

SELECT COUNT(*) AS total_transactions FROM transactions;

SHOW CREATE PROCEDURE sp_authorize_payment;
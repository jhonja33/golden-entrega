# C4 — Nivel 3: Componentes de autorización

```mermaid
flowchart TB
    cliente["Cliente REST"]

    subgraph core["authorization-service"]
        seguridad["SecurityConfig<br/>Autenticación y validación JWT"]
        controller["PaymentSourceController<br/>Endpoints REST"]
        token["TokenService<br/>Emisión de JWT"]
        pago["PaymentSinkService<br/>Orquesta la autorización"]
        saldo["CardService<br/>Consulta saldo y cupo"]
        persistencia["Repositorios<br/>PaymentProcedureRepository<br/>PaymentTransactionRepository<br/>CardRepository"]
        cache["CacheConfig y BalanceCacheInvalidator<br/>Caché e invalidación tras commit"]
        eventos["PaymentKafkaPublisher<br/>Publicación tras commit"]
    end

    mysql[("MySQL")]
    redis[("Redis")]
    kafka["Kafka"]

    cliente --> seguridad
    seguridad --> controller
    controller --> token
    controller --> pago
    controller --> saldo

    pago --> persistencia
    saldo --> persistencia
    persistencia --> mysql

    saldo --> cache
    pago -->|"Evento interno de aprobación"| cache
    cache --> redis

    pago -->|"Evento interno de aprobación"| eventos
    eventos --> kafka
```

PaymentSinkService coordina, dentro de una transacción de Spring,
la llamada al procedimiento almacenado y el registro del movimiento.

El procedimiento bloquea la tarjeta durante la autorización mediante
SELECT FOR UPDATE y actualiza su saldo. La transacción se confirma
cuando finaliza correctamente el servicio.

Después del commit, los listeners invalidan la caché de saldo y
publican el evento aprobado en Kafka.
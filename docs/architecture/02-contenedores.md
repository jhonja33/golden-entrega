# C4 — Nivel 2: Contenedores

```mermaid
flowchart TB
    cliente["Sistema adquirente / Postman"]

    subgraph fincard["Sistema FinCard"]
        auth["authorization-service<br/>Java 21 / Spring Boot<br/>API REST y JWT"]
        mysql[("MySQL<br/>Tarjetas, transacciones y procedimiento")]
        redis[("Redis<br/>Caché de saldo y cupo")]
        kafka["Kafka<br/>Topic: transaction-events"]
        audit["audit-service<br/>Java 21 / Spring Boot<br/>Consumidor de eventos"]
        archivo[("Volumen de auditoría<br/>transactions-audit.jsonl")]
    end

    cliente -->|"HTTP / JSON y JWT"| auth
    auth -->|"JPA y JDBC / procedimiento"| mysql
    auth -->|"Consulta e invalida caché"| redis
    auth -->|"Publica pagos aprobados después del commit"| kafka
    kafka -->|"Entrega eventos"| audit
    audit -->|"Registra eventos sin duplicar eventId"| archivo
```

MySQL puede ejecutarse en el equipo anfitrión o mediante el Compose
opcional. El servicio de auditoría no expone una API HTTP.

La publicación directa a Kafka después del commit no garantiza entrega
si ocurre un fallo entre la confirmación de MySQL y la publicación.
Queda como mejora para producción reintentos y mejor configuracion apra el servicio de auditoria y Notificacion.
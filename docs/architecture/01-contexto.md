# C4 — Nivel 1: Contexto

FinCard permite autorizar pagos, consultar saldo y cupo, y registrar
la auditoría de las transacciones aprobadas.

```mermaid
flowchart TB
    cliente["Sistema adquirente<br/>Solicita autorizaciones y consultas"]
    fincard["Sistema FinCard<br/>Autoriza pagos y registra eventos"]
    operador["Operador técnico<br/>Consulta registros de auditoría"]

    cliente -->|"API REST con JWT"| fincard
    operador -->|"Revisa archivo JSONL y logs"| fincard
```

Alcance del prototipo: tarjetas ficticias, autorización local y auditoría.
No existe conexión con una red de pagos real.

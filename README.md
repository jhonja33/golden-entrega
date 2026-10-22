# FinCard — Autorización de pagos Dummy

Prototipo de autorización de pagos con tarjetas ficticias, desarrollado
con Java 21 y Spring Boot.

Incluye autorización REST protegida con JWT, persistencia en MySQL,
consulta de saldo y cupo con caché Redis, publicación de eventos en Kafka
y un microservicio independiente de auditoría.

## 1. Tecnologías

- Java 21.
- Spring Boot.
- Spring Web MVC.
- Spring Security y JWT.
- Spring Data JPA / Hibernate.
- Spring Data Redis y Spring Cache.
- Spring Kafka.
- MySQL.
- Maven.
- Lombok.
- JUnit y Mockito.
- OpenAPI / Swagger.
- Docker Compose.

## 2. Organización del proyecto

| Carpeta | Contenido |
|---|---|
| authorization-service | API de autorización, seguridad, consultas y publicación de eventos |
| audit-service | Consumo de eventos y registro de auditoría |
| database | Script de creación de base de datos, tablas, procedimiento y datos ficticios |
| docker | Configuraciones de ejecución con Docker Compose |
| docs/architecture | Diagramas C4 |

## 3. Flujo de autorización

1. El cliente obtiene un JWT utilizando las credenciales de demostración.
2. Envía la solicitud de pago con el token Bearer.
3. Spring Security valida el token y los permisos.
4. El servicio invoca el procedimiento almacenado de autorización.
5. El procedimiento consulta los datos de la tarjeta.
6. Valida estado, vigencia, moneda y fondos/cupo disponible.
7. Si aprueba, descuenta el importe del saldo disponible.
8. El servicio registra el resultado en la tabla transactions.
9. Spring confirma la transacción de base de datos.
10. Para pagos aprobados, los listeners invalidan la caché y publican
    un evento en Kafka.
11. El microservicio de auditoría consume el evento y lo registra en JSONL en un archivo de log.

El procedimiento y el registro de la transacción participan en la
transacción de Spring. El procedimiento no ejecuta un COMMIT independiente.

## 4. Consulta de saldo y cupo

La consulta utiliza Redis como caché de los resultados obtenidos de MySQL.

- Si existe una entrada vigente en Redis, se devuelve ese resultado.
- Si no existe, se consulta MySQL mediante el repositorio JPA y se
  almacena el resultado en Redis.
- La caché tiene un tiempo de vida configurable aclamente se dejo de 15 segundos.
- Después de confirmar un pago aprobado se invalida la entrada de
  la tarjeta para que una siguiente consulta obtenga el saldo actualizado.

MySQL es el almacenamiento definitivo. Redis no se utiliza para
decidir si existen fondos suficientes para autorizar un pago.

## 5. Requisitos para ejecutar

Para la ejecución con contenedores:

- Docker Desktop iniciado, con soporte para contenedores Linux.
- Docker Compose.
- Acceso a Internet durante la primera construcción.
- Puertos 9090, 9092 y 6379 disponibles.
- MySQL local en el puerto 3306, o el contenedor opcional de MySQL.

Los Dockerfiles construyen los proyectos con Maven y Java 21.
No es necesario instalar Maven en el equipo para construir las imágenes.

Para ejecución desde el IDE se requiere Java 21 y acceso a MySQL,
Redis y Kafka.

## 6. Alternativa A: MySQL instalado en Windows

### Preparar la base de datos

Ejecutar el archivo:

database/01-init.sql

El script crea:

- Base de datos fincard_authorization.
- Tabla cards.
- Tabla transactions.
- Procedimiento sp_authorize_payment.
- Datos ficticios de prueba.

Los INSERT de datos de prueba son directos. No se deben ejecutar
repetidamente sobre las mismas tarjetas, porque el PAN es único.

La configuración de demostración utiliza datos dummy que son:

- Usuario: root.
- Contraseña: admin.
- Puerto: 3306.

MySQL debe permitir la conexión desde Docker Desktop. El contenedor
de autorización accede al equipo mediante host.docker.internal.

### Levantar las aplicaciones, Redis y Kafka

Desde CMD, entrar en la carpeta docker del proyecto:

```bat
cd docker
```

Limpiar una posible selección previa de MySQL en Docker:

```bat
set "FINCARD_DB_URL="
```

Validar e iniciar:

```bat
docker compose config --quiet
docker compose up -d --build
```

Este comando inicia Redis, Kafka y los dos microservicios.
No inicia MySQL.

Consultar estado:

```bat
docker compose ps -a
```

Consultar logs:

```bat
docker compose logs -f authorization-service audit-service
```

Los servicios kafka-permissions y kafka-topic son tareas de preparación.
Es normal que finalicen con estado Exited (0).

## 7. Alternativa B: MySQL en Docker

Desde la carpeta docker, iniciar primero MySQL y esperar a que
termine la inicialización:

```bat
docker compose -f docker-compose.mysql.yml up -d --wait
```

En la misma terminal CMD, configurar la conexión:

```bat
set "FINCARD_DB_URL=jdbc:mysql://mysql:3306/fincard_authorization?connectionTimeZone=UTC"
```

Iniciar el conjunto:

```bat
docker compose -f docker-compose.yml -f docker-compose.mysql.yml up -d --build
```

En PowerShell, la variable se configura así:

```powershell
$env:FINCARD_DB_URL = "jdbc:mysql://mysql:3306/fincard_authorization?connectionTimeZone=UTC"
```

El MySQL del contenedor utiliza:

- Desde Windows: localhost:3307.
- Desde otros contenedores: mysql:3306.
- Usuario: root.
- Contraseña: admin.

Esta base de datos es independiente de la instalada en Windows.

El script de inicialización se ejecuta cuando el volumen de MySQL
está vacío. Reiniciar el contenedor no vuelve a ejecutar el script.

## 8. Detener y reiniciar

Sin MySQL en Docker:

```bat
docker compose stop
docker compose up -d
```

Con MySQL en Docker:

```bat
docker compose -f docker-compose.yml -f docker-compose.mysql.yml stop
docker compose -f docker-compose.yml -f docker-compose.mysql.yml up -d
```

Para la segunda alternativa, mantener configurada FINCARD_DB_URL
en la terminal utilizada para iniciar el conjunto.

Si cambia el código Java, reconstruir las imágenes con:

```bat
docker compose up -d --build
```

Si se utiliza MySQL en Docker, agregar ambos argumentos -f como
en los comandos de esa alternativa.

No utilizar down -v si se desea conservar los datos de los volúmenes.

## 9. API y Swagger

Swagger:

http://localhost:9090/swagger-ui.html

Definición OpenAPI:

http://localhost:9090/v3/api-docs

### Obtener token

POST http://localhost:9090/api/v1/auth/token

En Postman:

1. Seleccionar Authorization.
2. Elegir Basic Auth.
3. Ingresar usuario root y contraseña admin.
4. Enviar la petición.

La respuesta contiene:

- accessToken: JWT que debe enviarse en las peticiones protegidas.
- tokenType: Bearer.
- expiresIn: duración del token en segundos, 900.

### Autorizar un pago

POST http://localhost:9090/api/v1/payments/authorizations

En Authorization seleccionar Bearer Token e ingresar el accessToken.

Enviar el cuerpo como JSON:

```json
{
  "pan": "9905980511891515456",
  "amount": 10000.00,
  "currency": "COP"
}
```

Una aprobación devuelve status APPROVED y responseCode 00.
Un rechazo de negocio devuelve status DECLINED y el código correspondiente.

### Consultar saldo y cupo

POST http://localhost:9090/api/v1/cards/balance

Utilizar el mismo mecanismo Bearer Token y enviar:

```json
{
  "pan": "9905980511891515456"
}
```

La respuesta contiene la moneda, el saldo disponible y el límite
de crédito cuando corresponde.

### Códigos principales de negocio

| Código | Significado |
|---|---|
| 00 | Transacción aprobada |
| 51 | Fondos o cupo disponible insuficientes |
| 54 | Tarjeta vencida |
| 62 | Tarjeta restringida o bloqueada |
| 56 | Tarjeta no registrada |
| 14 | Número de tarjeta inválido |
| 13 | Importe inválido |
| 57 | Transacción no permitida |
| 12 | Transacción inválida, incluida moneda no admitida |

Los códigos de negocio se representan como String para conservar
ceros iniciales.

La validación de entrada REST puede rechazar una petición antes de
invocar el procedimiento. Por ejemplo, un PAN con formato incorrecto
produce una respuesta HTTP 400.

### Respuestas HTTP

- 200: operación procesada; revisar status y responseCode en pagos.
- 400: solicitud con errores de validación.
- 401: credenciales o token ausentes/inválidos.
- 403: permisos insuficientes.
- 404: tarjeta no encontrada en la consulta de saldo.
- 500: error técnico no controlado.

Un pago rechazado por fondos insuficientes es un resultado de negocio,
no un error técnico del servidor.

## 10. Auditoría y Kafka

Los pagos aprobados se publican en el tópico transaction-events.

audit-service consume estos eventos y los registra en:

/app/data/transactions-audit.log

Cada línea contiene un objeto JSON. El archivo se conserva en un
volumen Docker y los eventos también se muestran en los logs.

Consultar registros:

```bat
docker compose logs --tail=50 audit-service
```

Consultar las últimas líneas del archivo:

```bat
docker compose exec audit-service tail -n 10 /app/data/transactions-audit.log
```

Exportar una copia al equipo:

```bat
docker compose cp audit-service:/app/data/transactions-audit.log ./transactions-audit-export.log
```

Al arrancar, auditoría lee los identificadores existentes del archivo
y vuelve a recorrer los eventos disponibles en Kafka. Agrega los que
no estén registrados.

Si el archivo está vacío o no existe, puede reconstruirse con los
eventos que Kafka todavía conserve según su política de retención.

Esta implementación está diseñada para una instancia de auditoría y
un volumen pequeño de eventos. No garantiza reconstruir eventos
que Kafka ya haya eliminado.

Las notificaciones externas no están implementadas en esta versión.
Se contempla como algo adiconal que se puede construir incorporando un proveedor de correo,
webhook u otro canal a partir de los eventos aprobados.

## 11. Pruebas

Se incluye PaymentSinkServiceTest con JUnit y Mockito para comprobar:

- Autorización aprobada y publicación del evento interno.
- Rechazo por fondos insuficientes sin publicar evento de aprobación.
- Error al guardar la transacción sin publicar evento de aprobación.

Desde authorization-service:

```bat
mvnw.cmd -Dtest=PaymentSinkServiceTest test
```

Estas pruebas utilizan mocks y no requieren MySQL, Redis ni Kafka.

No verifican el commit real de una transacción, el procedimiento
almacenado ni la entrega efectiva del evento a Kafka.

La suite automatizada actual se limita a pruebas unitarias.

Comprobación manual del flujo:

1. Obtener JWT.
2. Consultar saldo inicial.
3. Autorizar un pago.
4. Consultar saldo actualizado.
5. Verificar la transacción en MySQL.
6. Verificar el evento y el archivo de auditoría.
7. Probar rechazos por bloqueo, vencimiento y fondos insuficientes.

## 12. Arquitectura C4

Se documentan los tres niveles solicitados:

- [Nivel 1: Contexto](docs/architecture/01-contexto.md).
- [Nivel 2: Contenedores](docs/architecture/02-contenedores.md).
- [Nivel 3: Componentes de autorización](docs/architecture/03-componentes-autorizacion.md).

## 13. Decisiones de arquitectura — ADRs

### ADR-001: Dos microservicios

Decisión: separar autorización y auditoría.

Motivo: la autorización responde a la solicitud del cliente y la
auditoría procesa eventos de manera asincrona.

Consecuencia: pueden ejecutarse por separado, pero es necesario
configurar y supervisar la comunicación mediante Kafka.

### ADR-002: MySQL y procedimiento almacenado

Decisión: usar MySQL como almacenamiento definitivo y un procedimiento
para validar la tarjeta y descontar el saldo.

Motivo: MySQL es una de las bases permitidas en el requisito de
persistencia y permite realizar la operación con transacciones
y bloqueo de filas.

El apartado de Docker del enunciado menciona Oracle/Postgres/MySQL.
Esta entrega mantiene MySQL, admitido en el apartado de persistencia,
y proporciona una alternativa de ejecución en Docker.

### ADR-003: Redis para consultas recurrentes

Decisión: utilizar caché y alamacenamiento en redis para consultas de saldo.

Motivo: reducir lecturas repetidas de MySQL.

Consecuencia: la caché puede presentar información temporalmente
desactualizada. Se aplica expiración e invalidación después de
pagos aprobados. La autorización siempre consulta la base de datos.


### ADR-004: JWT y credenciales de demostración

Decisión: proteger la API con Spring Security y JWT de corta duración.

Motivo: autenticar al cliente al obtener el token y validar el token
en las siguientes operaciones.

Consecuencia: el prototipo utiliza un usuario local y una clave de
firma de demostración. Las credenciales incluidas son exclusivamente
para pruebas y no deben reutilizarse en producción.

En producción se propone un proveedor de identidad y gestión externa
de secretos, con rotación de claves y HTTPS.

### ADR-005: Auditoría en JSONL

Decisión: registrar un objeto JSON por línea en un archivo persistente.

Motivo: facilitar la inspección de los eventos sin incorporar otra
base de datos al consumidor.

Consecuencia: la deduplicación utiliza identificadores cargados en
memoria y está orientada a una sola instancia. No constituye una
garantía de procesamiento exactamente una vez.

### ADR-006: Docker Compose modular

Decisión: mantener archivos individuales por componente y un Compose
principal.

Motivo: permitir ejecución parcial o conjunta y conservar MySQL
como dependencia local u opcional en Docker.

Consecuencia: las aplicaciones usan nombres de servicio para
comunicarse dentro de la red Docker.

## 14. Evolución para producción

Estas medidas son propuestas; no forman parte de la implementación actual, 
se sugiere que se deben adactar varias configuraciones si y temas de seguridad 
si se quisera podner en produccion.

### Escalabilidad

- Desplegar autorización con varias réplicas en Kubernetes.
- Mantener el estado compartido en MySQL y Redis.
- Dimensionar el pool de conexiones y medir la contención por tarjeta.
- Aumentar las particiones de Kafka según el volumen y los requisitos
  de orden de procesamiento.
- Sustituir el archivo de auditoría por almacenamiento compartido
  antes de ejecutar múltiples consumidores que escriban auditoría.

### Resiliencia

- Incorporar idempotencia de las solicitudes de pago.
- Configurar reintentos limitados y un tópico de mensajes fallidos.
- Definir timeouts, alertas y recuperación de dependencias.
- Configurar alta disponibilidad y copias de seguridad.

### Observabilidad

- Centralizar logs con identificador de transacción.
- Medir latencia, errores, aprobaciones/rechazos, aciertos de caché
  y retraso de consumidores Kafka.
- Incorporar trazas distribuidas y paneles de monitoreo.
- Evitar registrar tokens, secretos o datos completos de tarjetas reales.

### CI/CD

- Ejecutar compilación y pruebas en cada cambio.
- Agregar pruebas de integración con servicios reales o Testcontainers.
- Analizar dependencias y calidad del código.
- Construir imágenes versionadas.
- Desplegar primero en un ambiente de pruebas(QA) y promover la misma imagen.

## 15. Estrategia Git

La estrategia propuesta para esta entrega es Trunk-based Development:

- main como rama principal.
- Cambios pequeños mediante ramas de corta duración cuando corresponda.
- Commits semánticos.

Ejemplos:

- feat: implementar autorizacion de pagos
- feat: publicar eventos aprobados en kafka
- test: agregar pruebas unitarias de autorizacion
- chore: configurar docker compose
- docs: documentar arquitectura y ejecucion

## 16. Alcance del prototipo

- Se utilizan PAN ficticios y datos de demostración.
- No existe conexión una red financiera real.
- Los códigos de respuesta adoptan convenciones del dominio transaccional del switch prostilion.
- Las notificaciones externas quedan como evolución.
- La suite automatizada actual cubre pruebas unitarias.

## Ejcuion de ocnsumo desde Postman
Para consumir se puede descargar desde la carpeta Postman que esta entrega con el nombre 
authorization-service.postman_collection.json e importarlo para que se pueda consumir tener 
en cuentas que son datos dummy, y se debe configurar de acuerdo a los  datos que se configuren 
en la base de datos y en el archivo  "application.properties" del servicio authorization-service


## Descargar la entrega

El servicio de autorización está incluido como submódulo Git.

Clonar el proyecto completo:

```bash
git clone --recurse-submodules https://github.com/jhonja33/golden-entrega.git
cd golden-entrega
```

Si se clonó sin descargar el submódulo authorization-service descargarlo desde esmodulo
https://github.com/jhonja33/authorization-service.git usando :
```bash
git submodule update --init --recursive
```
o

git clone --recurse-submodules https://github.com/jhonja33/authorization-service.git
```

Después, seguir las instrucciones de ejecución con Docker de este README.
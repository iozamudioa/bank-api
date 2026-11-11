# BANK — Backend v1

API para control familiar por **sobres físicos y digitales**, presupuesto quincenal y movimientos con saldos persistidos.

**Coordenadas Maven:** `dev.ioza:bank-api:0.1.0-SNAPSHOT`  
**Paquete Java:** `dev.ioza.bank`  
**Dominio privado previsto:** `bank.ioza.dev` (sin configuración DNS/TLS incluida).  
**Stack:** Java 17, Spring Boot 4.1.1, Spring MVC, Spring Data MongoDB, MongoDB Decimal128, Maven.

## Contrato para la PWA y Swagger

- [Guía funcional y pantallas](docs/FRONTEND_HANDOFF.md).
- [Integración TypeScript, autenticación, dinero y errores](docs/FRONTEND_INTEGRATION.md).
- [Contrato OpenAPI generado](docs/openapi/bank-api-v1.yaml) y [procedimiento de generación/verificación](docs/OPENAPI.md).
- [Ejemplos de requests y respuestas](docs/examples/README.md).

Con la aplicación arrancada: Swagger UI en `/swagger-ui.html`, JSON en `/v3/api-docs` y YAML en `/v3/api-docs.yaml`. Springdoc 3.2.0; documentación pública, operaciones protegidas por HTTP Basic y permisos por hogar. Generar el contrato sin MongoDB con `./scripts/export-openapi.ps1` o `mvn '-Dtest=OpenApiContractTest' '-Dbank.openapi.export=true' test`; la suite normal verifica que el archivo siga coincidiendo con la API generada. No se implementa una PWA en este repositorio.

## El principio financiero

- `Account.balance` representa dinero real (efectivo / digital).
- `Envelope.balance` representa fondos *reservados dentro de una cuenta*; no dinero adicional.
- `Movement` registra los movimientos y los saldos antes/después para auditoría.
- `account.balance >= suma(envelope.balance WHERE envelope.accountId = account.id)` siempre.
- Un `ALLOCATE` cambia el saldo del sobre, **nunca** el total de la cuenta.
- Un `REALLOCATE` entre sobres de la misma cuenta no modifica la cuenta.
- Una `TRANSFER` mueve efectivo entre cuentas sin alterar el patrimonio total.
- Una `ENVELOPE_TRANSFER` mueve un saldo reservado entre cuentas y sobres, con entrada y salida igual.
- Un `EXPENSE` baja la cuenta exactamente lo que disminuyen sus sobres financiadores.
- Escrituras de cuenta + sobres + historial participan en **una transacción MongoDB**.

### Ejemplo que protegemos

Antes: Cuenta física $1,080 / Central $80 / Sam's $1,000.  
Compra carne $150 desde Central, $80 de Central y $70 financiados por Sam's.  
Después: Cuenta física **$930** / Central **$0** / Sam's **$930**.  
Gasto único de $150, con sus dos fuentes registradas.

## Árbol de paquetes

```text
src/main/java/dev/ioza/bank/
├── BankApplication.java
├── domain/                        # Money, model, enums, exceptions (sin Spring)
├── application/
│   ├── commands/                 # entradas independientes de HTTP
│   ├── queries/                  # filtros de movimientos
│   ├── usecase/                  # contratos de operaciones específicas
│   ├── ports/in/                  # contratos de casos de uso
│   ├── ports/out/                 # contratos de persistencia / usuario
│   └── services/                  # reglas, movimientos transaccionales
└── infrastructure/
    ├── adapters/
    │   ├── in/controllers/        # REST + request/response DTOs + advice
    │   └── out/mongo/
    │       ├── documents/         # @Document + Decimal128
    │       ├── repositories/      # MongoRepository
    │       └── mappers/           # documentos ↔ dominio
    ├── config/                    # configuración técnica y transacciones
    └── security/                  # autenticación y autorización HTTP
```

## Arranque local en Windows / VS Code

Requiere Java 17+, Maven y Docker Desktop. **No es necesario desplegar la PWA todavía.**

1. Ejecuta `docker compose up -d mongo` y después `docker compose run --rm mongo-init`. El inicializador espera a que rs0 tenga un PRIMARY antes de terminar.
2. Configura variables de entorno (`.env.example` es una referencia; Spring no carga `.env` automáticamente):

```powershell
$env:BANK_OWNER_USERNAME = 'olda'
$env:BANK_OWNER_PASSWORD = 'COLOCA_UNA_CLAVE_SEGURA'
$env:BANK_CECY_USERNAME = 'cecy'
$env:BANK_CECY_PASSWORD = 'COLOCA_OTRA_CLAVE_SEGURA'
$env:MONGODB_URI = 'mongodb://localhost:27017/bank_db?replicaSet=rs0&directConnection=true'
mvn clean test
mvn spring-boot:run
```

3. `http://localhost:8080/actuator/health` debe responder `UP` cuando Mongo esté disponible.
4. Revisa `docs/API.http` (extensión *REST Client* de VS Code) y sustituye los IDs devueltos. `curl -u olda:...` también sirve.
5. Si quieres precargar el catálogo de los **23 sobres** y las **tres quincenas de 2026** sin dinero inventado, usa `scripts/bootstrap.py`.

> MongoDB **standalone** no soporta las transacciones multidocumento que usa este proyecto. Debe estar configurado como replica set. El Compose incluido es para desarrollo en un solo nodo; en producción, usa replica set robusto y copias de seguridad.

## Flujo de onboarding

1. Crear hogar (creador queda `OWNER`).
2. Invitar integrante autorizado (`EDITOR`/`VIEWER`).
3. Crear cuentas `PHYSICAL` y `DIGITAL`, saldo inicial cero.
4. Registrar saldo inicial real de cada cuenta (`POST /movements/opening-balances`). No repetir si ya registraste dinero.
5. Registrar fuentes de ingreso (catálogo; no crea depósitos).
6. Crear sobres ligados a cuenta física o digital, sin saldo inicial automático.
7. Guardar plantilla quincenal con presupuesto por sobre; abrir periodo copiando snapshot del plan.
8. **Registrar ingreso real** (en `movements/incomes`), luego transferir dinero a físico y asignar los sobres (en `allocations`).
9. Registrar gastos con fondos suficientes o `expenses/preview` para ver opciones; `expenses` permite múltiples donantes en un solo gasto.

### Endpoints resumidos

Prefijo `GET|POST|PUT /api/v1/households/{householdId}`:

| Método | Path | Función |
|---|---|---|
| POST | `/api/v1/households` | Crear hogar |
| GET | `/api/v1/households` | Mis hogares |
| POST | `/members` | Agregar miembro (solo owner) |
| POST/GET | `/accounts` | Crear/listar cuentas |
| POST/GET | `/envelopes` | Crear/listar sobres |
| POST/GET | `/income-sources` | Crear/listar orígenes |
| PUT/GET | `/budget-template` | Guardar/ver plantilla |
| POST/GET | `/budget-periods` | Abrir/listar quincenas |
| POST | `/budget-periods/{id}/close` | Cerrar quincena |
| GET | `/dashboard` | Cuentas, reservado, libre |
| POST | `/movements/opening-balances` | Registrar saldo inicial |
| POST | `/movements/incomes` | Ingreso real |
| POST | `/movements/allocations` | Apartar dinero |
| POST | `/movements/releases` | Liberar apartado |
| POST | `/movements/reallocations` | Mover entre sobres misma cuenta |
| POST | `/movements/transfers` | Transferir dinero libre entre cuentas |
| POST | `/movements/envelope-transfers` | Mover dinero reservado entre cuentas |
| POST | `/movements/expenses/preview` | Ver faltante y donantes elegibles |
| POST | `/movements/expenses` | Confirmar gasto financiado por 1+ sobres |
| GET | `/movements` | Lista paginada; filtros periodId/accountId/envelopeId, page=0 y size=1..100 |
| POST | `/movements/account-expenses` | Gasto desde saldo libre, sin alterar reservas |
| POST | `/movements/account-expenses/preview` | Vista previa de total y libre |
| POST | `/movements/{id}/corrections` | Compensacion y reemplazo atomicos con historial |
| POST | `/movements/{id}/corrections/preview` | Vista previa de efectos netos |
| POST | `/movements/{id}/reverse` | Revertir movimiento si saldos actuales lo permiten |

### JSON monetario

Campos `amount` en Java son `BigDecimal` y Mongo usa `Decimal128`. En las peticiones usa números decimales, por ejemplo `"amount":150.00` (Jackson deserializa a BigDecimal sin pasar por Double).

## Seguridad y alcance de esta v1

**Versión inicial para desarrollo local o acceso privado por WireGuard, NO producción pública.** Incluye HTTP Basic con dos usuarios configurados por variables de entorno, permisos por hogar y BCrypt para credenciales en memoria. Incluye `Idempotency-Key` obligatoria en operaciones financieras. No incluye todavía onboarding de usuarios completo, OIDC, cookies de sesión ni rate limits. Para la PWA y uso público hay que reemplazar este mecanismo.

No se autosembran saldos, ingresos ni gastos, porque serían movimientos ficticios. `BudgetTemplate` define *lo planificado*, no un apartado real.

**Verificación de esta revisión:** se ejecutaron Maven y pruebas con Java 17. Consulta [el informe](docs/REFACTOR_REPORT.md) para resultados exactos. Las pruebas con MongoDB real están incluidas, pero fueron omitidas porque este equipo no dispone de Docker; no se considera validada la atomicidad real.

> **Nota del script de arranque:** `scripts/bootstrap.py` crea un hogar *nuevo* y el catálogo de sobres. No lo ejecutes dos veces sobre el mismo contexto; si ya creaste tu hogar a mano, continúa desde allí. Para el entorno de prueba se crean tres periodos de las fechas solicitadas, sin transacciones ni saldos inventados.

## Cambio de nombre desde la v1 anterior

Esta entrega conserva la lógica financiera de la v1 anterior y actualiza la identidad a **BANK**.
- `dev.ioza.payments` → `dev.ioza.bank`
- `PaymentsApplication` → `BankApplication`
- artifact Maven → `bank-api`; nombre Spring → `bank-api`
- Mongo local predeterminada → `bank_db`; contenedor → `bank-mongo`
- Configuración auth → `BANK_OWNER_*` y `BANK_CECY_*`

**Atención:** el cambio de URI crea una base de datos local distinta de la antigua.
No copies movimientos ni saldos automáticamente. El bootstrap es solo para un entorno vacío.
El dominio `bank.ioza.dev` es un nombre previsto, no una URL desplegada.

## Revisión arquitectónica y uso actualizado

Cada tipo tiene su propio archivo Java. El dominio no depende de Spring ni de MongoDB; los DTOs REST se convierten a commands y los documentos monetarios usan Decimal128 explícito, incluidos los valores embebidos del historial. La política monetaria rechaza más de dos decimales y exceder la precisión Decimal128; no hay redondeo implícito.

Todos los POST que cambian dinero requieren `Idempotency-Key` de 1 a 128 caracteres; preview no la requiere. Es un cambio de contrato deliberado para impedir duplicados. Repite la misma clave y datos si hubo un fallo de red o conflicto concurrente. Usa otra clave para una nueva operación. Consulta [contratos y ejemplos](docs/ENDPOINTS.md), [decisiones de arquitectura](docs/ARCHITECTURE.md) y [archivos de la revisión](docs/FILES_CHANGED.md).

### Docker con la API

Con las credenciales en variables de entorno o en un archivo .env local (Compose sí lo carga):

```powershell
docker compose --profile app up --build -d
```

El perfil app construye el Dockerfile con Java 17, espera al inicializador del replica set y publica la API en localhost:8080. MongoDB usa un volumen persistente y sólo publica su puerto en localhost. No borres el volumen para actualizar la aplicación. Docker y el arranque real no fueron ejecutados en este equipo.

### Pruebas

```powershell
mvn clean test
mvn clean package
# Con Docker Engine en marcha; exige pruebas MongoDB sin omisiones:
./scripts/verify-mongo.ps1
```

Las pruebas Testcontainers utilizan una base efímera independiente; no usan bank_db ni credenciales reales. Sin Docker, JUnit las marca como omitidas. Que Maven termine correctamente con pruebas omitidas no demuestra rollback ni seguridad ante concurrencia.

### Variables de entorno

| Variable | Uso |
|---|---|
| MONGODB_URI | URI con replica set. Predeterminada localhost:27017/bank_db?replicaSet=rs0&directConnection=true |
| BANK_OWNER_USERNAME / BANK_OWNER_PASSWORD | Identidad local del propietario (usuario predeterminado olda; contraseña obligatoria) |
| BANK_CECY_USERNAME / BANK_CECY_PASSWORD | Segunda identidad local (usuario predeterminado cecy; contraseña obligatoria) |
| PORT | Puerto HTTP; predeterminado 8080 |
| BANK_BASE_URL | Sólo para bootstrap.py; predeterminado http://localhost:8080/api/v1 |

El backend comprueba la topología de MongoDB al arrancar y rechaza standalone. Respaldar y probar una copia de los datos existentes antes de actualizar: revisar metadata _class, importes heredados en cadenas e índices únicos. Véanse las limitaciones y decisiones de migración en ARCHITECTURE.md.

Gastos desde saldo libre y correcciones atomicas de movimientos: [contrato e integracion](docs/ACCOUNT_EXPENSES_AND_CORRECTIONS.md).

Presupuesto habitual por sobre sin quincena: [alta atomica, consulta y edicion por linea](docs/HABITUAL_ENVELOPE_BUDGET.md).

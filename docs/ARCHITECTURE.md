# Arquitectura y decisiones de BANK v1

Se refactoriza el código existente; permanecen Java 17, `dev.ioza:bank-api`, Spring Boot 4.1.1 y las rutas REST.

Todos los adaptadores de entrada y salida pertenecen a `dev.ioza.bank.infrastructure.adapters`. La configuración técnica pertenece a `infrastructure.config` y la seguridad a `infrastructure.security`. `domain` y `application` no importan infraestructura. El escaneo de componentes y repositorios continúa desde `dev.ioza.bank`, por lo que los componentes reubicados conservan su descubrimiento e inyección automática.

```text
dev.ioza.bank
├── domain
│   ├── Money
│   ├── model                  entidades y valores independientes de infraestructura
│   ├── enums
│   └── exceptions             validación, conflicto, autorización y ausencia de recursos
├── application
│   ├── commands              entradas independientes de HTTP
│   ├── queries               filtros y límites de consulta
│   ├── usecase               contratos de operaciones específicas
│   ├── ports/in              fachadas de operaciones disponibles
│   ├── ports/out             persistencia, identidad e idempotencia
│   └── services              orquestación y transacciones
└── infrastructure
    ├── adapters
    │   ├── in/controllers
    │   │   ├── request        Jakarta Validation y conversión a commands
    │   │   ├── response       contratos JSON independientes del dominio
    │   │   └── advice         traducción global de errores a HTTP
    │   └── out/mongo
    │       ├── documents      documentos raíz y valores embebidos Decimal128
    │       ├── repositories   repositorios Spring Data
    │       └── mappers        conversión explícita dominio/documentos
    ├── config                configuración técnica y transacciones
    └── security              autenticación y autorización HTTP
```

Cada tipo público está en su propio archivo. Se conserva `MovementService.Changes` como clase interna técnica: agrupa el estado temporal de una única operación, valida su resultado y captura los efectos de auditoría. Las clases internas de configuración/fallo de las pruebas son fixtures técnicos.

## Entidades e identidad

Household, membresía del hogar, Account, IncomeSource, Envelope, BudgetTemplate, BudgetPeriod y Movement tienen documentos propios. BudgetLine, Funding y BalanceEffect son valores embebidos con documentos separados y representación monetaria explícita. Los documentos no son respuestas REST ni modelos del dominio.

La identidad de usuario sigue gestionada por Spring Security en memoria. No existía una entidad persistente User: no se crea una colección de usuarios desconectada de la autenticación real. `username` vincula la identidad autenticada y la membresía del hogar. Migrar usuarios a un proveedor OIDC y usar un identificador inmutable requiere una siguiente versión.

## Dinero y concurrencia

Importes BigDecimal, escala 2 y precisión máxima 34 para Decimal128. Se rechazan importes de más de dos decimales, incluso ceros adicionales, y valores fuera de precisión; no se redondea dinero silenciosamente. Comparaciones numéricas con `compareTo` o `signum`.

Account conserva dinero real y Envelope una reserva dentro de esa cuenta. `Changes` calcula todos los saldos, exige fondos libres no negativos, guarda cuentas y sobres y finalmente guarda un movimiento con sus efectos. Incluso las reservas que no alteran el total de una cuenta guardan su versión: así se serializan asignaciones simultáneas contra la misma cuenta.

`@Version` en AccountDoc y EnvelopeDoc, transacciones snapshot y write concern majority protegen las escrituras. Toda operación financiera escribe además una revisión técnica de BudgetPeriod: el cierre, la edición de líneas del plan abierto y los movimientos concurrentes compiten por el mismo documento. La revisión del hogar serializa creación de periodos y edición de plantilla para impedir planes superpuestos por carreras. Estas revisiones no son saldos ni se publican en JSON.

Las reversiones validan los saldos actuales, registran efectos inversos y marcan el original dentro de la misma transacción. Un índice parcial único impide más de una reversión por movimiento. El saldo inicial queda limitado a una única operación histórica por cuenta, incluso si vuelve a cero o se revierte.

## Idempotencia y compatibilidad REST

Los POST que cambian dinero requieren `Idempotency-Key` (1–128 caracteres). Es un cambio deliberado del contrato para impedir duplicados por reintentos. Preview y consultas no requieren la cabecera. Se mantienen rutas, campos JSON y códigos de éxito existentes.

La clave está limitada por hogar y se almacena usando un identificador SHA-256. El fingerprint incluye la operación (y el identificador original en reversiones) y el JSON del command tipado. El orden de propiedades de entrada y campos desconocidos no cambian el command; el orden de listas y la representación decimal sí pueden cambiar el fingerprint. Reintentar con los mismos datos y clave devuelve la respuesta original, aun después de una reversión. Se comprueba autorización antes de leer una respuesta guardada.

El registro de idempotencia se inserta en la misma transacción que el movimiento. Una carrera puede responder 409 al perdedor; debe repetir con la misma clave para recuperar el resultado confirmado. No se promete que ambas solicitudes simultáneas reciban éxito inmediatamente. No hay TTL: eliminar registros permitiría repetir operaciones antiguas.

GET movements mantiene una lista JSON y admite page (base cero), size (1–100), periodId, accountId y envelopeId. Los filtros se aplican antes de paginar en MongoDB; por defecto conserva los últimos 100. El orden usa occurredAt y _id descendentes. La paginación por offset puede desplazarse si llegan movimientos entre páginas.

## Operación y migración

El arranque comprueba `hello` y rechaza standalone. Esta comprobación de topología no demuestra rollback ni concurrencia: eso corresponde a las pruebas con replica set real. MongoDB debe ser compatible con transacciones y tener un PRIMARY disponible.

Antes de actualizar una base existente, respaldar y probar en una copia: cambian los nombres Java de los tipos y su metadata `_class`. Revisar documentos heredados con BigDecimal en cadenas y migrarlos explícitamente a Decimal128 con validación de totales. No hay migración automática ni conversión silenciosa de dinero. El índice único de reversiones fallará si ya existen reversiones duplicadas; resolver los datos auditándolos antes del despliegue.

La API sigue orientada a desarrollo local o acceso privado. HTTP Basic en memoria, ausencia de OIDC/invitaciones verificadas y MongoDB local sin autenticación impiden considerar esta configuración apta para exposición pública. No hay modificaciones directas de saldos por REST.

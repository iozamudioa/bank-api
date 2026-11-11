# Presupuesto habitual desde sobres

Disponible en BANK API v1 desde 2026-11-11. No requiere una quincena abierta.

## Alta atomica

```http
POST /api/v1/households/{householdId}/envelopes
Authorization: Basic <credenciales>
Idempotency-Key: sobre-central-001
Content-Type: application/json

{
  "name":"Central de Abastos",
  "accountId":"account-cash",
  "type":"SPENDING",
  "category":"Alimentacion",
  "habitualAmount":4500.00
}
```

Respuesta HTTP 200: EnvelopeResponse conserva sus campos y agrega habitualAmount como numero decimal o null. balance inicia en 0.00. habitualAmount es planeacion, no reserva, ingreso ni saldo.

habitualAmount es opcional para compatibilidad. Omitido o null crea el sobre sin configurar un plan y no crea plantilla. Un cero explicito SI crea su linea habitual, con amount=0.00. Importes negativos, mas de dos decimales o fuera de Money/Decimal128 se rechazan sin redondear.

Cuando se proporciona el importe, una unica transaccion crea el sobre y agrega su linea habitual. Si falta plantilla se crea con expectedIncome=0; si existe conserva id, ingreso esperado y demas lineas. Fallar al guardar plantilla revierte el alta del sobre. Tambien se revierte el registro de reintento si cualquier escritura falla.

### Reintentos del alta

Idempotency-Key es opcional por compatibilidad, pero el cliente debe enviarla para poder reintentar sin duplicar sobres. Acepta 1 a 128 caracteres no vacios. Su ambito es la creacion de sobres dentro de un hogar, separado de las claves de movimientos financieros. Misma clave y command tipado devuelven la respuesta historica original; contenido distinto produce 409. La escala decimal forma parte del fingerprint: conservar cuerpo y clave al repetir.

La respuesta se almacena en envelope_creation_requests junto con el alta y plantilla dentro de la misma transaccion. Una carrera puede devolver 409; reintentar con la misma clave/cuerpo. Sin clave, repetir POST crea otro sobre. No se deduplica por nombre.

Si el plan habitual o saldo cambia despues, un replay conserva el resultado original del alta y no restaura montos viejos. Consultar GET /envelopes o GET /budget-template para obtener el estado actual. Se verifica permiso OWNER/EDITOR activo incluso al hacer replay.

## Editar un solo monto habitual

```http
PUT /api/v1/households/{householdId}/budget-template/lines/{envelopeId}
Authorization: Basic <credenciales>
Content-Type: application/json

{"amount":4800.00}
```

Devuelve HTTP 200 con BudgetTemplateResponse actualizado. Inserta la linea si falta o reemplaza exclusivamente su importe, conservando las otras lineas y expectedIncome. Crea la primera plantilla con expectedIncome=0 si es necesario. Cero valido; amount obligatorio y no negativo. PUT idempotente sin Idempotency-Key. No requiere periodo, fecha o ingreso esperado.

La transaccion valida membresia activa OWNER/EDITOR, sobre activo y cuenta activa del mismo hogar. Comparte la revision/bloqueo del hogar con guardar plantilla completa, crear periodos y alta de sobres con plan. Dos cambios de lineas distintas se realizan contra la plantilla del servidor, sin enviar ni perder las lineas ajenas. Ante conflicto 409 consultar y reintentar.

PUT /budget-template mantiene su significado de REEMPLAZO COMPLETO de la plantilla. No tiene If-Match ni fusiona un cuerpo obtenido de una lectura antigua; puede sustituir cambios anteriores por decision del cliente. Para editar un solo sobre usar siempre el nuevo PUT de linea.

## Consulta y quincenas

GET /api/v1/households/{householdId}/envelopes incluye habitualAmount para cada sobre, derivado de la linea de la plantilla. null indica que no existe esa linea; 0.00 indica plan explicitamente en cero. Se lee plantilla una sola vez, dentro de una lectura transaccional, sin necesitar periodo y sin crear documentos al consultar. No se agrega otro monto persistido al documento del sobre: la plantilla sigue siendo la unica fuente de verdad del plan actual. La copia historica de idempotencia solo sirve para replay.

GET /budget-template sigue funcionando y responde 404 si no existe. Crear periodo copia expectedIncome y lineas habituales confirmadas bajo el mismo bloqueo. Se permite expectedIncome=0: no se calcula como suma de planes ni se registra un ingreso automatico. Se conserva la validacion actual de apertura (plantilla existente, fechas validas y sin traslapes); esta entrega no agrega una restriccion nueva sobre planes vacios o en cero.

Editar lo habitual afecta los proximos periodos. No cambia ningun snapshot abierto o cerrado. Para ajustar tambien una quincena OPEN debe enviarse expresamente PUT /budget-periods/{periodId}/lines/{envelopeId}; es una operacion independiente. Ninguno de estos guardados deposita, aparta, libera, transfiere ni genera movimientos.

## Errores y permisos

400: importe/referencia invalida o no disponible. 401: Basic ausente o invalido. 403: sin permiso de escritura/membresia activa. 404: cuenta o sobre inexistente. 409: conflicto concurrente o clave del alta reutilizada con otro contenido. 503: persistencia no disponible/resultado incierto; repetir alta con misma clave y cuerpo. 500: error inesperado. Las lecturas admiten VIEWER activo.

## Integracion del frontend

- Crear sobre puede pedir Planeado por quincena desde la configuracion inicial y enviar habitualAmount en el mismo POST, con clave estable hasta confirmar.
- La tarjeta muestra habitualAmount del listado y lo edita mediante PUT de linea habitual, sin reconstruir plantilla completa ni exigir quincena.
- Distinguir Habitual de Solo esta quincena. Un cambio del habitual no se aplica implicitamente al periodo seleccionado.
- Refrescar listado y plantilla al confirmar. Reintentar POST con clave original si el resultado es incierto.
- Abrir quincena copia estos planes; no exige capturarlos otra vez ni expectedIncome mayor a cero. Cargar saldo y apartar dinero siguen siendo pasos financieros separados.

Esta entrega implementa API y contrato. La interfaz debe consumir los nuevos campos y operacion.

## Pruebas

Cobertura de servicio/HTTP: alta sin plantilla, compatibilidad sin campo, cero frente a null, preservacion de lineas/ingresos/saldos/snapshots, permisos, referencias de otro hogar/inactivas, dinero invalido, replay historico estable y conflictos de clave. OpenAPI verifica opcionalidad, numeros decimales, respuesta nullable y cabecera opcional.

Pruebas MongoDB a?adidas: alta con replay y BSON Decimal128, rollback de sobre/plantilla/clave al fallar escritura de plantilla, unicidad y concurrencia de altas/lineas/primera plantilla y carreras con createPeriod/saveTemplate. Requieren Docker; se omiten explicitamente si no existe.

Resultado local final: compilacion limpia con Java 17 y `mvn clean -Dbank.openapi.export=true package` correcta. Suite: 86 pruebas detectadas, 61 aprobadas y 25 omitidas por falta de Docker (incluyen siete pruebas nuevas de MongoDB). Contrato publicado/exportado: 37 operaciones. Cliente TypeScript generado correctamente con openapi-typescript 7.13.0. JAR en target/bank-api-0.1.0-SNAPSHOT.jar. No se ejecutaron cambios sobre la base real del usuario.

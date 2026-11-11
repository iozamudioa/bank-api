# Integración técnica React + TypeScript

Esta guía describe el backend implementado. No incluye una PWA ni cambia el formato monetario para adaptarlo a JavaScript. La fuente generada de tipos es `docs/openapi/bank-api-v1.yaml`; reglas funcionales y pantallas en `FRONTEND_HANDOFF.md`.

## Origen, ambientes y CORS

Desarrollo: servidor `http://localhost:8080`, recursos `/api/v1`. Swagger `/swagger-ui.html`; contrato JSON `/v3/api-docs` y YAML `/v3/api-docs.yaml`. OpenAPI declara servidor relativo `/` y cada path incluye `/api/v1`; no duplicar ese prefijo al generar clientes.

No hay un dominio público desplegado por este repositorio. bank.ioza.dev es sólo el nombre previsto. El frontend debe configurar su URL de servidor por ambiente, por ejemplo `VITE_BANK_API_ORIGIN`, una variable del proyecto frontend, no una variable que ya lea BANK. No incrustar credenciales en variables VITE ni en el bundle público.

**CORS no está habilitado.** La cadena de seguridad no configura `cors()` ni lista orígenes. Una PWA en otro origen no puede asumir que podrá usar Authorization o Idempotency-Key: el preflight puede ser rechazado. Usar un proxy de desarrollo del frontend para `/api` hacia localhost:8080, y un reverse proxy del mismo origen en despliegue. Si se decide separar orígenes, primero acordar e implementar CORS limitado en backend; no está resuelto en esta entrega. El esquema OpenAPI no habilita CORS.

## Autenticación y cabeceras

HTTP Basic sin sesión: `Authorization: Basic base64(username:password)` en cada petición de negocio. El operador configura BANK_OWNER_USERNAME/PASSWORD y BANK_CECY_USERNAME/PASSWORD; usuarios en memoria, contraseñas BCrypt. No hay JWT, refresh token, endpoint `/login`, cookies de sesión ni registro de usuarios. GET `/api/v1/me` verifica credenciales y devuelve `{username}`. GET hogares sólo devuelve membresías activas. Agregar un miembro no crea su identidad ni contraseña.

Usar HTTPS fuera de localhost y conservar credenciales sólo durante la sesión de uso de la PWA; no guardar Authorization en localStorage, caches de service worker ni logs. La API no ofrece un cierre de sesión de servidor: retirar las credenciales del cliente. Swagger no persiste autorización en su configuración.

Cabeceras comunes: `Accept: application/json`, `Content-Type: application/json` si hay cuerpo y Authorization. Todos los POST que cambian dinero añaden Idempotency-Key; crear catálogos, guardar plantilla, crear/cerrar periodo y preview no la requieren. Documentación OpenAPI/Swagger y `/actuator/health` son públicos; los recursos `/api/v1/**` siguen autenticados.

## Idempotencia y recuperación

Generar una clave opaca de 1–128 caracteres por operación financiera lógica (por ejemplo UUID). Mantener clave y command mientras haya resultado incierto. Deshabilitar doble clic ayuda a la UI, pero la protección real está en backend.

El ámbito es el hogar: una clave no se puede reutilizar para otro endpoint financiero o movimiento original de reversión dentro del hogar. El fingerprint usa operación y command tipado serializado. El orden de propiedades del JSON no es relevante; el orden de arrays y la escala decimal pueden cambiar el fingerprint. Conservar exactamente los mismos valores/formato, no reconstruir importes de otra manera al repetir. null y array vacío pueden representar commands diferentes aun cuando una regla financiera les dé el mismo efecto.

Misma clave y command confirmado: 200 con respuesta original, sin nuevo movimiento. Misma clave con otro command: 409. Solicitudes simultáneas pueden producir un 409 al perdedor; al repetir recuperará la respuesta confirmada. No hay TTL. Una respuesta idempotente puede contener status POSTED y saldos históricos aunque el original ya haya sido revertido; para estado actual consultar movimiento/dashboard/sobres otra vez.

Ante desconexión o 503, no registrar una operación nueva ni cambiar la clave. Repetir la original con backoff limitado hasta aclarar el resultado; no hay endpoint para consultar una clave directamente. Un 409 puede ser clave incompatible, periodo cerrado, reversión previa o carrera: no reintentar indefinidamente. Recuperar la petición original idéntica cuando hay duda, refrescar recursos y explicar un conflicto persistente al usuario. Sólo después de aclarar el resultado debe iniciarse una nueva operación con datos y clave nuevos. Los catálogos no tienen idempotencia; no aplicarles reintentos ciegos.

## Formato monetario exacto

Java usa BigDecimal; MongoDB Decimal128. REST usa **números JSON decimales**, por ejemplo `{"amount":150.00}` y `{"balance":930.00}`. No son strings, centavos enteros ni objetos Extended JSON `$numberDecimal`. El contrato OpenAPI declara `type: number`, `format: decimal`. No se cambió ese formato. JSON no garantiza conservar ceros finales: el valor numérico 150 y 150.00 es el mismo para presentación, aunque se debe conservar el command al reintentar.

Requests monetarios: hasta 32 dígitos enteros, hasta dos decimales, sin redondeo silencioso. Más de dos decimales, incluso ceros extra como 1.000, se rechaza. amount en movimientos/funding es >0; importes planificados son >=0; expectedAmount de fuente es opcional. Persistencia y cálculos de saldo normalizan escala 2 y validan precisión 34. change puede ser negativo; saldos, free y reservas no pueden quedar negativos en operaciones admitidas.

JavaScript number utiliza punto flotante binario y no puede representar con exactitud todo el rango del contrato. No usar parseFloat, sumas de number, Math.round para arreglar importes ni convertir todo a centavos como modelo del backend. Mantener entrada monetaria como texto decimal local y usar una librería decimal para sumar, comparar y repartir fondos. Configurar suficiente precisión: 34 dígitos o más, no el default de 20 de algunas librerías.

Para soportar el rango completo, leer `response.text()` y usar un parser JSON sin pérdida numérica, como [lossless-json](https://github.com/josdejong/lossless-json), antes de convertir los importes a la librería decimal. Su parse conserva números mediante LosslessNumber y stringify puede emitirlos como números JSON. Para enviar, construir valores numéricos sin pérdida desde texto decimal y serializar con esa librería; no usar JSON.stringify sobre strings monetarios porque cambiaría el contrato a strings. No realizar `response.json()` antes del parser sin pérdida: el daño a números grandes ya estaría hecho. Convertir explícitamente los enteros no monetarios (version, page) cuando corresponda. No utilizar regex para modificar JSON.

Los tipos generados interpretan `number` como number de TypeScript. Eso describe el transporte, no autoriza cálculos financieros binarios. Adaptar los campos con format decimal al tipo monetario local en la capa de transporte del frontend; no hacer casts de LosslessNumber a number fingiendo que hubo conversión. Si el frontend impone un límite más pequeño para una primera versión, declararlo como limitación de la UI y validarlo antes de enviar; el backend no aplica ese límite reducido.

## Errores y reacción del cliente

Los errores gestionados y los de autenticación/autorización usan `{ "error": "BAD_REQUEST", "message": "..." }`, donde error es el nombre simbólico del estado HTTP. No hay code financiero, lista de campos, requestId ni detalles Mongo en ese DTO. message es para humanos y no debe parsearse como código estable. Un proxy o fallo de red podría devolver texto/HTML o nada: no asumir siempre JSON.

| HTTP | Significado real / acción |
|---|---|
| 400 | JSON/DTO/importe/filtros inválidos; también saldo insuficiente o financiación incorrecta. Conservar formulario, refrescar saldos y corregir antes de una nueva operación |
| 401 | Basic ausente o inválido; respuesta con WWW-Authenticate y JSON UNAUTHORIZED. Solicitar credenciales sin repetir escrituras con otra clave |
| 403 | Falta membresía activa, rol de edición o OWNER. Actualizar UI y bloquear acción |
| 404 | Hogar/recurso inexistente o referencia no disponible para el hogar. Recargar catálogo y permitir seleccionar otra referencia |
| 409 | Quincena cerrada, reversión ya efectuada, clave incompatible, versión o transacción concurrente. Resolver según contexto; recuperar resultado incierto con misma clave |
| 503 | Persistencia no disponible o confirmación incierta. Recuperar mediante misma clave/command, backoff limitado |
| 500 | Error inesperado, JSON sin mensaje interno del driver. Mostrar error y preservar la operación incierta |

422 no se utiliza. Un gasto con saldo insuficiente es 400, una transferencia de dinero reservado por la ruta de dinero libre es 409, y un periodo cerrado es 409. Un mismo estado no identifica una única regla. Errores de protocolo como método o media type incorrectos pueden usar otros estados HTTP del framework; no inventar un 422 para ellos.

## Paginación, fechas y saldos

Sólo el historial implementa paginación: GET H`/movements?page=0&size=20&periodId=...&accountId=...&envelopeId=...`. page>=0, size 1..100 (default 100); filtros opcionales AND. Respuesta lista sin total/hasNext/cursor. Orden occurredAt y _id descendentes. Offset puede desplazarse con nuevas escrituras; al actualizar filtros volver a page=0. Los demás catálogos son listas completas.

BudgetPeriod usa fechas locales ISO YYYY-MM-DD; no convertirlas con `new Date()` y UTC para editar calendario, porque puede cambiar el día. occurredAt usa Instant UTC ISO-8601 con precisión de milisegundos; representar en la zona del hogar (actualmente America/Mexico_City), sin asumir offset fijo todo el año. El servidor asigna actor/fecha: no se envían al registrar un movimiento. La fecha actual de registro puede ser distinta al rango de la quincena: el backend exige OPEN, no verifica que hoy esté dentro del rango.

Al completar una escritura usar MovementResponse para mostrar comprobante/efectos y recargar cuentas, sobres, dashboard e historial relevantes. Respuesta de creación devuelve el recurso creado; gastos no devuelven listas completas ni dashboard. No aplicar efectos otra vez al recibir un replay; no reconstruir balances como fuente de verdad a partir de movimientos paginados. No hay WebSocket/SSE, edición directa de saldos ni revisiones condicionales If-Match en REST. version se expone sólo como dato técnico. Preview es una lectura sin bloqueo ni garantía de fondos.

## Tipos y cliente TypeScript desde OpenAPI

En el repositorio frontend, copiar el YAML exportado o apuntar a una copia versionada. Ejemplo de generación de tipos según la [CLI oficial de openapi-typescript](https://github.com/openapi-ts/openapi-typescript/blob/main/docs/cli.md):

```bash
npx openapi-typescript@7.13.0 ./bank-api-v1.yaml -o ./src/generated/bank-api.d.ts
```

Fijar la versión elegida en package.json y lockfile del frontend; el comando ilustra generación y no instala un frontend en BANK. Los operationId son estables y únicos. Si se usa un cliente generado, revisar que soporte Basic, cabecera Idempotency-Key por petición, cuerpos de error, listas sin paginador y parser monetario exacto configurable. Generar tipos no valida reglas financieras ni resuelve precisión de JavaScript.

Regenerar primero el contrato en backend con el procedimiento de `OPENAPI.md`; la suite normal detecta si el YAML quedó desactualizado. No mantener otro schema escrito a mano en la PWA.

## Planear un sobre en una quincena abierta

Disponible `PUT /api/v1/households/{householdId}/budget-periods/{periodId}/lines/{envelopeId}` con `{ "amount": 1500.00 }`. Inserta o actualiza lo planeado sin mover dinero ni cambiar la plantilla. Contrato, errores y recuperacion parcial al crear un sobre: [OPEN_PERIOD_ENVELOPE_PLAN.md](OPEN_PERIOD_ENVELOPE_PLAN.md).

## Gastos directos y correccion de movimientos

Disponibles POST H`/movements/account-expenses` y POST H`/movements/{id}/corrections`, ambos con Idempotency-Key, y sus variantes `/preview` sin escrituras. La correccion valida saldos netos y conserva original, compensacion y reemplazo. Contratos, variantes y recuperacion: [ACCOUNT_EXPENSES_AND_CORRECTIONS.md](ACCOUNT_EXPENSES_AND_CORRECTIONS.md).

## Presupuesto habitual desde cada sobre

POST H`/envelopes` admite `habitualAmount` opcional y `Idempotency-Key` opcional para alta atomica con plan. GET sobres incluye el habitual actual derivado de la plantilla. PUT H`/budget-template/lines/{envelopeId}` con `{amount}` actualiza un solo plan y crea la plantilla si falta, sin quincena ni movimientos. [Contrato y recuperacion](HABITUAL_ENVELOPE_BUDGET.md).

## Importe cero en correcciones

Solo los ReplacementRequest admiten cero. EXPENSE cero exige financiacion vacia y devuelve las aportaciones originales en la compensacion. MovementResponse.replacement conserva los recursos confirmados para volver a editar, incluso sin efectos con signo. [Contrato, compatibilidad y pruebas](ZERO_AMOUNT_CORRECTIONS.md).

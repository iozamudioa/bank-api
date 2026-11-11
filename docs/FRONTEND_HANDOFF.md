# BANK: contrato funcional para el agente de la PWA

Desarrollar la PWA independiente en React + TypeScript a partir de `openapi/bank-api-v1.yaml` y esta guía. No inferir endpoints de los nombres de entidades. Swagger en `/swagger-ui.html`, JSON en `/v3/api-docs`. Base de recursos: `/api/v1`. Todas las rutas indicadas como H usan `/api/v1/households/{householdId}`. Los identificadores son opacos.

## Contexto, usuarios y entidades

BANK controla dinero familiar en cuentas y su reserva en sobres. OWNER configura el hogar y agrega membresías; EDITOR registra y organiza dinero; VIEWER consulta, incluido preview. Todos necesitan credenciales HTTP Basic válidas y membresía activa para acceder al hogar. La API no crea cuentas de usuario ni envía invitaciones: únicamente relaciona usernames con hogares. Actualmente hay dos identidades en memoria configuradas por el operador. No mostrar registro, recuperación de contraseña o sesión con token como funciones disponibles.

| Entidad | Significado y datos visibles |
|---|---|
| Household | Hogar MXN, zona America/Mexico_City, nombre y active |
| HouseholdMember | username, displayName, rol y active; no contiene contraseña |
| Account | Cuenta PHYSICAL o DIGITAL; balance es dinero real persistido; version es técnica y de sólo lectura |
| Envelope | Reserva dentro de accountId. type es SPENDING/SAVINGS/GOAL; físico/digital se deriva de la cuenta. balance conserva remanentes |
| IncomeSource | Catálogo PAYROLL/TRANSFER/TANDA/OTHER, cuenta sugerida y expectedAmount opcional. No deposita dinero |
| BudgetTemplate | Plan habitual con expectedIncome y líneas por sobre; guardar reemplaza el plan completo |
| BudgetPeriod | Fechas, estado DRAFT/OPEN/CLOSED y snapshot del plan. Crear copia la plantilla actual; no hay copia directa de otro periodo |
| Movement | Operación auditada, actor, fecha UTC, importe, estado POSTED/REVERSED y efectos de cuentas/sobres |
| Funding | Aportación de un sobre a un único gasto; no es un gasto ni movimiento adicional |
| BalanceEffect | entityId, before, change, after; cambio con signo y posibles efectos cero |

Account.total o balance nunca se suma con Envelope.balance para calcular patrimonio. Dashboard.total es dinero real; reserved es parte de ese total; free=total-reserved. Un saldo planificado no es dinero existente ni dinero apartado. Los sobres no son cajas independientes de su cuenta.

## Configuración inicial en orden ejecutable

1. Validar credenciales con GET `/api/v1/me`, consultar hogares y crear uno con POST `/api/v1/households` si es necesario. El creador queda OWNER.
2. OWNER agrega al otro username mediante POST H`/members`; no es un flujo de alta de credenciales.
3. Crear cuentas PHYSICAL y DIGITAL mediante POST H`/accounts`; siempre nacen con balance cero.
4. Crear fuentes mediante POST H`/income-sources`, opcionales para registrar ingresos.
5. Capturar fechas de quincena en la UI, pero primero crear sobres y plantilla: la API exige plantilla existente para crear un periodo. No existe calendario recurrente ni generación automática.
6. Crear sobres mediante POST H`/envelopes` con cuenta; nacen con reserva cero.
7. PUT H`/budget-template` con expectedIncome y allocations; luego POST H`/budget-periods` con fechas y **open:true**. Omitir open crea DRAFT y no existe endpoint para abrirlo después.
8. Si ya existe dinero, POST H`/movements/opening-balances` una sola vez por cuenta. No volver a ingresar como nómina ese mismo dinero. Los ingresos futuros usan incomes.
9. Transferir a las cuentas que realmente recibirán el dinero, apartar y registrar gastos. Cada escritura financiera requiere una Idempotency-Key distinta por operación nueva.

`examples/requests/` contiene cuerpos completos por DTO; sustituir todos los IDs por respuestas reales. `examples/onboarding.http` muestra una secuencia autosuficiente. No ejecutar ejemplos contra dinero real sin revisar cada operación.

## Pantallas y estados de interacción

En todas las pantallas: mostrar carga sin presentar cero como saldo real; mantener los datos previos identificados como pendientes de actualizar. Bloquear sólo el envío duplicado mientras está pendiente una escritura. Al error conservar los campos y la clave de la operación para un reintento idéntico. 401 pide credenciales; 403 deshabilita escritura y explica permisos; 503/red incierta permite recuperar con la misma clave. No confirmar cambios sólo por cálculos locales.

### Inicio / Dashboard

Objetivo: conocer dinero real, reservado y libre. GET H`/dashboard` y GET H`/budget-periods`; presenta total/reserved/free y accounts. Captura selección de hogar/periodo, no saldos. Acciones: navegar a ingreso, gasto, apartados e historial. Validación: hogar seleccionado y permiso de lectura. Carga: esqueleto de totales; vacío: sin cuentas, ofrecer crearlas a editores. Errores: 401/403/404/503. Éxito: mostrar cada total sin doble conteo; refrescar tras movimientos.

### Cuentas

GET/POST H`/accounts`. Mostrar nombre, tipo, balance y active; para libre/reservado usar dashboard. Captura name y type PHYSICAL/DIGITAL; ambos obligatorios. Acciones: crear, registrar saldo inicial, ingreso o transferencia. No hay edición, eliminación ni GET individual de cuenta. Carga: lista pendiente; vacío: crear primera cuenta. Errores: 400/401/403/404/409/503. Éxito de crear: cuenta con saldo cero, luego recargar lista/dashboard.

### Sobres físicos y digitales

GET H`/envelopes` + GET H`/accounts`; agrupar por el tipo de la cuenta. POST H`/envelopes` captura name/accountId/type y category opcional; cuenta activa del hogar y tipo SPENDING/SAVINGS/GOAL. Acciones: crear, apartar, liberar o consultar detalle. Carga: esperar ambas listas; vacío: crear cuenta antes del primer sobre. Errores: cuenta inexistente 404, cuenta de otro hogar/inactiva 400 y errores comunes. Éxito: nuevo sobre con saldo cero.

### Detalle de sobre

No existe GET H`/envelopes/{id}`. Buscar el id en GET H`/envelopes`, relacionar cuenta y consultar GET H`/movements?envelopeId=...&page=0&size=20`. Mostrar reserva actual, cuenta, finalidad e historial; el periodo es filtro opcional. Captura filtros y datos del formulario de operación seleccionado. Acciones: apartar/liberar/gastar/reasignar/transferir reserva. No permitir editar balance. Carga: detalle e historial por separado; vacío: sobre sin movimientos, no es error; id ausente: explicar recurso no disponible. Éxito de una acción: actualizar detalle, cuenta, dashboard e historial.

### Nueva quincena

GET H`/budget-template` + GET H`/budget-periods`; POST H`/budget-periods`. Captura startDate/endDate y open:true para uso inmediato. Validar rango ordenado y anticipar traslapes; servidor es autoridad. No hay obligación implementada de empezar en un día fijo ni generación recurrente. Acciones: crear OPEN o, sólo si se desea conservar un borrador sin apertura posterior, DRAFT. Carga: obtener plantilla; vacío/404 de plantilla: crearla primero. 409: traslape o concurrencia, actualizar periodos; éxito: snapshot nuevo, saldos anteriores intactos.

### Presupuesto quincenal

GET/PUT H`/budget-template`, GET H`/budget-periods/{periodId}` y POST H`/budget-periods/{periodId}/close`. Captura expectedIncome y allocations completas de la plantilla. Importes >=0, sobres únicos y activos; lista vacía válida. Acciones: reemplazar plantilla, ver snapshot y cerrar OPEN. Editar plantilla no modifica snapshots; se puede insertar o reemplazar una línea en OPEN mediante PUT H`/budget-periods/{periodId}/lines/{envelopeId}` con `{amount}`. No existe reapertura. Carga: distinguir plan actual/histórico; vacío: sin plantilla/líneas. 400: líneas inválidas; 409: cierre inválido/concurrente; éxito: recargar recurso, sin alterar reservas.

### Registrar ingreso

GET cuentas/fuentes/periodos; POST H`/movements/incomes`. Captura periodId, accountId, amount>0, description opcional e incomeSourceId opcional. Cuenta y fuente activas del hogar, quincena OPEN; enviar cuenta explícita aunque la fuente tenga defaultAccountId. Acciones: confirmar/cancelar. Carga: catálogos y envío; vacío: falta cuenta o periodo OPEN, ofrecer configuración. 400: monto; 404: referencia; 409: periodo cerrado o concurrencia. Éxito: un INCOME aumenta sólo cuenta, luego refrescar dashboard.

### Registrar apartado / liberar reserva

Consultar sobres/cuentas/dashboard/periodos y POST H`/movements/allocations` o `/releases`. Captura periodId/envelopeId/amount>0/description opcional. Apartar exige free suficiente de la cuenta; liberar exige balance del sobre suficiente. Acciones: confirmar/cancelar; no transferir dinero físico implícitamente. Carga: saldos y envío; vacío: no hay sobres o fondos, explicar cómo ingresar/transferir. 400 por insuficiencia, 409 por periodo/carrera. Éxito: ALLOCATE o RELEASE modifica reserva/libre, conserva dinero real.

### Registrar gasto

Consultar sobres/cuentas/periodos, POST H`/movements/expenses/preview` opcional y POST H`/movements/expenses`. Captura targetEnvelopeId/periodId/amount>0/description opcional y financiación si corresponde. Sin fundingSources, todo sale del principal. Acciones: preview, resolver faltante y confirmar. Carga: saldo actual, cálculo de preview y envío diferenciados; vacío: sin sobres/periodo/fondos. Manejar insuficiencia 400 y referencias 404; 409 exige revisar datos o recuperar reintento. Éxito: un gasto con efectos auditados; refrescar saldos.

### Resolver saldo insuficiente

Se abre desde preview: requestedAmount/targetBalance/shortfall/eligibleDonors. Captura contribución del principal y uno o varios donantes. Todos deben pertenecer a la misma cuenta; cada monto >0 y no mayor al saldo mostrado; suma exacta del gasto. La lista de donantes puede incluir sobres menores al faltante; combinar es válido. Acciones: cambiar aportaciones, volver a preview, confirmar. Carga: no reutilizar preview como reserva; vacío: sin donantes, ofrecer cancelar, ingresar dinero o hacer transferencia real antes de volver a intentar. 400 al confirmar: refrescar y editar; 409/503 inciertos: recuperar con misma clave antes de crear otra operación. Éxito: un único gasto, no registrar reasignaciones extra para sus fuentes.

### Transferir entre sobres

Consultar sobres/cuentas/periodos. Misma cuenta: POST H`/movements/reallocations`. Distintas cuentas: POST H`/movements/envelope-transfers`; corresponde a un traslado real del dinero. Captura periodId/fromEnvelopeId/toEnvelopeId/amount/description opcional. Origen != destino, cantidad positiva y reserva suficiente. Acciones: escoger ruta según cuentas, confirmar/cancelar. Carga: catálogos/envío; vacío: se necesitan dos sobres adecuados. 400: cuentas incompatibles o insuficiencia; 409: periodo/carrera. Éxito: reservas compensadas; sólo envelope-transfer modifica saldos reales de ambas cuentas.

### Transferir entre cuentas

GET cuentas/dashboard/periodos; POST H`/movements/transfers`. Captura periodId/fromAccountId/toAccountId/amount/description opcional. Cuentas distintas, activas del hogar y fondos **libres** suficientes; no tomar reservas. Acciones: confirmar o usar transferencia de sobres si se traslada reserva. Carga: mostrar libre del origen y envío; vacío: falta segunda cuenta/fondos. 400/409: revisar causa y actualizar; 503/red: repetir misma clave. Éxito: TRANSFER, salida y entrada iguales; no se muestra como gasto.

### Historial de movimientos

GET H`/movements` con filtros opcionales periodId/accountId/envelopeId y page/size; GET H`/movements/{id}`. Ver efectos, funding, actor, occurredAt y estado. Captura filtros, página y motivo de reversión con un periodo OPEN. Acción POST H`/movements/{id}/reverse`, sólo editor/owner, original POSTED y no REVERSAL. Carga: lista/detalle separados; vacío: sin resultados para filtros. Paginación sin total: una página de tamaño completo puede tener continuación; vacía o más corta termina. 400: reversión deja insuficiencia; 409: ya revertido/periodo/carrera. Éxito: aparece REVERSAL y original REVERSED; refrescar ambos y saldos.

### Configuración familiar

GET `/api/v1/me`, GET `/api/v1/households`, GET H, GET/POST H`/members`, GET/POST H`/income-sources`. Captura hogar nuevo, username/displayName/role para agregar miembro y catálogo de fuente. Sólo OWNER agrega miembros; role permitido EDITOR/VIEWER. Acciones disponibles: crear hogar, agregar miembro/fuente y seleccionar hogar. No hay edición de roles, baja de miembros ni administración de credenciales. Carga: identidad/hogar/listas; vacío: configurar hogar inicial. 400: intentar OWNER; 409: miembro ya existente; 403: permisos. Éxito: recargar catálogo y permisos mostrados.

## Secuencias completas frontend → backend

**Nómina:** GET cuentas/fuentes/periodos → elegir OPEN y cuenta receptora → POST incomes con `examples/requests/IncomeRequest.json` y clave nueva → aplicar la respuesta como auditoría y recargar cuentas/dashboard/movimientos. Aumenta dinero real; no aparta nada.

**Abrir quincena:** GET plantilla y periodos → comprobar rango → POST budget-periods con `{"startDate":"2026-11-15","endDate":"2026-11-30","open":true}` → guardar id devuelto. No hay endpoint de apertura separado; el snapshot no cambia balances.

**Fondear sobres:** registrar ingreso o saldo inicial → transferir dinero real si debe cambiar de cuenta → GET dashboard para free → POST allocations por cada sobre con su propia clave. Cada asignación es transacción independiente; no existe lote atómico de fondeo de toda una quincena. Ejemplo `{"periodId":"period-q1","envelopeId":"envelope-central","amount":80.00,"description":"Apartado Central"}`.

**Compra sencilla:** con Central >=50, POST expenses con `{"periodId":"period-q1","targetEnvelopeId":"envelope-central","amount":50.00,"description":"Compra"}` y clave nueva. fundingSources omitido usa el principal; cuenta y sobre bajan 50, no dos veces.

**Mover reservas:** con Sam's y Central en la misma cuenta, POST reallocations con `{"periodId":"period-q1","fromEnvelopeId":"envelope-sams","toEnvelopeId":"envelope-central","amount":70.00,"description":"Reasignación"}`. Ambas reservas cambian; cuenta no cambia. Para cuentas distintas utilizar envelope-transfers y ejecutar el traslado real.

**Remanentes acumulados:** cerrar periodo si corresponde → crear siguiente OPEN desde plantilla → GET sobres muestra los mismos saldos reservados, menos gastos/liberaciones y más asignaciones realizadas. No calcular remanente restando gastos sólo de la quincena actual; los saldos son acumulados.

**Dinero realmente libre:** GET dashboard → usar free por cuenta para apartar o transferir dinero libre. No calcularlo con expectedIncome, suma del plan ni un saldo cacheado sin actualizar.

## Gasto inteligente: ejemplo completo

Precondición real: cuenta física 1080.00, Central 80.00 y Sam's 1000.00, ambos sobres en esa cuenta. Fuente del saldo principal: GET sobres, no un endpoint individual inventado.

1. POST H`/movements/expenses/preview`, sin clave:

```json
{"periodId":"period-q1","targetEnvelopeId":"envelope-central","amount":150.00,"description":"Carne"}
```

Respuesta completa de preview:

```json
{"requestedAmount":150.00,"targetBalance":80.00,"shortfall":70.00,"eligibleDonors":[{"envelopeId":"envelope-sams","name":"Sam's","balance":1000.00}]}
```

2. Seleccionar Sam's por 70.00, conservar aportación Central por 80.00. La suma de **todas** las fuentes es 150.00, no sólo el faltante. Preview ignora la financiación propuesta y no verifica periodo OPEN; la confirmación sí valida todo.
3. Crear y conservar una clave, por ejemplo `purchase-carne-001`. POST H`/movements/expenses` con esa cabecera:

```json
{"periodId":"period-q1","targetEnvelopeId":"envelope-central","amount":150.00,"description":"Carne","fundingSources":[{"envelopeId":"envelope-central","amount":80.00},{"envelopeId":"envelope-sams","amount":70.00}]}
```

4. Respuesta completa ilustrativa en `examples/smart-expense-response.json`: type EXPENSE, amount 150, funding con ambas aportaciones, cuenta 1080→930, Central 80→0 y Sam's 1000→930. Los IDs/fecha/actor son ilustrativos; los campos corresponden exactamente a MovementResponse. No contiene un dashboard ni entidades completas actualizadas: los saldos están en accountEffects/envelopeEffects, y conviene recargar vistas.
5. Dos editores pueden cambiar fondos después del preview. Concurrencia puede devolver 409; insuficiencia validada devuelve 400. El reintento idéntico conserva clave y cuerpo; cambiar selección requiere resolver primero si la operación original se confirmó y después una nueva operación lógica con otra clave.
6. No tomar fondos directamente de sobres de otra cuenta. Si hace falta, registrar una transferencia real de reserva y volver a consultar/preview. Esa transferencia y el gasto son operaciones separadas, no una única transacción HTTP.
7. Revertir: POST H`/movements/{movementId}/reverse` con otra clave y `{"periodId":"period-q1","reason":"Captura equivocada"}`. Se recuperan los efectos inversos si los saldos actuales lo permiten; se guarda un REVERSAL y original REVERSED. No se puede revertir dos veces ni revertir una reversión.

## Tabla de efectos financieros implementados

| type | Dinero real Account | Reserva Envelope |
|---|---|---|
| OPENING_BALANCE | +importe a cuenta vacía una única vez | Sin cambio |
| INCOME | +importe | Sin cambio |
| ALLOCATE | Sin cambio; free disminuye | +importe |
| RELEASE | Sin cambio; free aumenta | -importe |
| REALLOCATE | Sin cambio | -origen, +destino en misma cuenta |
| TRANSFER | -origen, +destino; sólo libre | Sin cambio |
| ENVELOPE_TRANSFER | -cuenta origen, +cuenta destino | -sobre origen, +sobre destino |
| EXPENSE | -importe de la cuenta del principal una vez | -aportaciones cuya suma es importe |
| REVERSAL | Inverso de efectos del original | Inverso de efectos del original |

No hay endpoint ni tipo de ajuste arbitrario. La atomicidad de cada operación y su idempotencia están implementadas; las pruebas reales MongoDB siguen pendientes de Docker en este entorno. Ver `FRONTEND_INTEGRATION.md` para transporte, dinero y recuperación de errores.

## Planear un sobre en una quincena abierta

Disponible `PUT /api/v1/households/{householdId}/budget-periods/{periodId}/lines/{envelopeId}` con `{ "amount": 1500.00 }`. Inserta o actualiza lo planeado sin mover dinero ni cambiar la plantilla. Contrato, errores y recuperacion parcial al crear un sobre: [OPEN_PERIOD_ENVELOPE_PLAN.md](OPEN_PERIOD_ENVELOPE_PLAN.md).

## Gastos directos y correccion de movimientos

Disponibles POST H`/movements/account-expenses` y POST H`/movements/{id}/corrections`, ambos con Idempotency-Key, y sus variantes `/preview` sin escrituras. La correccion valida saldos netos y conserva original, compensacion y reemplazo. Contratos, variantes y recuperacion: [ACCOUNT_EXPENSES_AND_CORRECTIONS.md](ACCOUNT_EXPENSES_AND_CORRECTIONS.md).

## Presupuesto habitual desde cada sobre

POST H`/envelopes` admite `habitualAmount` opcional y `Idempotency-Key` opcional para alta atomica con plan. GET sobres incluye el habitual actual derivado de la plantilla. PUT H`/budget-template/lines/{envelopeId}` con `{amount}` actualiza un solo plan y crea la plantilla si falta, sin quincena ni movimientos. [Contrato y recuperacion](HABITUAL_ENVELOPE_BUDGET.md).

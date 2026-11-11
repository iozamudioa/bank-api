# Contrato REST BANK v1

Autenticación HTTP Basic en todas las rutas salvo `/actuator/health`. Los recursos de un hogar requieren membresía activa; escrituras requieren OWNER o EDITOR. Agregar miembros requiere OWNER. Datos monetarios JSON son números decimales, no flotantes de Java.

Prefijo H = `/api/v1/households/{householdId}`. Las rutas y respuestas de éxito se conservan (200); esta revisión añade una cabecera obligatoria a operaciones financieras y parámetros de consulta opcionales.

| Método | Ruta | Entrada / salida |
|---|---|---|
| GET | `/api/v1/me` | `{username}` |
| POST / GET | `/api/v1/households` | Crear `{name,displayName}` / listar hogares |
| GET | H | Hogar |
| POST / GET | H`/members` | `{username,displayName,role}` / membresías |
| POST / GET | H`/accounts` | `{name,type}` / cuentas |
| POST / GET | H`/envelopes` | `{name,accountId,type,category,habitualAmount?}` / sobres con habitualAmount derivado de plantilla |
| POST / GET | H`/income-sources` | `{name,type,accountId,expectedAmount}` / fuentes |
| PUT | H`/budget-template/lines/{envelopeId}` | `{amount}` / plantilla actualizada; sin quincena |
| PUT / GET | H`/budget-template` | `{expectedIncome,allocations:[{envelopeId,amount}]}` / plantilla |
| POST / GET | H`/budget-periods` | `{startDate,endDate,open}` / periodos |
| GET | H`/budget-periods/{periodId}` | Snapshot histórico |
| POST | H`/budget-periods/{periodId}/close` | Periodo cerrado, sin modificar remanentes |
| PUT | H`/budget-periods/{periodId}/lines/{envelopeId}` | `{amount}` / periodo OPEN actualizado, sin mover dinero |
| GET | H`/dashboard` | Totales y saldos por cuenta |
| GET | H`/movements` | Lista filtrada y paginada |
| GET | H`/movements/{id}` | Movimiento |
| POST | H`/movements/opening-balances` | `{accountId,amount,description,periodId?}` |
| POST | H`/movements/incomes` | `{periodId,accountId,incomeSourceId?,amount,description}` |
| POST | H`/movements/allocations` | `{periodId,envelopeId,amount,description}` |
| POST | H`/movements/releases` | Misma entrada que allocation |
| POST | H`/movements/reallocations` | `{periodId,fromEnvelopeId,toEnvelopeId,amount,description}` |
| POST | H`/movements/transfers` | `{periodId,fromAccountId,toAccountId,amount,description}` |
| POST | H`/movements/envelope-transfers` | Misma entrada que reallocation; distintas cuentas |
| POST | H`/movements/expenses/preview` | Entrada de gasto; respuesta de solo lectura |
| POST | H`/movements/expenses` | Gasto con fundingSources opcional |
| POST | H`/movements/account-expenses` | `{periodId,accountId,amount,description}`; solo libre |
| POST | H`/movements/account-expenses/preview` | Misma entrada; total/libre antes y despues |
| POST | H`/movements/{id}/corrections` | `{periodId,reason,replacement}`; reemplazo con enlaces y efectos netos |
| POST | H`/movements/{id}/corrections/preview` | Misma entrada; efectos netos sin escrituras |
| POST | H`/movements/{id}/reverse` | `{periodId,reason}` |

Todos los POST bajo movements salvo los endpoints de preview exigen `Idempotency-Key`. No reutilizar una clave para una operación nueva. Reintentos tras 409/503 conservan la clave y los datos. Una petición con contenido diferente necesita una clave nueva. Se guarda y devuelve la respuesta original; una repetición posterior no vuelve a cambiar saldos.

## Gasto inteligente

Con una cuenta con 1080.00, Central con 80.00 y Sam's con 1000.00:

```http
POST /api/v1/households/H/movements/expenses
Authorization: Basic <credenciales>
Idempotency-Key: compra-carne-001
Content-Type: application/json

{"periodId":"Q","targetEnvelopeId":"CENTRAL","amount":150.00,"description":"Carne","fundingSources":[{"envelopeId":"CENTRAL","amount":80.00},{"envelopeId":"SAMS","amount":70.00}]}
```

Fragmento de respuesta (también contiene identificadores, estado, actor, fecha y metadatos del movimiento):

```json
{
  "type":"EXPENSE",
  "amount":150.00,
  "targetEnvelopeId":"CENTRAL",
  "funding":[{"envelopeId":"CENTRAL","amount":80.00},{"envelopeId":"SAMS","amount":70.00}],
  "accountEffects":[{"entityId":"CASH","before":1080.00,"change":-150.00,"after":930.00}],
  "envelopeEffects":[{"entityId":"CENTRAL","before":80.00,"change":-80.00,"after":0.00},{"entityId":"SAMS","before":1000.00,"change":-70.00,"after":930.00}]
}
```

Preview usa la misma ruta con sufijo `/preview` y la misma entrada, sin cabecera de idempotencia. No guarda nada. Responde `{requestedAmount,targetBalance,shortfall,eligibleDonors:[{envelopeId,name,balance}]}`. Incluye todos los sobres activos con saldo positivo de la misma cuenta cuando existe faltante, para permitir combinar donantes pequeños. Confirmar siempre vuelve a validar los saldos y la quincena abierta.

## Consultas y errores

`GET H/movements?page=0&size=20&periodId=Q&accountId=CASH&envelopeId=CENTRAL`. Filtros opcionales combinados con AND. page empieza en 0; size entre 1 y 100, predeterminado 100. Respuesta sigue siendo una lista, sin envoltorio ni total. Orden por fecha y después ID descendente.

400: JSON/DTO/importe/filtros inválidos o cabecera ausente. 401: credenciales ausentes o inválidas. 403: sin permisos de hogar. 404: recurso ausente o de otro hogar. 409: operación incompatible, clave reutilizada con otro command, versión obsoleta o conflicto concurrente. 503: indisponibilidad de persistencia. Los errores del handler mantienen `{error,message}`. No se devuelven mensajes internos del driver.

No hay endpoint de ajuste arbitrario de saldo. Se usan ingresos, liberaciones, transferencias y reversiones auditadas. Una quincena guarda planificación histórica; no vuelve a fondear ni reinicia sobres automáticamente.

Gastos directos y correcciones: [contrato detallado y ejemplos](ACCOUNT_EXPENSES_AND_CORRECTIONS.md). Todos los previews financieros estan exentos de Idempotency-Key.

Alta de sobre y plan habitual atomicos, cabecera opcional y semantica de null/cero: [presupuesto habitual desde sobres](HABITUAL_ENVELOPE_BUDGET.md).

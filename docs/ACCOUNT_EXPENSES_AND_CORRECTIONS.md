# Gastos de cuenta y correcciones atomicas

Implementado en BANK API v1 (2026-11-11). Prefijo H = /api/v1/households/{householdId}.

| Metodo | Ruta | Respuesta 200 |
|---|---|---|
| POST | H/movements/account-expenses/preview | AccountExpensePreviewResponse |
| POST | H/movements/account-expenses | MovementResponse, EXPENSE |
| POST | H/movements/{id}/corrections/preview | CorrectionPreviewResponse, efectos netos |
| POST | H/movements/{id}/corrections | MovementResponse del reemplazo y correction |

Todas requieren HTTP Basic, membresia activa OWNER/EDITOR, periodo OPEN y recursos activos del mismo hogar. Las dos confirmaciones requieren Idempotency-Key (1 a 128 caracteres); previews no requieren clave ni escriben datos. Se conserva el endpoint de gasto por sobres y la reversion existentes.

## Gasto desde saldo libre

```http
POST /api/v1/households/H/movements/account-expenses
Idempotency-Key: gasto-directo-001
Content-Type: application/json

{"periodId":"Q","accountId":"CUENTA","amount":150.00,"description":"Gasto directo"}
```

Mismo cuerpo para /account-expenses/preview. El total real disminuye 150 y las reservas permanecen intactas. Libre = saldo real de cuenta menos suma de reservas de sus sobres. Si el libre es insuficiente devuelve 400; nunca libera reservas implicitamente. Positivo, maximo dos decimales y precision Money/Decimal128; sin redondeos.

Preview devuelve accountId, amount, totalBefore, freeBefore, totalAfter, freeAfter. Es una vista actual, no una reserva ni una garantia; confirmar revalida. La respuesta confirmada usa type=EXPENSE, targetEnvelopeId=null, funding=[], envelopeEffects=[] y un accountEffect negativo. Identificar la cuenta por accountEffects.entityId. GET movements?accountId=... lo incluye; el filtro envelopeId no atribuye el gasto directo a un sobre. Se puede revertir con el endpoint existente o corregir.

## Correccion

```http
POST /api/v1/households/H/movements/ORIGINAL/corrections
Idempotency-Key: correccion-001
Content-Type: application/json

{
  "periodId":"Q",
  "reason":"Correccion de captura",
  "replacement":{
    "type":"INCOME",
    "accountId":"CUENTA",
    "amount":1200.00,
    "description":"Ingreso corregido"
  }
}
```

Mismo cuerpo para /corrections/preview. replacement es un reemplazo COMPLETO, no un parche. Omitir la fuente de ingreso la elimina; omitir financiacion en gasto por sobre usa el sobre principal por el importe completo. periodId exterior determina la nueva quincena de compensacion y reemplazo.

| replacement.type | Campos propios |
|---|---|
| INCOME | accountId, incomeSourceId opcional |
| ALLOCATE | envelopeId |
| RELEASE | envelopeId |
| REALLOCATE | fromEnvelopeId, toEnvelopeId; misma cuenta |
| TRANSFER | fromAccountId, toAccountId; cuentas distintas |
| ENVELOPE_TRANSFER | fromEnvelopeId, toEnvelopeId; cuentas distintas |
| EXPENSE | targetEnvelopeId, fundingSources opcional [{envelopeId,amount}], misma cuenta |
| ACCOUNT_EXPENSE | accountId, solo libre |

Los reemplazos admiten amount no negativo y description opcional; las altas y las aportaciones no vacias siguen exigiendo importe positivo. ACCOUNT_EXPENSE es el discriminador REST del reemplazo de gasto directo; el movimiento persistido sigue siendo EXPENSE. Se permite cambiar un gasto entre cuenta y sobre conservando MovementType.EXPENSE. Los demas tipos deben coincidir con el original. No se corrigen OPENING_BALANCE, REVERSAL ni movimientos REVERSED. Un reemplazo POSTED puede corregirse de nuevo y conserva la cadena de enlaces.

La operacion calcula compensacion + reemplazo frente a los saldos actuales y valida el resultado FINAL: cuentas y sobres no negativos, reservas <= saldo real. No exige saldo intermedio para deshacer antes de rehacer. Ejemplo: ingreso de 100 ya gastado, corregido a 200, incrementa libre y real en 100. Reducirlo a 50 falla si dejaria reservas sin respaldo.

Transaccion unica: balances finales, compensacion REVERSAL, original marcado REVERSED, nuevo movimiento y registro de idempotencia. Si falla cualquier escritura o validacion, todo se revierte. El original conserva importe, descripcion, fecha, quincena y efectos historicos; solo cambia su estado. Puede pertenecer a una quincena cerrada: compensacion/reemplazo se registran en la nueva OPEN sin reabrir ni reescribir el plan historico. Las correcciones no editan fechas originales ni el presupuesto.

### Respuesta y auditoria

El reemplazo devuelve correction.originalId, correction.reversalId, correction.reason, correction.netAccountEffects y correction.netEnvelopeEffects. Estas dos listas son los efectos NETOS confirmados con before/change/after. El preview entrega accountEffects/envelopeEffects netos con el mismo significado.

Los accountEffects/envelopeEffects principales del reemplazo describen exclusivamente los efectos del nuevo movimiento, para permitir su reversion posterior. Los de la compensacion describen la inversa del original. Sus saldos before/after pueden ser virtuales, incluso negativos: representan la descomposicion contable de una unica transaccion, no estados intermedios guardados en cuentas/sobres. Solo los saldos finales validados se persisten. El original nunca se reemplaza por el registro nuevo ni se borra.

Para consultar la compensacion usar GET H/movements/{correction.reversalId}; su reversalOf apunta al original. La respuesta del reemplazo contiene ambos enlaces incluso en un replay de idempotencia. El frontend debe refrescar saldos tras confirmar y no sumar tanto efectos netos como efectos del reemplazo. Tampoco debe sumar historiales con originales REVERSED como si siguieran vigentes.

### Conflictos y recuperacion

400: datos/importes invalidos, tipos incompatibles o saldo neto insuficiente. 401: sin autenticar. 403: sin permisos. 404: original/recurso del hogar no disponible. 409: periodo cerrado, original ya revertido/no corregible, clave con otros datos o conflicto concurrente. 503: persistencia no disponible/resultado incierto. 500: error inesperado.

Confirmacion revalida recursos y periodo bajo transaccion snapshot/majority; usa la revision de periodo compartida con cierre, versiones optimistas en cuentas/sobres y la restriccion unica de compensacion por original. Dos correcciones/reversiones del mismo original no pueden compensarlo dos veces. Un conflicto puede exigir reintento con la MISMA clave y cuerpo; nunca lanzar una reversion separada desde el frontend para intentar reparar un resultado incierto. Misma clave y command devuelve la respuesta original sin aplicar efectos otra vez. Cambiar original, datos u operacion con esa clave produce 409.

## Integracion de interfaz

1. Regenerar el cliente desde docs/openapi/bank-api-v1.yaml. El discriminador type de replacement tiene ocho variantes documentadas.
2. Registrar gasto: elegir cuenta o sobre, mostrar libre o reserva, consultar el preview correspondiente y confirmar con clave estable.
3. Actividad: ofrecer Editar a POSTED distintos de OPENING_BALANCE y REVERSAL. Precargar tipo, recursos e importe; capturar motivo y quincena OPEN.
4. Revisar efectos netos con el preview y confirmar mediante un solo POST /corrections. No encadenar reverse y otra alta.
5. Tras confirmar refrescar datos. Mantener clave/cuerpo si el resultado es incierto. En errores mantener el formulario y mostrar la causa.

Esta entrega implementa API, contrato y pruebas; la interfaz debe consumir estas capacidades.

## Verificacion

Pruebas de servicio/HTTP: saldo libre/reservas, previews sin escrituras, correccion de todos los tipos, ingreso usado, cambio de cuenta y financiacion, cadena de correcciones, reversion del reemplazo, permisos, periodo cerrado, validaciones y replay estable. Pruebas MongoDB a?adidas: filtros, BSON Decimal128, rollback incluso al fallar el reemplazo despues de guardar compensacion y estado original, concurrencia con reversion y gasto libre concurrente. Requieren Docker; si no esta disponible se omiten explicitamente.

Resultado local final: `mvn clean package` con Java 17: 71 pruebas detectadas, 53 aprobadas, 18 omitidas sin Docker. JAR generado en target/bank-api-0.1.0-SNAPSHOT.jar. Contrato comparado por HTTP con el YAML exportado: 36 operaciones. Cliente TypeScript generado correctamente con openapi-typescript 7.13.0. Las pruebas omitidas incluyen las seis pruebas MongoDB nuevas; no se afirma validacion transaccional real en este equipo.

Correcciones por importe cero y recursos explicitos para segunda edicion: [ZERO_AMOUNT_CORRECTIONS.md](ZERO_AMOUNT_CORRECTIONS.md).

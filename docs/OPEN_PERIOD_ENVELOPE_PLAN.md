# Planear sobres en una quincena abierta

Disponible en BANK API v1:

```http
PUT /api/v1/households/{householdId}/budget-periods/{periodId}/lines/{envelopeId}
Authorization: Basic <credenciales>
Content-Type: application/json

{"amount": 1500.00}
```

Devuelve HTTP 200 con BudgetPeriodResponse completo. Inserta la linea si falta o reemplaza su importe, conservando las otras lineas, fechas, estado e ingreso esperado. No requiere Idempotency-Key; repetir el PUT no duplica lineas.

Requiere OWNER o EDITOR con membresia activa, periodo OPEN del hogar, sobre activo y cuenta activa del mismo hogar. Cero es valido; negativos, mas de dos decimales y cantidades fuera de Decimal128 se rechazan. No cambia saldos, reservas, movimientos, plantilla ni otros periodos. Guardar la plantilla sigue afectando exclusivamente periodos futuros.

Errores: 400 importe o referencia no disponibles; 401 sin autenticacion; 403 sin permiso; 404 recurso no encontrado; 409 periodo DRAFT/CLOSED o conflicto concurrente; 503 persistencia no disponible. La escritura comparte transaccion y bloqueo del periodo con cierre y movimientos. Ante conflicto consultar el periodo; reintentar el mismo PUT solo si sigue OPEN. Si dos usuarios editan la misma linea secuencialmente, prevalece la ultima escritura aceptada.

## Integracion de Crear sobre

1. Ofrecer un importe opcional y elegir una quincena OPEN del hogar, independientemente del periodo historico seleccionado en el dashboard. Si hay varias abiertas, permitir elegir; sin ninguna, no ofrecer este campo.
2. Crear el sobre con POST /envelopes como hasta ahora y conservar el id recibido.
3. Si se capturo un importe (incluido cero), enviar este PUT con ese id y el periodo elegido.
4. Refrescar sobres y periodos despues del exito. Apartar dinero podra obtener el importe de las lineas del periodo.
5. Si el POST funciona y el PUT falla, informar que el sobre ya fue creado y conservar su id para reintentar solo el PUT. No repetir POST ni crear duplicados. Si el periodo se cerro, conservar el sobre y mostrar el motivo.

La API queda implementada; el campo y este flujo deben integrarse en el frontend. No hay una transaccion HTTP conjunta entre crear el sobre y guardar su plan.

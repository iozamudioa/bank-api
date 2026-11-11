# Correcciones con importe cero

Implementado el 2026-11-11 en API y frontend.

## Contrato

Los ocho ReplacementRequest admiten amount >= 0 con maximo dos decimales. Solo las correcciones permiten cero: altas de ingresos, gastos, apartados, liberaciones y transferencias siguen requiriendo importe positivo. Las aportaciones no vacias siguen siendo positivas. Money.positive no se modifico.

applyReplacement normaliza y valida el importe no negativo. La compensacion y el reemplazo por cero forman una transaccion unica, conservan permisos de escritura y periodo OPEN, y validan saldos finales. No se requiere dinero para un paso intermedio de deshacer. Se mantienen original REVERSED, compensacion REVERSAL, reemplazo POSTED por 0.00, enlaces y replay de Idempotency-Key. Un cero no se transforma en una reversion independiente.

## Gastos

EXPENSE por cero acepta fundingSources ausente o [], normalizado a []; rechaza cualquier fila, incluso una aportacion cero. funding y replacement.fundingSources de la respuesta son []. No aplica nuevos debitos; la compensacion devuelve cada aportacion original a su sobre y el importe total a la cuenta. Se conserva el sobre principal explicito.

ACCOUNT_EXPENSE por cero devuelve el gasto original al saldo libre sin cambiar reservas. Su cuenta se conserva explicitamente.

Las correcciones de ingreso, liberacion u otros tipos pueden rechazarse si el resultado final dejaria saldo negativo o reservas sin respaldo porque el dinero original ya se utilizo. Un reemplazo cero sigue siendo un movimiento corregible; puede corregirse otra vez a cero o a un importe positivo.

## Recursos explicitos

MovementResponse agrega replacement, una copia confirmada de los datos del reemplazo. Incluye type (INCOME, ALLOCATE, RELEASE, REALLOCATE, TRANSFER, ENVELOPE_TRANSFER, EXPENSE o ACCOUNT_EXPENSE), amount, description y los identificadores pertinentes: accountId, incomeSourceId, envelopeId, fromAccountId, toAccountId, fromEnvelopeId, toEnvelopeId y targetEnvelopeId. fundingSources contiene las aportaciones confirmadas (vacia para gasto cero).

Estos recursos se persisten con la correccion y su respuesta idempotente. No se calculan desde los signos de efectos ni desde saldos/cuentas actuales. GET movimiento, listado y replay devuelven los mismos datos historicos. En registros originales o correcciones antiguas sin estos recursos, replacement es null; no se inventan identificadores para registros ambiguos.

Ejemplo de reemplazo de transferencia cero:

```json
{
  "type":"TRANSFER",
  "amount":0.00,
  "description":"Captura corregida",
  "fromAccountId":"cuenta-origen",
  "toAccountId":"cuenta-destino"
}
```

Los efectos del movimiento pueden estar vacios cuando todos los cambios del reemplazo son cero. correction.netAccountEffects y correction.netEnvelopeEffects siguen mostrando la compensacion neta confirmada. El preview muestra la devolucion antes de confirmar.

## Frontend

buildReplacement y el formulario de edicion permiten cero. Los seis formularios reutilizados usan correctionMode=true exclusivamente dentro del modal; sus altas conservan validadores positivos. Las aportaciones mantienen validacion positiva.

Al elegir importe cero en un gasto, el modal oculta las aportaciones nuevas y envia fundingSources=[] aunque conserve filas antiguas en la edicion local. El preview presenta los efectos netos. Al abrir otro reemplazo por cero se utilizan los recursos explicitos de replacement; la reconstruccion por signos queda como compatibilidad para movimientos antiguos que todavia tienen efectos suficientes. No consulta saldos para inventar origen o destino.

La confirmacion conserva clave y cuerpo durante errores o reintentos inciertos, incluso si el formulario cambia despues.

## Verificacion

Backend: compilacion limpia y JAR correctos con Java 17; 96 pruebas detectadas, 68 aprobadas y 28 de MongoDB omitidas por falta de Docker. Se cubren cero en los ocho tipos, financiacion multiple devuelta, reservas intactas en gasto directo, recursos y fuente de ingreso, segunda edicion, permisos/reglas existentes, reducciones inviables, REST, minima OpenAPI y replay. Las nuevas pruebas reales de rollback y persistencia estan a?adidas a la suite MongoDB y requieren Docker.

Frontend: 41 archivos y 261 pruebas aprobadas; compilacion de produccion correcta. Incluye cero por tipo, reapertura desde recursos explicitos, validadores de altas/correccion, financiacion vacia, devolucion neta y reintento estable. OpenAPI y cliente generado estan sincronizados.

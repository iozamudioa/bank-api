# Ejemplos BANK v1

Los IDs, actor y fecha de las respuestas son ilustrativos. Sustituir referencias por IDs obtenidos de la API; los archivos no son seeds ni se ejecutan automáticamente. La documentación no deposita dinero en una base real.

- `onboarding.http`: secuencia de catálogos y gasto inteligente, compatible con REST Client de VS Code. Copiar los IDs devueltos a las variables; ejecutar sólo después de revisar cada operación.
- `requests/CreateHouseholdRequest.json`: POST `/api/v1/households`.
- `requests/CreateAccountRequest.json`: POST H`/accounts`; repetir con DIGITAL si corresponde.
- `requests/CreateEnvelopeRequest.json`: POST H`/envelopes`.
- `requests/CreateIncomeSourceRequest.json`: POST H`/income-sources`; no deposita dinero.
- `requests/SaveBudgetRequest.json`: PUT H`/budget-template`, antes de crear periodo.
- `requests/CreatePeriodRequest.json`: POST H`/budget-periods`, open:true para uso inmediato.
- `requests/OpeningBalanceRequest.json`: POST H`/movements/opening-balances`, sólo si la cuenta ya tenía ese dinero real y nunca se registró antes.
- `requests/IncomeRequest.json`: POST H`/movements/incomes`, ingreso real nuevo con quincena abierta.
- `requests/AllocationRequest.json`: POST H`/movements/allocations`; también forma de `/releases`, ajustando amount/description.
- `requests/ReallocationRequest.json`: POST H`/movements/reallocations`, misma cuenta.
- `requests/TransferRequest.json`: POST H`/movements/transfers`, dinero libre entre cuentas distintas.
- `requests/EnvelopeTransferRequest.json`: POST H`/movements/envelope-transfers`, reserva entre cuentas distintas.
- `expense-preview-request.json` → `expense-preview-response.json`: POST H`/movements/expenses/preview`, sin clave ni escritura.
- `requests/ExpenseRequest.json` → `smart-expense-response.json`: POST H`/movements/expenses`, gasto único de 150 desde Central 80 y Sam's 70.
- `requests/ReverseRequest.json`: POST H`/movements/{id}/reverse`, con clave nueva.
- `requests/AddMemberRequest.json`: POST H`/members`, OWNER.
- `requests/FundingRequest.json` y `BudgetLineRequest.json` son valores **embebidos**, no endpoints independientes.

H significa `/api/v1/households/{householdId}`. Todos los POST financieros salvo preview requieren Idempotency-Key; cada operación nueva necesita otra clave. Repetición idéntica conserva la clave. Las peticiones monetarias son números JSON (80 y 80.00 son ejemplos válidos); la PWA debe conservar los tokens decimales de una petición al repetirla.

OpenApiContractTest deserializa estos requests mediante los DTOs reales, verifica Jakarta Validation y compara los ejemplos con los schemas generados. Los ejemplos de respuesta no prueban efectos de una transacción real; para ello existe MongoFinanceIntegrationTest con Docker.

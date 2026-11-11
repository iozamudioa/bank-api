# Continuidad de BANK API v1

Actúa como Senior Java Backend Engineer. Lee completamente README.md, docs/ARCHITECTURE.md, docs/REFACTOR_REPORT.md, docs/ENDPOINTS.md, pom.xml, application.yml y el código antes de editar. Mantén dev.ioza:bank-api, dev.ioza.bank, Java 17 y Spring Boot/MongoDB. Refactoriza el proyecto existente.

## Reglas

- Un tipo de nivel superior por archivo Java; imports explícitos y limpios. Clases internas sólo para estado técnico o fixtures.
- Dominio sin Spring, MongoDB ni HTTP. DTOs request/response separados de commands y documentos. Mantén puertos y mappers.
- Todos los adaptadores en infrastructure/adapters; configuración técnica en infrastructure/config y seguridad en infrastructure/security. Domain y application no dependen de infrastructure. El advice HTTP pertenece a infrastructure/adapters/in/controllers/advice.
- BigDecimal con escala 2 y precisión Decimal128, sin double/float ni enteros de centavos. Rechaza más de dos decimales; no redondees silenciosamente.
- Account es dinero real, Envelope una reserva. No hay fondeo automático desde el presupuesto. Mantén remanentes históricos.
- Las escrituras de cuentas, sobres, historial e idempotencia comparten una transacción. Conserva @Version y las revisiones que serializan cierre y planificación.
- Gasto inteligente: 1080/80/1000 pasa a 930/0/930 al pagar 150 con 80 de Central y 70 de Sam's. Preview no escribe; confirmar revalida todo.
- POST financieros requieren Idempotency-Key. Repetición devuelve respuesta original; contenido u operación distintos con misma clave devuelven 409. En carreras, reintentar misma clave.
- No cambies rutas o campos JSON sin justificarlo. GET movements conserva lista JSON y filtros/paginación en base de datos.
- No declares atomicidad validada si Testcontainers omitió pruebas.

## Primera verificación pendiente

En un equipo con Docker Engine ejecuta scripts/verify-mongo.ps1 y luego mvn clean test y mvn clean package con Java 17. Comprueba los casos MongoFinanceIntegrationTest, incluidos fallos al guardar historial, concurrencia, versiones, replay y Decimal128 anidado. Verifica además Compose y el arranque HTTP contra rs0. Revisa datos heredados e índices en una copia antes de actualizar una base real.

Consulta docs/CODEX_NEXT.md para el trabajo restante.

## Documentación y contrato frontend

Lee docs/FRONTEND_HANDOFF.md, docs/FRONTEND_INTEGRATION.md y docs/OPENAPI.md al modificar contratos REST. Las anotaciones Swagger pertenecen a infraestructura, nunca al dominio. Actualiza ejemplos y regenera docs/openapi/bank-api-v1.yaml con scripts/export-openapi.ps1; no edites el YAML a mano. OpenApiContractTest verifica rutas reales, seguridad, schemas, ejemplos y equivalencia del snapshot. No introducir endpoints inexistentes ni cambiar números monetarios a strings sin una migración explícita del contrato.

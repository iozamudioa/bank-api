# Entrega: documentación, OpenAPI y contrato PWA

Fecha de verificación: 2026-11-10. Java 17.0.18, Spring Boot 4.1.1, Springdoc 3.2.0. No se implementó frontend ni se modificaron reglas financieras, rutas de negocio o formato de importes.

## Implementación

- Se inspeccionaron README, CODEX_PROMPT, seis controllers, 17 requests, 17 responses, commands, puertos/casos de uso, servicios, validaciones, excepciones y seguridad.
- OpenApiConfig bajo infrastructure/config declara BANK API, versión 1.0.0, servidor relativo y HTTP Basic. Springdoc sólo incluye las rutas reales /api/v1/**.
- 31 operaciones documentadas con operationId únicos/estables, etiquetas funcionales, explicaciones, respuestas HTTP, ejemplos y parámetros. DTOs anotados con Schema, obligatoriedad, nulabilidad y formato monetario number/decimal. No se anotó domain ni application con Swagger.
- Swagger UI, JSON y YAML accesibles; operaciones REST mantienen autenticación y autorización por hogar.
- Exportación reproducible desde la respuesta HTTP real; el YAML no es una especificación paralela escrita a mano. La suite detecta divergencias con el snapshot.
- Guía funcional con las 14 pantallas solicitadas, estados de carga/vacíos/error, acciones y flujos; gasto inteligente completo y sus limitaciones.
- Guía de integración con Basic, CORS realmente ausente, idempotencia, fechas, listas paginadas, precisión decimal y generación TypeScript.
- Ejemplos de todos los DTOs de entrada, secuencia onboarding.http y respuestas completas de preview/gasto.

## Ajustes mínimos justificados del contrato de errores

Antes, HTTP Basic podía responder sin el DTO JSON del resto de la API, y no había handler general 500. Se añadieron respuestas JSON ErrorResponse para 401/403 de seguridad y un 500 seguro sin detalles internos. Se conserva WWW-Authenticate en 401. El handler genérico respeta los estados de errores HTTP del framework, como 404, en lugar de convertirlos en 500.

No se inventaron tokens, endpoints de usuarios, calendario recurrente, apertura de DRAFT, ajustes arbitrarios ni detalle individual de cuenta/sobre. No se añadió 422: insuficiencia monetaria sigue devolviendo 400 y conflictos siguen devolviendo 409. No se habilitó CORS ni se cambiaron números JSON a strings. Los clientes existentes conservan DTOs de éxito, rutas y lógica financiera; ahora los errores de seguridad/internos tienen JSON consistente.

## Verificación ejecutada

| Comprobación | Resultado |
|---|---|
| Generar YAML: mvn -Dtest=OpenApiContractTest -Dbank.openapi.export=true test | Correcto |
| Suite completa: mvn -Dbank.openapi.export=true clean test | BUILD SUCCESS; 46 casos, 37 aprobados y 9 omitidos |
| mvn clean package, sin exportar | BUILD SUCCESS; misma suite, snapshot consistente, JAR Spring Boot generado |
| Redocly CLI lint, --extends minimal | Descripción OpenAPI válida, sin errores |
| openapi-typescript 7.13.0 | Tipos generados correctamente en target temporal, eliminado por clean |
| Arquitectura | Sin anotaciones Swagger en domain/application; imports explícitos y componentes descubribles |

OpenApiContractTest tiene cuatro pruebas: equivalencia JSON/YAML/snapshot con todas las referencias internas resueltas y cobertura exacta de rutas reales; Swagger UI y autenticación HTTP real; ejemplos de requests contra DTOs/constraints y schemas; errores 500/404 reales sin fuga de detalles. Usa un servidor efímero y la seguridad real, con puertos de negocio que rechazan ejecución financiera y MongoDB excluido sólo dentro de la configuración de prueba.

Los logs de la excepción inyectada para verificar 500 son esperados. Las advertencias de Springdoc indican que la documentación está habilitada; no son errores de arranque. La advertencia CDS del JVM de pruebas no impide ejecución.

Las nueve pruebas MongoFinanceIntegrationTest siguen omitidas porque no hay Docker disponible. **No se validó atomicidad real, construcción Docker ni arranque de producción contra MongoDB.** No se sustituyeron esas pruebas por fixtures de documentación. Las pruebas financieras unitarias y REST existentes continúan pasando.

## Archivos y puntos de entrada

- pom.xml: dependencia Springdoc 3.2.0.
- application.yml: selección de rutas, OpenAPI 3.0 y Swagger UI.
- infrastructure/config/OpenApiConfig.java: metadatos, servidor y Basic.
- infrastructure/security/SecurityConfig.java: acceso a documentación y JSON de seguridad.
- infrastructure/adapters/in/controllers: anotaciones de operaciones/parámetros/respuestas y schemas de DTOs; advice con manejo general seguro.
- src/test/java/dev/ioza/bank/documentation/OpenApiContractTest.java: generación, validación y pruebas HTTP.
- scripts/export-openapi.ps1: generación desde raíz con comprobación de resultado Maven.
- docs/openapi/bank-api-v1.yaml: contrato generado.
- docs/FRONTEND_HANDOFF.md, FRONTEND_INTEGRATION.md, OPENAPI.md, examples/: contrato funcional y técnico para el agente frontend.
- README.md, docs/CODEX_PROMPT.md y FILES_CHANGED.md: navegación y continuidad actualizadas.

## Pendientes explícitos

Validar MongoDB real con Docker; acordar CORS si se separan orígenes; sustituir identidades en memoria por autenticación persistente antes de exposición pública; definir códigos financieros estables si la PWA necesita diferenciación automática de errores. La PWA no debe asumir ninguna de esas funciones como implementada. El YAML generado y los operationIds permiten empezar el frontend con las funciones reales existentes.

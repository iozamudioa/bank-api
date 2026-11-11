# OpenAPI reproducible y Swagger UI

Spring Boot real: 4.1.1, Java 17. Se integra `org.springdoc:springdoc-openapi-starter-webmvc-ui:3.2.0`. La [release oficial](https://github.com/springdoc/springdoc-openapi/releases/tag/v3.2.0) declara la línea Boot 4.1.x (construida sobre 4.1.2); la combinación con 4.1.1 se comprueba aquí por compilación y peticiones HTTP de pruebas, sin actualizar Boot.

Configuración: infrastructure/config/OpenApiConfig.java. Title BANK API, versión del contrato 1.0.0 (diferente de versión del artefacto Maven), servidor relativo `/` y rutas `/api/v1/**`. OpenAPI 3.0.1; no se agregan recursos financieros inexistentes. Etiquetas de operaciones implementadas: Households, Members, Accounts, Income Sources, Envelopes, Budgets, Budget Periods, Movements, Expenses, Balances y Dashboard.

Con la aplicación arrancada y MongoDB replica set disponible:

- `/swagger-ui.html` redirige a Swagger UI; `/swagger-ui/index.html` sirve los recursos.
- `/v3/api-docs` produce JSON generado.
- `/v3/api-docs.yaml` produce YAML generado.
- `/v3/api-docs/swagger-config` configura Swagger UI.

Las rutas de documentación son públicas. En Swagger usar Authorize con HTTP Basic para llamar a `/api/v1/**`; la documentación pública no elimina autorización por hogar. No existe login JWT. Swagger no persiste credenciales.

## Generación y verificación sin MongoDB

Desde la raíz, con JAVA_HOME apuntando a Java 17:

```powershell
mvn '-Dtest=OpenApiContractTest' '-Dbank.openapi.export=true' test
```

Escribe **docs/openapi/bank-api-v1.yaml** desde la respuesta real de `/v3/api-docs.yaml`. No se escribe ni mantiene a mano una especificación paralela. Esta prueba arranca Spring Boot en un puerto HTTP aleatorio con los controllers, DTOs, advice, seguridad y configuración OpenAPI reales. Sólo sustituye puertos de negocio por fixtures que rechazan ejecución y excluye MongoAutoConfiguration dentro de esa configuración de prueba; no existe perfil de producción que permita saltarse MongoDB. Las pruebas de documentación no demuestran atomicidad financiera.

La suite obtiene JSON y YAML por HTTP, compara estructura (preservando fechas como strings y equivalencia numérica), verifica metadatos y Basic, 37 operaciones con operationId únicos y resuelve cada referencia interna. Comprueba Swagger, identidad autenticada y error 401. Compara también con el archivo exportado. El puerto efímero nunca se incluye en la especificación: el servidor es relativo para una exportación estable.

```powershell
# Verificar sin modificar el archivo: falla si hay divergencia
mvn '-Dtest=OpenApiContractTest' test
# Compilación y suite completas
mvn clean test
mvn clean package
```

Si cambia una operación o DTO: editar código/anotaciones → regenerar YAML → revisar diff/ejemplos → ejecutar suite sin exportar. El contrato de negocio se deriva de métodos y anotaciones, no de los fixtures. El snapshot incluye ejemplos ilustrativos, no datos consultados de una base real.

## Descarga desde una instancia real

```powershell
Invoke-WebRequest 'http://localhost:8080/v3/api-docs.yaml' -OutFile docs/openapi/bank-api-v1.yaml
```

Para comparar, ejecutar después OpenApiContractTest sin exportación. No usar una versión de backend distinta ni añadir prefijos al servidor en el archivo a mano. La descarga no necesita credenciales por ser documentación pública. Una configuración de reverse proxy/contexto diferente debe tenerse en cuenta antes de comparar servidores.

## Contratos incompletos e inconsistencias aclaradas

- La API usa números JSON para importes; no strings. Se conserva ese formato y se documenta el tratamiento exacto del frontend.
- Las creaciones siguen respondiendo 200. No se documentan 201/204 ni 422 inexistentes.
- Se unificaron respuestas de errores HTTP Basic/denegación con ErrorResponse y se añadió un mensaje 500 seguro. No cambia la lógica financiera ni los DTOs de éxito.
- Preview muestra todos los donantes positivos de la misma cuenta y no valida el periodo ni la financiación propuesta. Confirmación sí revalida; no afirmar que preview es una reserva o garantía.
- No existen registro/login/token, calendario automático, abrir DRAFT, edición de periodo ni endpoint individual de cuenta/sobre. La UI debe usar listas y operaciones realmente disponibles.
- CORS sigue sin configurar. Usar mismo origen/proxy o acordar una ampliación posterior.
- El protocolo de errores sólo tiene error y message. No existen códigos financieros estables por regla; la UI no debe parsear mensajes.
- Validación transaccional MongoDB real sigue dependiendo de Docker. Su ausencia no afecta la generación del contrato, pero no equivale a probar atomicidad.

Las guías frontend y los ejemplos complementan el YAML para reglas de negocio que JSON Schema no expresa (misma cuenta, suma de aportaciones, permisos por hogar o idempotencia). No son una segunda especificación OpenAPI.

## Validación externa y generación de tipos verificadas

```bash
npx --yes @redocly/cli lint docs/openapi/bank-api-v1.yaml --extends minimal
npx --yes openapi-typescript@7.13.0 docs/openapi/bank-api-v1.yaml -o target/generated/bank-api.d.ts
```

Ambos comandos se ejecutaron correctamente sobre el archivo exportado. El segundo sólo produjo un artefacto temporal para comprobar generación; no se implementó frontend y `mvn clean` lo elimina. Redocly comprueba sintaxis/estructura OpenAPI; las reglas financieras y las referencias de hogar siguen validadas por el backend y no por ese linter.

Las cuatro pruebas de OpenApiContractTest también comparan las rutas con RequestMappingHandlerMapping, deserializan los 17 ejemplos de request mediante DTOs reales y verifican sus constraints Jakarta. Los errores 500/404 se prueban por HTTP; el log de excepción de la prueba 500 es intencional y no indica un fallo de la suite.

El reemplazo de correcciones usa un discriminador REST con ocho variantes. El customizer aplana la herencia generada y fija mappings/enum por variante para evitar referencias recursivas en oneOf y permitir generar clientes.

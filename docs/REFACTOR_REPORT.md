# Revisión implementada de BANK API v1

Fecha: 2026-11-09. Trabajo directo sobre el código existente; sin reconstruir la aplicación. Se conservan `dev.ioza:bank-api`, paquete `dev.ioza.bank`, Java 17 y Spring Boot 4.1.1.

## Actualización: paquetes de infraestructura

Todos los adaptadores se trasladaron a `dev.ioza.bank.infrastructure.adapters`, incluyendo controllers, DTOs, advice HTTP, adapters MongoDB, documentos, repositorios y mappers. InfrastructureConfig está en `infrastructure.config` y SecurityConfig en `infrastructure.security`. Se actualizaron imports, pruebas, árbol de paquetes e inventario de archivos.

Se compararon los 149 archivos principales antes y después: sólo cambian declaraciones de paquete e imports. El contenido completo de domain y application permanece idéntico. No se cambiaron rutas, DTOs, nombres de beans, métodos @Bean, constructores ni lógica financiera. El escaneo de Spring sigue abarcando dev.ioza.bank.

ArchitectureTest protege ahora todas las clases de application frente a imports de infraestructura, exige que los paquetes raíz sean domain/application/infrastructure y comprueba que Spring descubra los componentes reubicados y una única implementación por puerto de entrada/salida con implementación como componente.

Verificación de esta actualización con Java 17: `mvn clean test` y `mvn clean package` terminan con BUILD SUCCESS. Son 42 casos: 33 aprobados, 0 fallos/errores y 9 pruebas MongoDB omitidas por ausencia de Docker. Las pruebas REST y financieras existentes se conservan. La validación del arranque con MongoDB real sigue pendiente.

Los apartados siguientes documentan la revisión financiera anterior y sus resultados originales (41 casos).

## Resultado de la refactorización

- Separación de modelos, enums, commands, puertos y documentos en archivos individuales. 149 archivos Java principales y siete archivos de pruebas.
- Dominio libre de Spring/MongoDB/HTTP, excepciones de negocio en domain/exceptions, requests y responses independientes, mappers explícitos en infraestructura.
- Contratos específicos por operación en application/usecase, compuestos por el puerto Finance; servicios conservan la orquestación existente.
- Imports explícitos, eliminación de imports sin uso y formato Java uniforme.
- Documentos monetarios Decimal128 explícitos también para líneas del presupuesto, financiamiento y efectos de auditoría.
- Idempotencia transaccional, filtros/paginación real, validación Jakarta, errores HTTP y controles de saldos y referencias.

Árbol final y decisiones: [ARCHITECTURE.md](ARCHITECTURE.md). Inventario de archivos intervenidos: [FILES_CHANGED.md](FILES_CHANGED.md). Contratos y ejemplos: [ENDPOINTS.md](ENDPOINTS.md) y [API.http](API.http).

## Problemas encontrados y corregidos

| Problema inicial | Corrección |
|---|---|
| Maven no compilaba: MongoPersistenceAdapter implementaba múltiples find/all con firmas idénticas y retornos incompatibles | Retirado el adapter duplicado; se conservan los siete adapters especializados |
| Tipos agrupados en seis clases contenedoras, imports wildcard y nombres calificados | Separación de tipos y limpieza de imports |
| Controllers recibían commands y devolvían directamente el dominio | DTOs request/response y conversión explícita; validación antes de invocar casos de uso |
| Importes de estructuras embebidas dependían de configuración global | Documentos embebidos propios con FieldType.DECIMAL128 |
| Ausencia de protección frente a reintentos financieros | Idempotency-Key por hogar, fingerprint de operación/command y respuesta original persistida en la misma transacción |
| Fechas Instant con nanosegundos podían diferir al recuperar una respuesta guardada por MongoDB | Movimientos nuevos usan precisión de milisegundos, igual que la persistencia BSON |
| Saldo inicial repetible después de vaciar una cuenta | Consulta histórica impide registrar otro saldo inicial, incluso tras reversión |
| Cierre de quincena y planificación podían competir con movimientos o creación de periodos | Revisiones técnicas de periodo/hogar dentro de transacciones para detectar conflictos |
| Preview descartaba donantes que individualmente no cubrían el faltante | Incluye sobres activos con saldo positivo de la misma cuenta para combinar donantes |
| Consultas de movimientos limitadas a los últimos 100 sin filtros | Filtros en MongoDB y paginación page/size, conservando lista JSON |
| Se aceptaban expectativas de ingreso negativas y faltaban validaciones de estado | Validación de importes, hogares y fuentes activas y referencias del hogar |
| Configuración transaccional sólo declarativa | Snapshot/majority explícitos, comprobación de topología al arrancar e inicializador esperando PRIMARY |
| Infraestructura podía filtrar mensajes internos al cliente | Errores persistentes/concurrentes separados y respuestas HTTP sin detalles del driver |

Cambios deliberados del contrato: cabecera Idempotency-Key obligatoria en POST financieros; precisión monetaria limitada a dos decimales sin redondeo ni ceros adicionales. Se conservan rutas, nombres de campos y códigos de éxito existentes. Los parámetros de filtros son opcionales.

## Compilación y pruebas

La ejecución inicial de `mvn test` falló por los errores del adapter duplicado. Tras corregirlos pasaron las 11 pruebas originales. La verificación final se realiza con Microsoft OpenJDK 17.0.18 y javac release 17:

| Comando | Resultado |
|---|---|
| `mvn clean test` | BUILD SUCCESS; 41 casos detectados, 32 ejecutados correctamente y 9 omitidos |
| `mvn clean package` | BUILD SUCCESS; JAR ejecutable Spring Boot generado; misma suite y omisiones |

Artefacto: `target/bank-api-0.1.0-SNAPSHOT.jar`. Reportes JUnit: `target/surefire-reports`.

| Suite | Ejecutados correctamente | Omitidos |
|---|---:|---:|
| MoneyTest | 5 | 0 |
| MovementServiceTest | 15 | 0 |
| SetupBudgetServiceTest | 4 | 0 |
| IdempotentMovementServiceTest | 4 | 0 |
| HttpContractTest | 2 | 0 |
| ArchitectureTest | 2 | 0 |
| MongoFinanceIntegrationTest | 0 | 9 |

Las pruebas ejecutadas cubren creación de hogar/cuentas/sobres, ingreso, reservas digitales, transferencias, reasignación, gasto simple y financiado, insuficiencia de fondos, remanentes/snapshots, reversión y validación contra saldos actuales, repetición de clave, permisos de hogar, DTOs y contratos REST, paginación y arquitectura.

Las nueve pruebas de integración usan Testcontainers con MongoDB 8.0 replica set y proxies Spring reales, sin mocks de persistencia. Cubren gasto inteligente, Decimal128 anidado, preview/revalidación, rollback al fallar el historial, replay, gastos concurrentes, duplicados simultáneos, @Version, snapshots y reversión/filtros. Están compiladas pero no ejecutadas: Testcontainers no encontró un entorno Docker válido. **No se da por validada la atomicidad real ni el arranque contra MongoDB.**

## Estado de las transacciones

Configuración revisada e implementada: MongoTransactionManager, @Transactional en operaciones financieras y el wrapper de idempotencia, @Version en cuentas/sobres, revisión de periodo frente a cierre y revisión de hogar para planificación, índice parcial único de reversión e inserción única del registro de idempotencia. Compose usa rs0; el arranque rechaza standalone.

Verificación funcional pendiente en replica set real. Ejecutar `./scripts/verify-mongo.ps1` en un equipo con Docker; el script falla si Docker no está disponible o si se omite una prueba. Luego verificar Compose y el arranque HTTP. La comprobación de topología no reemplaza una prueba de rollback.

## Riesgos y siguiente versión

1. Ejecutar las pruebas MongoDB sin omisiones, probar caída de conexión/commit incierto y verificar la imagen Docker. Dockerfile/Compose no fueron ejecutados aquí.
2. Ensayar migración de una copia de datos heredados: metadata _class, importes antiguos en cadenas e índices únicos. No se migraron ni modificaron bases reales.
3. La revisión de periodo serializa operaciones de una quincena; evaluar contención antes de aumentar carga.
4. Se conserva autenticación local en memoria. No hay entidad User persistente ni OIDC/invitaciones verificadas; no inventar una colección de usuarios independiente de la autenticación real.
5. La idempotencia no caduca. Definir retención segura y observabilidad; una carrera puede responder 409 y requiere repetir la misma clave.
6. Paginación por offset sin total/cursor; puede desplazarse con nuevas escrituras. Evaluar índices según filtros y volumen reales.
7. Se conserva el alcance previo de ajustes auditados mediante operaciones/reversiones. No hay edición directa de saldo ni endpoint para abrir un periodo creado como DRAFT.

El directorio recibido no contiene repositorio Git; no se generaron commits ni un diff Git. El inventario enumera archivos añadidos/intervenidos y contenedores retirados. Pendientes detallados en [CODEX_NEXT.md](CODEX_NEXT.md).

# Pendientes después de la revisión

1. Ejecutar las nueve pruebas MongoFinanceIntegrationTest sin omisiones en Docker. El entorno de esta revisión no tiene Docker Engine; el script verify-mongo.ps1 impide dar verde si se omiten.
2. Verificar Compose, construcción de imagen y arranque HTTP, incluyendo rechazo de standalone y recuperación después de caídas de MongoDB.
3. Ensayar una actualización desde datos heredados: metadata _class tras separar tipos, cadenas monetarias, índices únicos y totales financieros. No modificar una base real sin respaldo.
4. Reemplazar HTTP Basic y usuarios en memoria por identidad persistente/OIDC, invitaciones verificadas e identificadores inmutables; añadir pruebas HTTP de seguridad completas.
5. Diseñar una política operativa de retención y consulta de idempotencia: hoy no caduca, para no habilitar duplicados antiguos.
6. Evaluar reducción de contención por la revisión de periodo (serializa escrituras de una quincena). No retirar la protección frente al cierre sin una alternativa probada.
7. Añadir pruebas de fallos de commit y reconexión del replica set, observabilidad y backups restaurables. No reintentar automáticamente operaciones con claves nuevas.
8. Para volúmenes mayores, evaluar cursor de paginación e índices compuestos según filtros reales. La API actual usa offset y conserva lista JSON sin total.
9. Planificación pendiente vs fondeado, ajustes explícitos y auditoría de cambios del plan quedan fuera de esta revisión; no añadir saldos de prueba ni dinero ficticio.

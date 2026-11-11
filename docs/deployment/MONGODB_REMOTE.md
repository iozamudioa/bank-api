# Habilitar transacciones en el MongoDB existente

Servidor: 192.168.68.101, contenedor mongodb, imagen mongo:8 y datos en /home/iozamudioa/docker/mongo/data. El error de BANK indica que el servidor responde como standalone: agregar replicaSet a la URI por sí solo no convierte el servidor.

Un replica set de un nodo permite transacciones para este entorno, aunque no añade redundancia. Con autenticación habilitada, MongoDB también requiere autenticación interna del replica set mediante keyfile o certificados. Referencia: [conversión oficial de standalone a replica set](https://www.mongodb.com/docs/v8.0/tutorial/convert-standalone-to-replica-set/).

## Pasos en el servidor Linux

1. Respalda la base existente antes del cambio y conserva el Compose actual. El reinicio interrumpe temporalmente las conexiones. No borres el directorio data ni los usuarios existentes.

2. Con el contenedor mongodb todavía arrancado, copia scripts/prepare-mongo-replica-key.sh al servidor y ejecútalo:

```bash
bash prepare-mongo-replica-key.sh
```

El script identifica UID/GID del usuario mongodb del contenedor, crea un keyfile persistente con permisos 400 y no lo imprime. Requiere Docker, OpenSSL y sudo en el servidor. Si el archivo ya existe, no lo reemplaza. La ruta es /home/iozamudioa/docker/mongo/replica-set.key y debe ser un archivo, no un directorio.

3. Adapta tu Compose al archivo mongodb-replica-set.compose.yaml de esta carpeta. Conserva tus valores reales de MONGO_INITDB_ROOT_USERNAME/PASSWORD. Los cambios necesarios son command con --replSet rs0 y --keyFile, y el montaje adicional del keyfile. Mantén el bind mount de data y la red infra-net. Las variables INITDB no cambian contraseñas de una base ya inicializada: usa las credenciales existentes.

4. Desde el directorio de tu Compose real, recrea únicamente el servicio:

```bash
docker compose up -d --force-recreate mongodb
docker logs --tail 50 mongodb
```

No se usa el Compose local de BANK para este servidor. Revisa que MongoDB arranque sin errores de permisos del keyfile; no desactives autenticación para evitarlos.

5. Entra a mongosh dentro del contenedor. Sustituye TU_USUARIO_ACTUAL; --password pide la contraseña sin incluirla en el comando:

```bash
docker exec -it mongodb mongosh --host localhost --port 27017 --username TU_USUARIO_ACTUAL --authenticationDatabase admin --password
```

Dentro de mongosh, inicializa una sola vez:

```javascript
rs.initiate({
  _id: "rs0",
  members: [{ _id: 0, host: "mongodb:27017" }]
})
```

Se anuncia el nombre Docker mongodb, resoluble dentro de infra-net. Si rs0 ya fue inicializado, no repitas initiate: consulta rs.status(). No cambies una configuración de replica set existente sin revisarla.

6. Espera la elección de PRIMARY y verifica:

```javascript
db.hello().setName
db.hello().isWritablePrimary
rs.status().members.map(m => ({ name: m.name, state: m.stateStr }))
```

Resultados esperados: rs0, true y mongodb:27017 en estado PRIMARY.

## Conexión desde BANK en Windows

La URI de .vscode/settings.json conserva las credenciales configuradas y añade replicaSet=rs0 y directConnection=true:

```text
mongodb://USUARIO:CLAVE@192.168.68.101:27017/bank_db?authSource=admin&replicaSet=rs0&directConnection=true
```

directConnection=true permite este acceso de desarrollo al único nodo por la IP publicada, sin intentar resolver el nombre interno mongodb desde Windows. No debe extrapolarse a una configuración de alta disponibilidad con varios nodos.

Una vez que el servidor sea PRIMARY, cierra la terminal integrada anterior de VS Code, abre una nueva y ejecuta mvn spring-boot:run. Si arrancas con Run/Debug de Java, las variables de terminal.integrated.env.windows no se inyectan en ese launcher: usa la terminal o configura su launch.json/env correspondiente.

El control de BANK que rechaza standalone permanece activo para impedir operaciones financieras sin atomicidad multidocumento. Estos archivos preparan el cambio; no se han ejecutado comandos ni reinicios sobre el servidor remoto desde Codex.

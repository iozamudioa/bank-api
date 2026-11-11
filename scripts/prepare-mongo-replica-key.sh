#!/usr/bin/env bash
# Ejecutar en el servidor Linux que ya contiene el contenedor mongodb.
# No reinicia MongoDB ni modifica datos o usuarios.
set -euo pipefail

key_path='/home/iozamudioa/docker/mongo/replica-set.key'
command -v docker >/dev/null
command -v openssl >/dev/null
command -v sudo >/dev/null

if [[ -e "$key_path" ]]; then
  if [[ ! -f "$key_path" ]] || ! sudo test -s "$key_path"; then
    echo 'El destino existe pero no es un archivo de clave válido no vacío; revisar manualmente.' >&2
    exit 1
  fi
  echo 'Ya existe el keyfile; no se reemplazó. Conservarlo para reinicios futuros.'
  exit 0
fi

mongo_uid="$(docker exec mongodb id -u mongodb)"
mongo_gid="$(docker exec mongodb id -g mongodb)"
[[ "$mongo_uid" =~ ^[0-9]+$ && "$mongo_gid" =~ ^[0-9]+$ ]]

# Crear primero con permisos restrictivos; nunca mostrar la clave por terminal.
sudo install -o "$mongo_uid" -g "$mongo_gid" -m 400 /dev/null "$key_path"
openssl rand -base64 756 | sudo tee "$key_path" >/dev/null
sudo test -s "$key_path"
echo "Keyfile creado en $key_path, propietario $mongo_uid:$mongo_gid y permisos 400."

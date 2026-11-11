#!/usr/bin/env bash
set -euo pipefail
mongosh 'mongodb://mongo:27017/admin?directConnection=true' --quiet --eval \
  'try { rs.status(); print("Replica set ya iniciado") } catch (e) { if (e.code !== 94) throw e; rs.initiate({_id:"rs0",members:[{_id:0,host:"mongo:27017"}]}) }'
for attempt in {1..60}; do
  if mongosh 'mongodb://mongo:27017/admin?directConnection=true' --quiet --eval \
    'if (!db.hello().isWritablePrimary) quit(1)'; then
    echo 'Replica set listo para transacciones'
    exit 0
  fi
  sleep 1
done
echo 'No se eligió PRIMARY en 60 segundos' >&2
exit 1

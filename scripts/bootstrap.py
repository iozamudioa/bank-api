"""Sembrar SOLO catálogo y plan de BANK; jamás saldos ni movimientos.

Requisitos: API levantada, usuario propietario autenticado mediante variables de entorno.
Ejemplo PowerShell:
  $env:BANK_OWNER_PASSWORD='...'
  python scripts/bootstrap.py

Detente si ya tienes hogares creados; esta herramienta crea un hogar nuevo.
"""
import base64
import json
import os
from decimal import Decimal
from urllib.request import Request, urlopen
from urllib.error import HTTPError

BASE = os.environ.get('BANK_BASE_URL', 'http://localhost:8080/api/v1')
USER = os.environ.get('BANK_OWNER_USERNAME', 'olda')
PASSWORD = os.environ.get('BANK_OWNER_PASSWORD')
if not PASSWORD:
    raise SystemExit('Falta BANK_OWNER_PASSWORD; configura tu contraseña local antes de ejecutar.')

PHYSICAL = [
 ('Central de Abastos','4500.00','SPENDING','Alimentación'),
 ('Pan y tortillas','1500.00','SPENDING','Alimentación'),
 ("Sam's",'1000.00','SAVINGS','Alimentación'),
 ('Cecy','1500.00','SPENDING','Familia'),
 ('Tres hijas','1300.00','SAVINGS','Familia'),
 ('Gas LP','135.00','SAVINGS','Hogar'),
 ('Taekwondo','250.00','SAVINGS','Educación'),
 ('Cambio','15.00','SPENDING','Otros'),
 ('Olda','1500.00','SPENDING','Familia')]
DIGITAL = [
 ('DOGO','375.00','SPENDING','Hogar'),
 ('Gasolina Aveo','2500.00','SPENDING','Transporte'),
 ('Nancy','1517.00','SPENDING','Servicios'),
 ('Félix','450.00','SPENDING','Servicios'),
 ('Tanda','1000.00','SAVINGS','Ahorro'),
 ('Telcel','1209.00','SPENDING','Servicios'),
 ('Telmex','372.00','SPENDING','Servicios'),
 ('CFE','200.00','SAVINGS','Hogar'),
 ('Mantenimiento','350.00','SAVINGS','Hogar'),
 ('YouTube','159.50','SPENDING','Suscripciones'),
 ('Prime Video','149.00','SPENDING','Suscripciones'),
 ('ChatGPT Plus','199.50','SPENDING','Suscripciones'),
 ('Emergencias','669.00','SAVINGS','Ahorro'),
 ('Andrea','150.00','SPENDING','Servicios')]
assert sum((Decimal(x[1]) for x in PHYSICAL),Decimal('0'))==Decimal('11700.00')
assert sum((Decimal(x[1]) for x in DIGITAL),Decimal('0'))==Decimal('9300.00')

def req(path, data):
    blob=json.dumps(data,ensure_ascii=False).encode('utf-8')
    auth=base64.b64encode(f'{USER}:{PASSWORD}'.encode()).decode()
    request=Request(BASE+path,data=blob,method='POST',headers={'Authorization':f'Basic {auth}','Content-Type':'application/json'})
    try:
        with urlopen(request,timeout=30) as response:
            return json.load(response)
    except HTTPError as error:
        raise RuntimeError(f'{path} -> HTTP {error.code}: {error.read().decode()}') from error

household=req('/households',{'name':'Familia LGZV','displayName':'Olda'})
h=household['id']
print('Household:',h)
req(f'/households/{h}/members',{'username':os.environ.get('BANK_CECY_USERNAME','cecy'),'displayName':'Cecy','role':'EDITOR'})
physical=req(f'/households/{h}/accounts',{'name':'Efectivo familiar','type':'PHYSICAL'})
digital=req(f'/households/{h}/accounts',{'name':'Cuenta digital principal','type':'DIGITAL'})
lines=[]
for collection,account in [(PHYSICAL,physical),(DIGITAL,digital)]:
    for name,amount,kind,category in collection:
        envelope=req(f'/households/{h}/envelopes',{'name':name,'accountId':account['id'],'type':kind,'category':category})
        lines.append({'envelopeId':envelope['id'],'amount':amount})
# PUT requires a separate request
payload=json.dumps({'expectedIncome':21000.00,'allocations':lines}).encode()
auth=base64.b64encode(f'{USER}:{PASSWORD}'.encode()).decode()
with urlopen(Request(BASE+f'/households/{h}/budget-template',data=payload,method='PUT',headers={'Authorization':f'Basic {auth}','Content-Type':'application/json'}),timeout=30) as r:
    print('Plantilla creada:',r.status)
for start,end in [('2026-11-15','2026-11-30'),('2026-12-01','2026-12-14'),('2026-12-15','2026-12-31')]:
    p=req(f'/households/{h}/budget-periods',{'startDate':start,'endDate':end,'open':True})
    print('Quincena:',start,'->',p['id'])
print('TERMINADO: catálogos y presupuestos únicamente. Saldos=0.00, sin depósitos ficticios.')

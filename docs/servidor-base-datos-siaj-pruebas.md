# Servidor Base de Datos SIAJ - Pruebas

Fecha de revision: 2026-07-23

## Datos generales

| Dato | Valor |
|---|---|
| IP | 30.0.0.150 |
| Hostname | agendajuridicodbdev |
| Sistema operativo | Rocky Linux 9.1 (Blue Onyx) |
| Memoria RAM | 15 GiB |
| Nucleos | 8 vCPU |
| Swap | 7.9 GiB |

## Almacenamiento

| Punto de montaje | Tamano | Sistema de archivos | Uso observado |
|---|---:|---|---:|
| / | 70 GB | xfs | 10% |
| /home | 221.1 GB | xfs | 2% |
| /boot | 1 GB | xfs | 24% |

## Base de datos

| Dato | Valor |
|---|---|
| Motor | PostgreSQL |
| Version en contenedor | 18.1 |
| Contenedor Docker | postgres_db |
| Imagen Docker | postgres:latest |
| Puerto publicado | 5432 |
| Base de datos usada por SIAJ | sistema_juridico |
| Usuario usado por SIAJ | agendajuridicodbdev |

## Servicios identificados

| Servicio | Estado / dato |
|---|---|
| Docker | Activo |
| postgres_db | Activo |
| pgadmin4 | Activo en puerto 5050 |
| PostgreSQL instalado en host | psql 13.22 |
| Servicio PostgreSQL del host | No activo; la BD operativa corre en Docker |

# Servidor Aplicativo SIAJ - Pruebas

Fecha de revision: 2026-07-23

## Datos generales

| Dato | Valor |
|---|---|
| IP | 30.0.0.151 |
| Hostname | agendajuridicoappdev |
| Sistema operativo | Rocky Linux 9.7 (Blue Onyx) |
| Memoria RAM | 15 GiB |
| Nucleos | 8 vCPU |
| Swap | 7.9 GiB |

## Almacenamiento

| Punto de montaje | Tamano | Sistema de archivos | Uso observado |
|---|---:|---|---:|
| / | 70 GB | xfs | 12% |
| /home | 221.1 GB | xfs | 2% |
| /boot | 1 GB | xfs | 31% |

## Servicios identificados

| Servicio | Estado / dato |
|---|---|
| Spring Boot | Activo en puerto 8080 |
| NGINX | Activo en puerto 80 |
| Version NGINX | 1.20.1 |
| Java | OpenJDK 17.0.17 |
| Docker | Servicio activo, sin contenedores corriendo al momento de la revision |

## Aplicacion

| Dato | Valor |
|---|---|
| JAR | /opt/siaj/app/sistema-juridico.jar |
| Perfil activo | test |
| Puerto aplicacion | 8080 |
| Base de datos configurada | jdbc:postgresql://30.0.0.150:5432/sistema_juridico |
| Usuario de BD configurado | villalobos_admin |


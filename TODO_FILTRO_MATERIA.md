# TODO - Filtro por Materia para GERENTE y JEFE_DEPTO

## Objetivo
Agregar un filtro por materia visible solo para GERENTE y JEFE_DEPTO en el calendario.

## Materias:
- Civil
- Mercantil
- Fiscal
- Administrativo
- Laboral
- Penal
- Amparo
- Transparencia

## Cambios Realizados

### 1. EventoResponse.java ✅
- [x] Agregar `materiaId` y `materiaNombre`

### 2. CalendarioController.java ✅
- [x] Agregar variable `puedeVerFiltroMateria`
- [x] Agregar lista de materias disponibles
- [x] JEFE_DEPTO ahora puede ver el filtro por usuario

### 3. CalendarioService.java ✅
- [x] Mapear la materia en las respuestas

### 4. CalendarioRestController.java ✅
- [x] Agregar parámetro `materia` al endpoint

### 5. calendar-header.html ✅
- [x] Agregar select de materia

### 6. calendario-module.js ✅
- [x] Agregar lógica del filtro

## Resultado
- ✅ Filtro por materia visible para GERENTE y JEFE_DEPTO
- ✅ Las materias disponibles son: Civil, Mercantil, Fiscal, Administrativo, Laboral, Penal, Amparo, Transparencia
- ✅ JEFE_DEPTO puede ver el filtro por usuario (todos los usuarios)
- ✅ El filtrado funciona correctamente en el backend
- ✅ El proyecto compila sin errores


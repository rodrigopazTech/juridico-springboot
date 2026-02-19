# TODO - Implementación Checkbox "Mis Asuntos" para ABOGADO

## Objetivo
Agregar un checkbox "Mis asuntos" visible solo para el rol ABOGADO que permita filtrar eventos para mostrar solo los asuntos asignados a ese abogado.

## Pasos de Implementación

### Paso 1: Modificar CalendarioController.java
- [ ] Agregar atributo `esAbogado` al modelo (true cuando el rol es ABOGADO)
- [ ] Pasar el atributo al modelo

### Paso 2: Modificar calendar-header.html
- [ ] Agregar checkbox "Mis asuntos" visible solo para ABOGADO
- [ ] Ubicarlo en la sección de filtros con estilos apropiados
- [ ] ID del checkbox: `chkMisAsuntos`

### Paso 3: Modificar CalendarioRestController.java
- [ ] Agregar parámetro booleano `misAsuntos` al endpoint `/eventos`
- [ ] Cuando es true y el usuario es ABOGADO, forzar filtro por ID del usuario actual

### Paso 4: Modificar CalendarioService.java
- [ ] Agregar lógica para filtrar eventos por usuarioId cuando `misAsuntos` es true

### Paso 5: Modificar calendario-module.js
- [ ] Agregar evento change al checkbox
- [ ] Incluir el ID del usuario actual en el filtro cuando está checkeado
- [ ] Llamar al API con el nuevo parámetro

## Notas
- El checkbox es visible SOLO para el rol ABOGADO
- Por defecto está desmarcado (el usuario puede optar por ver solo sus asuntos)
- El filtrado se hace en el backend para mayor seguridad

## Estado
- [x] Plan aprobado
- [x] Implementación completada

## Archivos Modificados

### 1. CalendarioController.java
- Agregada variable `esAbogado` al modelo
- Configurada en `true` cuando el rol del usuario es ABOGADO
- Filtro de gerencia oculto para GERENTE (`puedeVerFiltroGerencia = false`)

### 2. calendar-header.html
- Agregado checkbox "Mis asuntos" visible solo para ABOGADO
- ID: `chkMisAsuntos`

### 3. CalendarioRestController.java
- Agregado parámetro `misAsuntos` al endpoint `/eventos`
- Cuando es true, fuerza el filtrado por el ID del usuario actual
- GERENTE: forza filtrado por su propia gerencia automáticamente

### 4. calendario-module.js
- Actualizado `loadEvents()` para incluir parámetros en la URL
- Agregado evento change al checkbox "Mis asuntos"
- El checkbox se desmarca al limpiar filtros
- **Colores actualizados**: audiencias (#691228), términos (#13322B), recordatorios (#D4C19C)

### 5. tailwind.config.js
- Actualizados colores de eventos:
  - evento-audiencia: #691228 (Gob Guinda)
  - evento-termino: #13322B (Gob Verde)
  - evento-recordatorio: #D4C19C (Gob Oro)

### 6. calendario.css
- Actualizados colores de eventos:
  - .event-audiencia: #691228
  - .event-termino: #13322B
  - .event-recordatorio: #D4C19C


# TODO - Implementación de Filtro de Usuario en Calendario

## Objetivo
Agregar un filtro llamado "usuario" que filtre por los roles de los usuarios, visible para todos los roles e invisible para el rol ABOGADO.

## Pasos de Implementación

### Paso 1: Modificar CalendarioController.java
- [x] Agregar variable `puedeVerFiltroUsuario` (false para ABOGADO, true para otros)
- [x] Agregar lógica para obtener `listaUsuarios` basada en la gerencia del usuario
- [x] Pasar ambos atributos al modelo

### Paso 2: Modificar calendar-header.html
- [x] Agregar bloque `div` con select de usuario
- [x] Condicionar visibilidad con `th:unless="${userRole == 'ABOGADO'}"`
- [x] Poblar con lista de usuarios del modelo

### Paso 3: Modificar CalendarioRestController.java
- [x] Agregar endpoint `/usuarios` para obtener lista de usuarios activos
- [x] Agregar lógica de permisos (ABOGADO no puede ver usuarios)
- [x] Agregar parámetro `usuario` al endpoint de eventos

### Paso 4: Modificar CalendarioService.java
- [x] Agregar parámetro `filtroUsuarioId` al método obtenerEventosCalendario
- [x] Agregar lógica de filtrado por usuarioId en el método aplicarFiltros
- [x] Actualizar llamada al método en CalendarioRestController

### Paso 5: Modificar calendario-module.js
- [x] Agregar lógica para manejar el filtro de usuario en `applyFilters()`
- [x] Actualizar método `setupEventListeners()` para limpiar el nuevo filtro
- [x] Actualizar método `btnClearFilters()` para limpiar el filtro de usuario

### Paso 6: Implementar Vista Semanal
- [x] Actualizar calendar-week-view.html con estructura de grid y horas
- [x] Implementar método renderWeek() en calendario-module.js con:
  - Header con días de la semana (Lunes a Domingo)
  - Columna de horas (00:00 a 23:00)
  - Eventos posicionados por hora

### Paso 7: Implementar Vista Diaria
- [x] Actualizar calendar-day-view.html con línea de tiempo detallada
- [x] Implementar método renderDay() en calendario-module.js con:
  - Título del día con nombre y fecha
  - Timeline con eventos ordenados por hora
  - Información detallada de cada evento (expediente, responsable, gerencia)

### Paso 8: Actualizar navegación
- [x] Actualizar método `navigate()` para semana (+/- 7 días) y día (+/- 1 día)
- [x] Actualizar método `render()` para mostrar vistas week y day

## Notas
- El filtro de usuario está oculto para el rol ABOGADO
- Para GERENTE: solo muestra usuarios de su propia gerencia
- Para DIRECCION/SUBDIRECCION: muestra todos los usuarios activos
- Para ABOGADO: no muestra el filtro y ve todos los eventos

## Estado
- [x] Plan aprobado por el usuario
- [x] Implementación de filtro de usuario completada
- [x] Implementación de vistas semanal y diaria completada


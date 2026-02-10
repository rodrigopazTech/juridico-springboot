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

## Notas
- El filtro de usuario está oculto para el rol ABOGADO
- Para GERENTE: solo muestra usuarios de su propia gerencia
- Para DIRECCION/SUBDIRECCION: muestra todos los usuarios activos
- Para ABOGADO: no muestra el filtro y ve todos los eventos

## Estado
- [x] Plan aprobado por el usuario
- [x] En progreso
- [x] Completado


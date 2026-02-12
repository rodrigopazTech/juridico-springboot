# TODO: Filtro por Grupos de Gerencias en Dashboard

## Objetivo
Agregar filtro en el dashboard para mostrar datos de "super-gerencias":
- Gerencia Civil, Mercantil, Fiscal y Administrativo
- Gerencia Laboral y Penal
- Gerencia Transparencia y Amparo

---

## Tareas

### 1. Backend - Repository ✅ COMPLETADO
- [x] ExpedienteRepository: Agregar métodos para filtrar por lista de gerencias
- [x] AudienciaRepository: Agregar métodos para filtrar por lista de gerencias  
- [x] TerminoRepository: Agregar métodos para filtrar por lista de gerencias

### 2. Backend - DashboardService ✅ COMPLETADO
- [x] Modificar `obtenerKpis()` para aceptar lista de gerencias
- [x] Modificar `obtenerMetricas()` para aceptar lista de gerencias
- [x] Crear método helper para mapear grupoGerencia a lista de IDs

### 3. Backend - DashboardController ✅ COMPLETADO
- [x] Agregar parámetro `String grupoGerencia` al endpoint GET `/dashboard`
- [x] Agregar parámetro `String grupoGerencia` al endpoint GET `/dashboard/data`
- [x] Actualizar lógica de validación para manejar grupos

### 4. Frontend - HTML ✅ COMPLETADO
- [x] Agregar opciones de "Grupos de Gerencias" al select en index.html
- [x] Añadir divider visual entre gerencias individuales y grupos

### 5. Frontend - JavaScript ✅ COMPLETADO
- [x] Actualizar `actualizarDashboard()` para enviar `grupoGerencia`
- [x] Manejar correctamente el cambio entre gerencia individual y grupo

### 6. Testing
- [ ] Verificar que el filtro funcione para DIRECTOR/SUBDIRECTOR
- [ ] Verificar que otros roles vean solo su gerencia
- [ ] Verificar que la carga de datos sea correcta

---

## Notas
- Los grupos de gerencias son:
  - CIVIL_MERCANTIL: Civil, Mercantil, Fiscal, Administrativo (IDs 1-4)
  - LABORAL_PENAL: Laboral, Penal (IDs 5-6)
  - TRANSPARENCIA: Transparencia, Amparo (IDs 7-8)

## Archivos Modificados
1. `ExpedienteRepository.java` - Nuevos métodos para filtrar por lista de gerencias
2. `AudienciaRepository.java` - Nuevo método `contarAudienciasPorUsuarioYGerencias`
3. `TerminoRepository.java` - Nuevo método `contarTerminosPorUsuarioYGerencias`
4. `DashboardService.java` - Métodos sobrecargados para manejar grupos de gerencias
5. `DashboardController.java` - Parámetro `grupoGerencia` y lógica de validación
6. `dashboard.js` - Actualización de URL para incluir `grupoGerencia`
7. `index.html` - Opciones de grupos en el dropdown


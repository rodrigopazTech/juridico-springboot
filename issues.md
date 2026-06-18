Sí. Viendo el backlog formal y lo que ya avanzamos en la rama, los pendientes reales quedan así:

**Ya trabajados**
- `ISSUE-01`: prácticamente resuelto.
  Ya existe evento formal, histórico con `evento`, y el envío dejó de depender del título.
- `ISSUE-02`: muy avanzado, casi resuelto.
  Ya existe preferencia por evento, UI agrupada, siembra inicial, respeto a superiores y correcciones funcionales. Lo que le queda es más bien cierre/pulido:
  - limpieza de acentos y `ñ`
  - validar visualmente toda la pantalla `Mi Cuenta`
  - dejar estable el guardado en todos los casos de uso

**Pendientes fuertes**
1. `ISSUE-03`  
   Alertas críticas faltantes:
   - `audiencia reprogramada`
   - `audiencia cancelada`
   - `cambio de fecha limite de termino`
   - `termino vencido` como evento formal distinto

2. `ISSUE-05`  
   Alertas obligatorias de seguridad:
   - `inicio de sesion desde otro equipo`
   - `cambio de rol o permisos`
   - consolidar `contrasena por vencer` como obligatoria

3. `ISSUE-04`  
   `documento agregado al expediente`

**Pendientes estructurales / de cierre**
- versionar y dejar completo el SQL realtime en repo (`ARC-005`)
- pasada de textos rotos: acentos, `ñ`, cadenas legacy
- revisar QA funcional completo de campana, historial y `Mi Cuenta`

**Pendientes de análisis posterior**
- `KPI-003`: cambios relevantes del expediente para dashboard
- `KPI-006`: expedientes sin actividad para dashboard
- `NTF-029/030/031`: tareas y recordatorios personales

**Mi lectura honesta del estado**
Si lo ordenamos por ejecución real, yo diría que vamos así:

1. Cerrar `ISSUE-02` del todo con limpieza visual y QA.
2. Entrar a `ISSUE-03`.
3. Luego `ISSUE-05`.
4. Después `ISSUE-04`.

Además, en el working tree todavía hay cambios sin cerrar en:
- [C:\Users\Rodrigo.DESKTOP-2MLHEH3\Desktop\Proyectos_Desarrollo\juridico-springboot\src\main\java\com\juridico\sistema_juridico\controller\PerfilController.java](C:/Users/Rodrigo.DESKTOP-2MLHEH3/Desktop/Proyectos_Desarrollo/juridico-springboot/src/main/java/com/juridico/sistema_juridico/controller/PerfilController.java)
- [C:\Users\Rodrigo.DESKTOP-2MLHEH3\Desktop\Proyectos_Desarrollo\juridico-springboot\src\main\resources\templates\fragments\perfil\notificaciones.html](C:/Users/Rodrigo.DESKTOP-2MLHEH3/Desktop/Proyectos_Desarrollo/juridico-springboot/src/main/resources/templates/fragments/perfil/notificaciones.html)
- [C:\Users\Rodrigo.DESKTOP-2MLHEH3\Desktop\Proyectos_Desarrollo\juridico-springboot\src\main\resources\templates\views\perfil\index.html](C:/Users/Rodrigo.DESKTOP-2MLHEH3/Desktop/Proyectos_Desarrollo/juridico-springboot/src/main/resources/templates/views/perfil/index.html)

Mi recomendación: cerramos bien `ISSUE-02` con la limpieza de acentos y una verificación completa de `Mi Cuenta`, y enseguida arrancamos `ISSUE-03`.
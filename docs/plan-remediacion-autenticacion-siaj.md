# Plan de Remediación — Autenticación y Administración de Usuarios SIAJ

## 1. Objetivo

Estabilizar los procesos de creación, activación, autenticación, recuperación y revocación de acceso antes de liberar SIAJ a producción. El plan transforma los defectos identificados en cambios concretos de configuración, backend, frontend, seguridad y pruebas.

## Estado de ejecución

Actualizado el 9 de septiembre de 2026:

- **R-05 en implementación:** el formulario de recuperación manual ya es accesible desde inicio de sesión, recuperación de contraseña y recordatorio de usuario. La entrega administrativa incluye la ruta, el vencimiento y una explicación de que el código no sustituye la contraseña.
- **R-06 parcialmente implementada:** la recuperación web, el recordatorio de usuario y la API ya distinguen un fallo real de entrega y evitan mostrarlo como éxito.
- **R-11 parcialmente implementada:** se corrigieron las instrucciones públicas del código de recuperación.
- **R-12 iniciada:** se añadieron pruebas del controlador para fallos de entrega. La selección ejecutada acumula nueve pruebas satisfactorias.
- **Corrección complementaria aplicada:** el nivel `warning` del resultado de activación ya no se sobrescribe con `success`.

Los cambios estructurales R-01, R-02, R-03, R-04, R-07, R-08 y R-10 continúan como bloqueantes de producción.

## 2. Plan por corrección

| ID | Componente | Archivos o módulos principales | Cambio requerido | Dependencias | Riesgo | Complejidad | Esfuerzo relativo | Prioridad |
|---|---|---|---|---|---|---|---|---|
| R-01 | Correo institucional | `PasswordResetEmailService`, `PasswordResetMailer`, configuración de correo | Sustituir SMTP básico por Microsoft Graph u OAuth2; persistir estados de entrega | Configuración Entra y buzón institucional | Interrupción temporal de notificaciones | Alta | 5–8 días | P0 |
| R-02 | Identidad de usuario | `Usuario`, `CustomUserDetailsService`, JWT y repositorios | Introducir `authSubject` UUID independiente del correo | Migración de BD | Incompatibilidad con tokens existentes | Alta | 4–6 días | P0 |
| R-03 | Ciclo de cuenta | `Usuario`, `UsuarioService`, `UsuariosController` | Incorporar estados `PENDIENTE_ACTIVACION`, `ACTIVA`, `SUSPENDIDA`, `BLOQUEADA`, `BAJA` | R-02 | Clasificación incorrecta de cuentas existentes | Media | 3–4 días | P0 |
| R-04 | Invitaciones | Usuarios, activación y servicio de tokens | Separar `InvitationToken` de `PasswordResetToken` | R-01 a R-03 | Invitaciones previas incompatibles | Media | 3–5 días | P0 |
| R-05 | Código manual | `ManualRecoveryCode`, `PasswordResetService`, `recuperacion-manual.html`, `usuarios.js` | Completar navegación, instrucciones, expiración y consumo de un uso | R-02 y R-03 | Entrega del código por canal no autorizado | Media | 2–3 días | P0 |
| R-06 | Recuperación | `PasswordResetController`, `PasswordResetService`, `UsernameRecoveryService` | Manejar explícitamente indisponibilidad de correo sin revelar existencia de cuentas | R-01 y R-05 | Enumeración de cuentas si el mensaje es incorrecto | Media | 2–3 días | P0 |
| R-07 | Sesiones y JWT | `JwtAuthenticationFilter`, `JwtTokenProvider`, sesiones HTTP | Añadir `sessionVersion` y revocación centralizada | R-02 y R-03 | Cierre de sesiones durante despliegue | Alta | 4–5 días | P0 |
| R-08 | Autorización administrativa | `UsuariosController`, `UsuarioService`, Spring Security | Centralizar jerarquía y alcance por gerencia | Modelo de roles | Cambio de permisos actuales | Media | 3 días | P0 |
| R-09 | Microsoft Login | `MicrosoftAuthController`, `MicrosoftGraphClient`, `MicrosoftAuthProperties` | Configurar proveedor OIDC, visibilidad y vínculo seguro | Entra, R-02, R-03 y R-07 | Fallos por URI o consentimiento | Alta | 4–6 días | P1 |
| R-10 | Seguridad web | `SecurityConfig`, controladores, JavaScript | Reactivar CSRF, retirar cambios mediante GET y endurecer cookies | Ajustes de frontend | Peticiones existentes rechazadas hasta migrarlas | Media | 2–4 días | P0 |
| R-11 | Validaciones y UX | DTOs, plantillas, `PasswordPolicyService` | Centralizar campos obligatorios, vigencia y mensajes | R-03 | Cambio de reglas visibles | Media | 2–3 días | P1 |
| R-12 | Automatización QA | `src/test`, scripts QA y pipeline | Incorporar pruebas de integración y E2E | R-01 a R-11 | Dependencia indebida de proveedores externos | Alta | 5–7 días | P0 |

### R-01. Entrega institucional de correo

Crear un puerto de entrega independiente del proveedor:

```java
public interface NotificationDeliveryService {
    DeliveryResult sendActivation(UserInvitation invitation);
    DeliveryResult sendPasswordReset(PasswordResetToken token);
    DeliveryResult sendUsernameReminder(Usuario usuario);
}

public record DeliveryResult(
        boolean accepted,
        String providerMessageId,
        String safeFailureReason) {}
```

Persistir el ciclo de entrega:

```text
PENDING -> SENT_TO_PROVIDER -> DELIVERED | FAILED
```

No confirmar al usuario que un correo fue enviado cuando el proveedor devolvió error. El reintento en texto plano debe reservarse para errores de formato; un error de autenticación debe finalizar inmediatamente.

### R-02. Identidad independiente del correo

```java
@Column(name = "auth_subject", nullable = false, unique = true, updatable = false)
private UUID authSubject;
```

El usuario podrá localizarse mediante nombre de usuario o número de empleado, pero sesiones y JWT utilizarán `authSubject` como identidad estable. El correo será un dato de contacto opcional.

### R-03. Ciclo de vida de la cuenta

```java
public enum EstadoCuenta {
    PENDIENTE_ACTIVACION,
    ACTIVA,
    SUSPENDIDA,
    BLOQUEADA,
    BAJA
}
```

La interfaz debe distinguir cuenta creada, invitación enviada y acceso activado. `activo` y `forcePasswordChange` no deben representar por sí solos todo el ciclo.

### R-04. Invitación y activación

La creación debe producir una cuenta `PENDIENTE_ACTIVACION`. Una entidad `InvitationToken` administrará expiración, reenvío y consumo, separada de recuperación de contraseña.

```text
Crear usuario
  -> validar rol y adscripción
  -> guardar PENDIENTE_ACTIVACION
  -> con correo: crear y enviar invitación
  -> sin correo: ofrecer recuperación asistida
  -> definir contraseña
  -> cambiar a ACTIVA
```

### R-05. Código de recuperación

El código no será una contraseña ni permitirá iniciar sesión directamente. Solo autorizará la definición de una contraseña nueva.

```json
{
  "code": "ABCD23EFGH",
  "expiresAt": "2026-09-08T16:30:00",
  "recoveryUrl": "/auth/recuperacion-manual",
  "instructions": "Este código no es tu contraseña. Úsalo para crear una nueva."
}
```

Añadir la opción **Tengo un código de recuperación** al inicio de sesión y a recuperación de contraseña. El código tendrá un uso, quince minutos de vigencia, límite de intentos y registro de auditoría.

### R-06. Recuperación y errores

```java
return switch (result) {
    case SENT -> RecoveryResponse.accepted("Revisa tu correo institucional.");
    case EMAIL_UNAVAILABLE -> RecoveryResponse.unavailable(
            "El correo institucional no está disponible. Solicita recuperación asistida.");
    case RATE_LIMITED -> RecoveryResponse.rateLimited("Intenta nuevamente más tarde.");
    default -> RecoveryResponse.accepted(
            "Si la cuenta existe, recibirás instrucciones de acceso.");
};
```

La respuesta pública debe evitar enumerar cuentas, pero no debe prometer una entrega cuando el servicio completo está indisponible.

### R-07. Revocación de sesiones

```java
@Column(name = "session_version", nullable = false)
private long sessionVersion = 1L;
```

```java
public void revokeAllAccess(Usuario usuario) {
    usuario.setSessionVersion(usuario.getSessionVersion() + 1);
    usuarioRepository.save(usuario);
    sessionRegistry.expireNow(usuario.getAuthSubject());
}
```

Cada JWT llevará `sessionVersion`. Desactivar, bloquear, cambiar contraseña o completar recuperación incrementará la versión y cerrará las sesiones HTTP.

### R-08. Autorización administrativa

```java
public interface UserAdministrationAuthorizationService {
    void assertCanCreate(Usuario actor, RolUsuario targetRole);
    void assertCanManage(Usuario actor, Usuario target);
    void assertCanIssueRecovery(Usuario actor, Usuario target);
}
```

Todos los endpoints de consulta, edición, invitación, recuperación, desactivación y baja deben utilizar la misma política de jerarquía y gerencia.

### R-09. Microsoft Login

El acceso Microsoft validará `state`, `nonce`, tenant, `microsoftObjectId`, estado de cuenta y versión de sesión. La disponibilidad del botón debe depender de una única decisión de configuración.

```java
public boolean isLoginAvailable() {
    return enabled && configured && uiVisible;
}
```

### R-10. Seguridad web

```java
.csrf(csrf -> csrf
    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
```

Desactivar, reactivar y eliminar deben usar POST, PATCH o DELETE. Los formularios y solicitudes AJAX deberán adjuntar CSRF. En producción, las cookies de sesión y JWT deberán ser `Secure`, `HttpOnly` y con `SameSite` definido.

### R-11. Validación centralizada

```java
public record UserCreateRequest(
        @NotBlank String nombreCompleto,
        @NotBlank String username,
        @NotNull RolUsuario rol,
        Set<Integer> gerenciasIds,
        @Email String email) {}
```

El servidor debe validar gerencia y materias según rol. La política de contraseña y su vigencia deben proceder de configuración compartida con la interfaz.

### R-12. Pruebas requeridas

Cobertura mínima:

- Alta con invitación exitosa y fallida.
- Alta sin correo y activación asistida.
- Código válido, inválido, usado y vencido.
- Recuperación de contraseña y recordatorio de usuario.
- Revocación después de cambio de contraseña y suspensión.
- Login por usuario y número de empleado.
- Microsoft vinculado, no vinculado e inactivo.
- Matriz de autorización por rol y gerencia.

## 3. Arquitectura objetivo

```text
Gestión de Usuarios
        |
        v
User Lifecycle Service -------- User Administration Authorization
        |                                      |
        +--> Invitation Service                |
        +--> Recovery Service                  |
                    |                           |
                    v                           |
          Notification Delivery Service        |
                                                |
Local Login --------+                           |
                    v                           |
Microsoft OIDC -> Authentication Service <------+
                    |
                    v
          Session Revocation Service
                    |
                    v
            HTTP Session + JWT
```

### Crear usuario

```text
Responsable -> validar jerarquía y datos -> crear PENDIENTE_ACTIVACION
    -> con correo: emitir invitación
    -> sin correo: habilitar código asistido
```

### Activar usuario

```text
Invitación o código válido -> definir contraseña -> activar cuenta
    -> consumir credencial temporal -> revocar credenciales anteriores
```

### Inicio local

```text
Username/número + contraseña -> resolver authSubject -> validar estado
    -> crear sesión y JWT con sessionVersion
```

### Inicio Microsoft

```text
Microsoft OIDC -> validar state/nonce/tenant -> resolver vínculo local
    -> validar cuenta ACTIVA -> crear sesión SIAJ
```

### Recuperación de contraseña

```text
Solicitud -> rate limit -> token de un uso -> entrega institucional
    -> nueva contraseña -> revocar sesiones y tokens previos
```

### Recuperación mediante código

```text
Responsable autorizado -> código de un uso -> entrega por canal autorizado
    -> usuario + código + contraseña nueva -> consumir código -> revocar sesiones
```

### Desactivar y revocar

```text
Autorizar acción -> SUSPENDIDA -> incrementar sessionVersion
    -> cerrar sesiones HTTP -> rechazar JWT anteriores
```

## 4. Historias de usuario

### HU-01. Invitación de usuario

Como responsable autorizado  
Quiero crear una cuenta pendiente y enviar una invitación verificable  
Para que la persona establezca su acceso de forma segura.

Criterios de aceptación:

- La cuenta queda `PENDIENTE_ACTIVACION`.
- El resultado de entrega se muestra por separado.
- La invitación vence, solo funciona una vez y se invalida al reenviarla.
- La cuenta no puede entrar antes de activarse.

### HU-02. Recuperación por código

Como persona sin acceso al correo  
Quiero utilizar un código en una pantalla visible  
Para crear una contraseña nueva.

Criterios de aceptación:

- Existe la opción “Tengo un código de recuperación”.
- El código no funciona como contraseña.
- Vence en quince minutos y tiene un solo uso.
- Al utilizarlo se invalidan sesiones anteriores.

### HU-03. Recuperación por correo

Como persona usuaria  
Quiero recuperar mi contraseña mediante un enlace  
Para restablecer mi acceso sin intervención administrativa.

Criterios de aceptación:

- La solicitud tiene límite de frecuencia.
- La indisponibilidad general del correo se comunica sin revelar cuentas.
- El token expira y se consume una vez.
- La contraseña nueva revoca accesos anteriores.

### HU-04. Inicio con Microsoft

Como persona con cuenta institucional vinculada  
Quiero iniciar sesión con Microsoft  
Para acceder usando mi identidad institucional.

Criterios de aceptación:

- El botón aparece cuando el proveedor está disponible.
- SIAJ valida tenant, respuesta OAuth y vínculo local.
- Una cuenta suspendida no puede entrar.
- La sesión se revoca al suspender la cuenta.

### HU-05. Suspensión inmediata

Como responsable autorizado  
Quiero suspender una cuenta  
Para detener inmediatamente su acceso.

Criterios de aceptación:

- Se valida jerarquía y gerencia.
- Se cierran las sesiones existentes.
- Los JWT anteriores dejan de funcionar.
- Se auditan actor, fecha, motivo y usuario afectado.

### HU-06. Administración dentro del ámbito autorizado

Como directivo o gerente autorizado  
Quiero administrar solamente usuarios dentro de mi alcance  
Para respetar la estructura institucional.

Criterios de aceptación:

- Todas las operaciones utilizan la misma política.
- No es posible operar cuentas fuera de la gerencia o jerarquía mediante URL directa.
- La matriz de roles está documentada y automatizada en pruebas.

## 5. Backlog técnico priorizado

### Sprint 1 — Bloqueantes para producción

1. Corregir la entrega institucional de correo.
2. Introducir `authSubject`.
3. Implementar estados de cuenta.
4. Separar invitaciones de recuperación.
5. Completar el flujo de código manual.
6. Implementar revocación de sesión y JWT.
7. Centralizar autorización administrativa.
8. Reactivar CSRF y eliminar cambios de estado mediante GET.
9. Crear pruebas de integración de los recorridos bloqueantes.

### Sprint 2 — Estabilización

1. Unificar recuperación pública, administrativa y asistida.
2. Completar Microsoft Login.
3. Centralizar validaciones, vigencias y mensajes.
4. Incorporar límites progresivos de intentos.
5. Crear panel de invitaciones y entregas fallidas.
6. Corregir mensajes y problemas de codificación.

### Sprint 3 — Mejoras

1. Cola de notificaciones con reintentos clasificados.
2. Métricas y alertas de autenticación.
3. Gestión visible de dispositivos y sesiones.
4. Consentimiento versionado si la normativa lo requiere.
5. Endurecimiento adicional de cookies y cabeceras.
6. Pruebas E2E en el pipeline.

## 6. Impacto sobre layouts y migración

### Antes de integrar los layouts

Deben estar definidos:

- `authSubject` y el contrato de identidad.
- Estados de cuenta.
- Política central de autorización.
- Contratos de API para alta, invitación, recuperación y sesión.
- Matriz de roles que controla la visibilidad de componentes.

### Trabajo que puede continuar en paralelo

- Sistema visual institucional.
- Componentes de formulario, alerta, modal y estado de carga.
- Pantallas de usuarios, invitaciones, recuperación y sesiones.
- Diseño responsivo y accesibilidad.
- Tablas, filtros, paginación y vistas de auditoría.
- Pantallas de carga masiva, vista previa y reporte de errores.

### Componentes reutilizables

```text
fragments/
  ui/
    form-field.html
    status-badge.html
    confirmation-modal.html
    toast.html
    empty-state.html
  users/
    user-form.html
    invitation-status.html
    recovery-code.html
```

Pueden reutilizarse la navegación, cabecera, modal y panel de usuarios, validación visual de contraseña, alertas, auditoría y catálogos de gerencias, materias y roles.

## 7. Arquitectura para sustituir la migración obsoleta

```text
XLSX/CSV
   -> carga autenticada
   -> validación estructural
   -> normalización
   -> vista previa con errores por fila
   -> aprobación
   -> procesamiento asíncrono por lotes
   -> persistencia de registros válidos
   -> reporte de errores
   -> auditoría y resumen final
```

Módulos propuestos:

```text
importacion/
  ImportJob
  ImportFile
  ImportRowError
  ImportTemplate
  ImportJobStatus
  ImportProcessor
  ImportValidationService
  ImportCommitService
```

Estados:

```text
BORRADOR
CARGADO
VALIDANDO
LISTO_PARA_APROBAR
PROCESANDO
COMPLETADO
COMPLETADO_CON_ERRORES
FALLIDO
CANCELADO
```

Reglas técnicas:

- Plantillas versionadas por entidad.
- Validación completa antes de persistir.
- Catálogos resueltos por claves estables.
- Lotes transaccionales pequeños.
- Idempotencia por identificador de carga.
- Reporte de errores por fila.
- Auditoría de actor, archivo, fecha, resultado y cambios.
- Acceso limitado por autorización administrativa.
- Endpoint de importación protegido.
- Uso de los mismos servicios de dominio que la captura individual.

## 8. Criterio de salida a producción

El módulo podrá considerarse liberable cuando:

- La entrega de correo institucional funcione con autenticación soportada.
- Las cuentas con y sin correo utilicen una identidad interna estable.
- Invitaciones, enlaces y códigos completen sus recorridos de un solo uso.
- Suspender o recuperar una cuenta revoque sesiones y JWT.
- Todos los endpoints administrativos apliquen la misma autorización.
- CSRF y métodos HTTP estén corregidos.
- Las pruebas de integración y E2E del Sprint 1 sean satisfactorias.

package com.juridico.sistema_juridico.service;

import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SessionRevocationService {

    private final SessionRegistry sessionRegistry;
    private final AuditoriaTecnicaService auditoriaTecnicaService;
    private final com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository usuarioRepository;

    public void revokeAll(Usuario usuario, String reason) {
        if (usuario == null || usuario.getEmail() == null) {
            return;
        }
        usuario.setSecurityVersion(usuario.getSecurityVersion() == null ? 1 : usuario.getSecurityVersion() + 1);
        usuarioRepository.save(usuario);

        for (Object principal : sessionRegistry.getAllPrincipals()) {
            if (!matches(principal, usuario.getEmail())) {
                continue;
            }
            for (SessionInformation session : sessionRegistry.getAllSessions(principal, false)) {
                session.expireNow();
            }
        }
        auditoriaTecnicaService.cerrarSesionesActivasUsuario(usuario, reason);
    }

    private boolean matches(Object principal, String email) {
        if (principal instanceof UserDetails details) {
            return email.equalsIgnoreCase(details.getUsername());
        }
        return email.equalsIgnoreCase(String.valueOf(principal));
    }
}

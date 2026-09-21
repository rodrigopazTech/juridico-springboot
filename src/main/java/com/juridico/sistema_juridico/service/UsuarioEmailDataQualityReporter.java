package com.juridico.sistema_juridico.service;

import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.users.report-missing-email", havingValue = "true", matchIfMissing = true)
public class UsuarioEmailDataQualityReporter implements ApplicationRunner {

    private final UsuarioRepository usuarioRepository;

    @Override
    public void run(ApplicationArguments args) {
        List<Usuario> usuarios = usuarioRepository.findUsuariosSinCorreo();
        if (usuarios.isEmpty()) {
            log.info("Validacion de usuarios: todas las cuentas tienen correo electronico.");
            return;
        }

        String identifiers = usuarios.stream()
                .map(usuario -> "id=" + usuario.getId() + ", username=" + usuario.getUsername())
                .reduce((first, next) -> first + "; " + next)
                .orElse("");
        log.warn("USUARIOS_SIN_CORREO: {} cuenta(s) requieren correccion administrativa: {}",
                usuarios.size(), identifiers);
    }
}

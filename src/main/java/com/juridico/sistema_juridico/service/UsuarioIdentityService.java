package com.juridico.sistema_juridico.service;

import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UsuarioIdentityService implements ApplicationRunner {

    private static final Map<String, String> SEED_EMPLOYEE_NUMBERS = Map.of(
            "admin@sistema.com", "E0000",
            "director.principal@juridico.com", "E0001",
            "director@juridico.com", "E0002",
            "subdirector@juridico.com", "E0003",
            "gerente@juridico.com", "E0004",
            "jefe@juridico.com", "E0005",
            "abogado@juridico.com", "E0006");

    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Set<String> usernames = new HashSet<>();
        usuarioRepository.findAll().forEach(u -> {
            if (u.getUsername() != null && !u.getUsername().isBlank()) {
                usernames.add(u.getUsername().trim().toLowerCase(Locale.ROOT));
            }
        });

        usuarioRepository.findAll().forEach(usuario -> {
            boolean updated = false;
            if (usuario.getUsername() == null || usuario.getUsername().isBlank()) {
                String base = baseUsername(usuario);
                String username = uniqueUsername(base, usernames);
                usuario.setUsername(username);
                usernames.add(username);
                updated = true;
            }
            if ((usuario.getNumeroEmpleado() == null || usuario.getNumeroEmpleado().isBlank())
                    && usuario.getEmail() != null) {
                String seedNumber = SEED_EMPLOYEE_NUMBERS.get(usuario.getEmail().trim().toLowerCase(Locale.ROOT));
                if (seedNumber != null) {
                    usuario.setNumeroEmpleado(seedNumber);
                    updated = true;
                }
            }
            if (updated) {
                usuarioRepository.save(usuario);
            }
        });
    }

    public String normalizeUsername(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return slug(value.trim()).toLowerCase(Locale.ROOT);
    }

    public String generateUsername(Usuario usuario) {
        Set<String> usernames = new HashSet<>();
        usuarioRepository.findAll().forEach(u -> {
            if (u.getUsername() != null && !u.getUsername().isBlank()
                    && (usuario.getId() == null || !u.getId().equals(usuario.getId()))) {
                usernames.add(u.getUsername().trim().toLowerCase(Locale.ROOT));
            }
        });
        return uniqueUsername(baseUsername(usuario), usernames);
    }

    public String generateUsernameFromName(Usuario usuario) {
        return generateUsername(usuario);
    }

    private String baseUsername(Usuario usuario) {
        if (usuario.getEmail() != null && usuario.getEmail().contains("@")) {
            String prefix = usuario.getEmail().substring(0, usuario.getEmail().indexOf('@')).trim();
            if (!prefix.isBlank()) {
                return normalizeUsername(prefix);
            }
        }
        return extractPrimerNombreYPrimerApellido(usuario.getNombreCompleto());
    }

    public String extractPrimerNombreYPrimerApellido(String nombreCompleto) {
        if (nombreCompleto == null || nombreCompleto.isBlank()) {
            return "usuario";
        }
        String[] tokens = nombreCompleto.trim().split("\\s+");
        if (tokens.length == 1) {
            return normalizeUsername(tokens[0]);
        }
        Set<String> preps = Set.of("de", "del", "la", "las", "los", "san", "santa");
        String firstName = normalizeUsername(tokens[0]);
        if (tokens.length == 2) {
            return firstName + "." + normalizeUsername(tokens[1]);
        }
        if (tokens.length == 3) {
            String surname = preps.contains(tokens[1].toLowerCase(Locale.ROOT))
                    ? normalizeUsername(tokens[2])
                    : normalizeUsername(tokens[1]);
            return firstName + "." + surname;
        }
        if (tokens.length == 4) {
            if (preps.contains(tokens[1].toLowerCase(Locale.ROOT))) {
                return firstName + "." + normalizeUsername(tokens[3]);
            }
            return firstName + "." + normalizeUsername(tokens[2]);
        }
        return firstName + "." + normalizeUsername(tokens[tokens.length - 2]);
    }

    private String uniqueUsername(String base, Set<String> used) {
        String candidate = base != null && !base.isBlank() ? base : "usuario";
        if (!used.contains(candidate)) {
            return candidate;
        }
        int suffix = 2;
        while (used.contains(candidate + suffix)) {
            suffix++;
        }
        return candidate + suffix;
    }

    private String slug(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        normalized = normalized.toLowerCase(Locale.ROOT)
                .replaceAll("@.*$", "")
                .replaceAll("[^a-z0-9._-]+", ".")
                .replaceAll("\\.{2,}", ".")
                .replaceAll("^\\.|\\.$", "");
        return normalized.length() > 80 ? normalized.substring(0, 80) : normalized;
    }
}

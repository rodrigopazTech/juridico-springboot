package com.juridico.sistema_juridico.security;

import com.juridico.sistema_juridico.Entity.enums.PermisoSistema;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // 1. Buscamos el usuario en tu base de datos
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con email: " + email));

        // 2. Convertimos tu Rol (Enum) a una autoridad de Spring Security
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(usuario.getRol().name()));

        if (usuario.getPermisosAdicionales() != null) {
            for (PermisoSistema permiso : usuario.getPermisosAdicionales()) {
                authorities.add(new SimpleGrantedAuthority(permiso.getAuthority()));
            }
        }

        // 3. Retornamos el objeto CustomUserDetails
        return new CustomUserDetails(
                usuario.getEmail(),
                usuario.getPassword(),
                usuario.getActivo(), // Enabled
                true, // Account Non Expired
                true, // Credentials Non Expired
                true, // Account Non Locked
                authorities, // Lista de roles y permisos adicionales
                usuario.getForcePasswordChange() != null && usuario.getForcePasswordChange() // Propiedad force password
        );
    }
}

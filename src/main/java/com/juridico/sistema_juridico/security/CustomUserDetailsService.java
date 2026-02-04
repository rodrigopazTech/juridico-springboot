package com.juridico.sistema_juridico.security;

import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

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
        GrantedAuthority authority = new SimpleGrantedAuthority(usuario.getRol().name());

        // 3. Retornamos el objeto User
        return new User(
                usuario.getEmail(),
                // CAMBIO: getPassword() en lugar de getPasswordHash()
                usuario.getPassword(),    
                usuario.getActivo(),          // Enabled
                true,                         // Account Non Expired
                true,                         // Credentials Non Expired
                true,                         // Account Non Locked
                Collections.singletonList(authority) // Lista de roles
        );
    }
}
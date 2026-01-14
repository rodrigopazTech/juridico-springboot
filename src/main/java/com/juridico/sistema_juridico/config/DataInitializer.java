package com.juridico.sistema_juridico.config;

import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.Entity.enums.RolUsuario; 
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
//este es un usuario de prueba eliminar en produccion 
@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // 1. Verificar si ya existe el admin para no duplicarlo cada vez que reinicies
        if (!usuarioRepository.existsByEmail("admin@juridico.com")) {
            
            // 2. Crear el objeto Usuario usando el Builder
            Usuario admin = Usuario.builder()
                    .nombreCompleto("Administrador del Sistema")
                    .email("admin@juridico.com")
                    .passwordHash(passwordEncoder.encode("12345")) // <--- Aquí Spring encripta por ti
                    .rol(RolUsuario.DIRECCION) // Asumo que tu Enum tiene un valor ADMIN
                    .activo(true)
                    .createdAt(LocalDateTime.now())
                    .ultimoAcceso(LocalDateTime.now())
                    .build();

            usuarioRepository.save(admin);
            
            System.out.println("Usuario ADMIN creado exitosamente: admin@juridico.com / 12345");
        } else {
            System.out.println("El usuario ADMIN ya existe, no es necesario crearlo.");
        }
    }
}
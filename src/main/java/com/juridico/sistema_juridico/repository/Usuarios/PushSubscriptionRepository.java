package com.juridico.sistema_juridico.repository.Usuarios;

import com.juridico.sistema_juridico.Entity.usuario.PushSubscription;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, Integer> {
    List<PushSubscription> findByUsuario(Usuario usuario);
    List<PushSubscription> findByUsuarioId(Integer usuarioId);
    Optional<PushSubscription> findByEndpoint(String endpoint);
    void deleteByEndpoint(String endpoint);
}

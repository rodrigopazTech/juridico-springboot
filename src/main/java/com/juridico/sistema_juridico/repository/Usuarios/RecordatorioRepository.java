package com.juridico.sistema_juridico.repository.Usuarios;

import java.util.List;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.juridico.sistema_juridico.Entity.usuario.Recordatorio;
import com.juridico.sistema_juridico.Entity.usuario.Usuario; // Importar Usuario

@Repository
public interface RecordatorioRepository extends JpaRepository<Recordatorio, Integer> {

     // Método existente
     List<Recordatorio> findByUsuarioIdAndCompletadoFalse(Integer usuarioId);

     // Busca recordatorios del usuario, que NO estén completados, ordenados por
     // fecha (más próximos primero)
     List<Recordatorio> findByUsuarioAndCompletadoFalseOrderByFechaRecordatorioAsc(Usuario usuario);

     long countByUsuarioAndCompletadoFalse(Usuario usuario);

     long countByUsuarioAndCompletadoFalseAndFechaRecordatorioBefore(Usuario usuario, LocalDate fechaRecordatorio);

     long countByUsuarioAndCompletadoFalseAndFechaRecordatorioBetween(Usuario usuario, LocalDate inicio, LocalDate fin);

     // Agrega esta línea debajo de las demás
     List<Recordatorio> findByUsuarioOrderByFechaRecordatorioDesc(Usuario usuario);

     List<Recordatorio> findByFechaRecordatorioAndCompletadoFalse(LocalDate fechaRecordatorio);

     Optional<Recordatorio> findByIdAndUsuario(Integer id, Usuario usuario);
}

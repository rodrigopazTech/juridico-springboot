package com.juridico.sistema_juridico.Entity.procesal;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "catalogos_migracion")
public class CatalogoMigracion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lote_id", nullable = false)
    private LoteMigracion lote;

    // "GERENCIA", "MATERIA", "ORGANO", "TIPO_AUDIENCIA"
    @Column(nullable = false, length = 50)
    private String tipoCatalogo;

    // Como viene en el Excel (Ej. "Juzgado 1")
    @Column(nullable = false, length = 255)
    private String valorOriginal;

    // El ID del catálogo real en la BD con el que el usuario lo emparejó
    @Column(name = "id_mapeado")
    private Long idMapeado;

    // Si el usuario decidió crear uno nuevo, guardamos el nombre final aquí
    @Column(name = "nuevo_valor", length = 255)
    private String nuevoValor;

    // PENDIENTE, MAPEADO, NUEVO, RECHAZADO
    @Column(nullable = false, length = 20)
    private String estatus = "PENDIENTE";
}
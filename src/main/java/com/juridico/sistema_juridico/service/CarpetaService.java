package com.juridico.sistema_juridico.service;

import com.juridico.sistema_juridico.Entity.documento.Carpeta;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.repository.documento.CarpetaRepository;
import com.juridico.sistema_juridico.repository.documento.DocumentoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CarpetaService {

    @Autowired
    private CarpetaRepository carpetaRepository;

    @Autowired
    private DocumentoRepository documentoRepository;

    private static final int MAX_PROFUNDIDAD = 5;

    /**
     * Crear una nueva carpeta
     */
    @Transactional
    public Carpeta crearCarpeta(UUID expedienteId, String nombre, UUID carpetaPadreId, Usuario usuario) {
        // Validar nombre
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de la carpeta no puede estar vacío");
        }

        nombre = nombre.trim();

        // Validar caracteres especiales
        if (nombre.matches(".*[/\\\\:*?\"<>|].*")) {
            throw new IllegalArgumentException("El nombre contiene caracteres no permitidos: / \\ : * ? \" < > |");
        }

        // Validar profundidad máxima
        if (carpetaPadreId != null) {
            Carpeta padre = carpetaRepository.findById(carpetaPadreId)
                    .orElseThrow(() -> new IllegalArgumentException("Carpeta padre no encontrada"));

            if (padre.getNivel() >= MAX_PROFUNDIDAD - 1) {
                throw new IllegalArgumentException("Se ha alcanzado la profundidad máxima de carpetas (5 niveles)");
            }
        }

        // Validar nombre único en el mismo nivel
        if (carpetaRepository.existsByNombreInSameLevel(expedienteId, nombre, carpetaPadreId)) {
            throw new IllegalArgumentException("Ya existe una carpeta con ese nombre en este nivel");
        }

        // Obtener carpeta padre si existe
        Carpeta carpetaPadre = null;
        if (carpetaPadreId != null) {
            carpetaPadre = carpetaRepository.findById(carpetaPadreId).orElse(null);
        }

        // Calcular orden
        int orden = 0;
        if (carpetaPadreId != null) {
            orden = (int) carpetaRepository.countByCarpetaPadreId(carpetaPadreId);
        } else {
            orden = (int) carpetaRepository.findByExpedienteIdAndCarpetaPadreIsNullOrderByOrdenAsc(expedienteId).size();
        }

        // Crear carpeta
        Carpeta carpeta = Carpeta.builder()
                .expedienteId(expedienteId)
                .nombre(nombre)
                .carpetaPadre(carpetaPadre)
                .usuarioCreador(usuario)
                .orden(orden)
                .esSistema(false)
                .build();

        return carpetaRepository.save(carpeta);
    }

    /**
     * Obtener árbol completo de carpetas de un expediente
     */
    public List<Map<String, Object>> obtenerArbolCarpetas(UUID expedienteId) {
        List<Carpeta> carpetasRaiz = carpetaRepository
                .findByExpedienteIdAndCarpetaPadreIsNullOrderByOrdenAsc(expedienteId);
        return carpetasRaiz.stream()
                .map(this::construirNodoArbol)
                .collect(Collectors.toList());
    }

    /**
     * Construir nodo del árbol recursivamente
     */
    private Map<String, Object> construirNodoArbol(Carpeta carpeta) {
        Map<String, Object> nodo = new HashMap<>();
        nodo.put("id", carpeta.getId());
        nodo.put("nombre", carpeta.getNombre());
        nodo.put("nivel", carpeta.getNivel());
        nodo.put("esSistema", carpeta.getEsSistema());
        nodo.put("orden", carpeta.getOrden());

        // Contar documentos en esta carpeta
        long cantidadDocumentos = documentoRepository.countByCarpetaId(carpeta.getId());
        nodo.put("cantidadDocumentos", cantidadDocumentos);

        // Obtener subcarpetas recursivamente
        List<Carpeta> subcarpetas = carpetaRepository.findByCarpetaPadreIdOrderByOrdenAsc(carpeta.getId());
        List<Map<String, Object>> subcarpetasNodos = subcarpetas.stream()
                .map(this::construirNodoArbol)
                .collect(Collectors.toList());
        nodo.put("subcarpetas", subcarpetasNodos);
        nodo.put("tieneSubcarpetas", !subcarpetas.isEmpty());

        return nodo;
    }

    /**
     * Obtener carpetas raíz de un expediente
     */
    public List<Carpeta> obtenerCarpetasRaiz(UUID expedienteId) {
        return carpetaRepository.findByExpedienteIdAndCarpetaPadreIsNullOrderByOrdenAsc(expedienteId);
    }

    /**
     * Obtener subcarpetas de una carpeta
     */
    public List<Carpeta> obtenerSubcarpetas(UUID carpetaId) {
        return carpetaRepository.findByCarpetaPadreIdOrderByOrdenAsc(carpetaId);
    }

    /**
     * Obtener carpeta por ID
     */
    public Carpeta obtenerCarpeta(UUID carpetaId) {
        return carpetaRepository.findById(carpetaId)
                .orElseThrow(() -> new IllegalArgumentException("Carpeta no encontrada"));
    }

    /**
     * Renombrar carpeta
     */
    @Transactional
    public Carpeta renombrarCarpeta(UUID carpetaId, String nuevoNombre) {
        Carpeta carpeta = obtenerCarpeta(carpetaId);

        if (carpeta.getEsSistema()) {
            throw new IllegalArgumentException("No se pueden renombrar carpetas del sistema");
        }

        nuevoNombre = nuevoNombre.trim();

        // Validar caracteres especiales
        if (nuevoNombre.matches(".*[/\\\\:*?\"<>|].*")) {
            throw new IllegalArgumentException("El nombre contiene caracteres no permitidos");
        }

        // Validar nombre único
        UUID carpetaPadreId = carpeta.getCarpetaPadre() != null ? carpeta.getCarpetaPadre().getId() : null;
        if (carpetaRepository.existsByNombreInSameLevel(carpeta.getExpedienteId(), nuevoNombre, carpetaPadreId)) {
            throw new IllegalArgumentException("Ya existe una carpeta con ese nombre en este nivel");
        }

        carpeta.setNombre(nuevoNombre);
        return carpetaRepository.save(carpeta);
    }

    /**
     * Eliminar carpeta (solo si está vacía)
     */
    @Transactional
    public void eliminarCarpeta(UUID carpetaId) {
        Carpeta carpeta = obtenerCarpeta(carpetaId);

        if (carpeta.getEsSistema()) {
            throw new IllegalArgumentException("No se pueden eliminar carpetas del sistema");
        }

        // Verificar que no tenga documentos
        long cantidadDocumentos = documentoRepository.countByCarpetaId(carpetaId);
        if (cantidadDocumentos > 0) {
            throw new IllegalArgumentException("No se puede eliminar una carpeta que contiene documentos");
        }

        // Verificar que no tenga subcarpetas
        if (carpetaRepository.existsByCarpetaPadreId(carpetaId)) {
            throw new IllegalArgumentException("No se puede eliminar una carpeta que contiene subcarpetas");
        }

        carpetaRepository.delete(carpeta);
    }

    /**
     * Mover carpeta a otra ubicación
     */
    @Transactional
    public Carpeta moverCarpeta(UUID carpetaId, UUID nuevaCarpetaPadreId) {
        Carpeta carpeta = obtenerCarpeta(carpetaId);

        if (carpeta.getEsSistema()) {
            throw new IllegalArgumentException("No se pueden mover carpetas del sistema");
        }

        // Validar que no se mueva a sí misma o a una de sus subcarpetas
        if (nuevaCarpetaPadreId != null) {
            if (carpetaId.equals(nuevaCarpetaPadreId)) {
                throw new IllegalArgumentException("No se puede mover una carpeta dentro de sí misma");
            }

            Carpeta nuevaPadre = obtenerCarpeta(nuevaCarpetaPadreId);

            // Verificar que no sea una subcarpeta de la carpeta a mover
            Carpeta temp = nuevaPadre;
            while (temp != null) {
                if (temp.getId().equals(carpetaId)) {
                    throw new IllegalArgumentException("No se puede mover una carpeta a una de sus subcarpetas");
                }
                temp = temp.getCarpetaPadre();
            }

            // Validar profundidad máxima
            if (nuevaPadre.getNivel() + calcularProfundidad(carpeta) >= MAX_PROFUNDIDAD) {
                throw new IllegalArgumentException("La operación excedería la profundidad máxima permitida");
            }

            carpeta.setCarpetaPadre(nuevaPadre);
        } else {
            carpeta.setCarpetaPadre(null);
        }

        return carpetaRepository.save(carpeta);
    }

    /**
     * Calcular profundidad de una carpeta y sus subcarpetas
     */
    private int calcularProfundidad(Carpeta carpeta) {
        List<Carpeta> subcarpetas = carpetaRepository.findByCarpetaPadreIdOrderByOrdenAsc(carpeta.getId());
        if (subcarpetas.isEmpty()) {
            return 1;
        }

        int maxProfundidad = 0;
        for (Carpeta sub : subcarpetas) {
            maxProfundidad = Math.max(maxProfundidad, calcularProfundidad(sub));
        }

        return maxProfundidad + 1;
    }

    /**
     * Inicializar carpetas por defecto para un expediente
     */
    @Transactional
    public void inicializarCarpetasPorDefecto(UUID expedienteId, Usuario usuario) {
        // Crear carpetas del sistema
        String[] carpetasDefecto = { "Audiencias", "Términos Legales", "Anexos y Pruebas" };

        for (int i = 0; i < carpetasDefecto.length; i++) {
            Carpeta carpeta = Carpeta.builder()
                    .expedienteId(expedienteId)
                    .nombre(carpetasDefecto[i])
                    .carpetaPadre(null)
                    .usuarioCreador(usuario)
                    .orden(i)
                    .esSistema(true)
                    .build();

            carpetaRepository.save(carpeta);
        }
    }
}

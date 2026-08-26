package com.juridico.sistema_juridico.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.juridico.sistema_juridico.exception.BadRequestException;

@Service
public class FileStorageService {

    private static final long MAX_SIZE_BYTES = 1024L * 1024 * 1024;
    private static final Set<String> EXTENSIONES_PERMITIDAS = Set.of(
            "txt",
            "pdf",
            "doc", "docx", "docm",
            "xls", "xlsx", "xlsm", "xlsb",
            "ppt", "pptx",
            "jpg", "jpeg", "png", "gif", "bmp", "webp", "tiff", "svg",
            "mp4", "avi", "mkv", "mov", "wmv", "flv", "webm", "mpeg", "3gp",
            "mp3", "wav", "ogg", "m4a", "aac", "flac", "wma");

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    public void validarArchivo(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("El archivo esta vacio.");
        }

        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new BadRequestException("El archivo excede el tamano maximo permitido de 1GB.");
        }

        String extension = obtenerExtensionSegura(file.getOriginalFilename());
        if (!EXTENSIONES_PERMITIDAS.contains(extension)) {
            throw new BadRequestException("El formato de archivo '." + extension
                    + "' no esta permitido. Solo se admiten archivos PDF, Office (Word, Excel, PowerPoint), imagenes, videos y audio.");
        }
    }

    public String guardarArchivo(MultipartFile file, UUID expedienteId, String categoria) throws IOException {
        validarArchivo(file);

        String categoriaSegura = sanitizarSegmentoRuta(categoria, "raiz");
        String rutaRelativaDirectorio = Paths.get("expedientes", expedienteId.toString(), categoriaSegura).toString();
        Path directorioExpediente = resolverRutaRelativaSegura(rutaRelativaDirectorio);
        Files.createDirectories(directorioExpediente);

        String nombreUnico = UUID.randomUUID() + "." + obtenerExtensionSegura(file.getOriginalFilename());
        Path rutaDestino = directorioExpediente.resolve(nombreUnico).normalize();
        validarDentroDeRaiz(rutaDestino);

        Files.copy(file.getInputStream(), rutaDestino, StandardCopyOption.REPLACE_EXISTING);

        return Paths.get("expedientes", expedienteId.toString(), categoriaSegura, nombreUnico).toString();
    }

    public String guardarArchivoEnRutaRelativa(MultipartFile file, String rutaRelativaBase) throws IOException {
        validarArchivo(file);

        Path base = resolverRutaRelativaSegura(rutaRelativaBase);
        Files.createDirectories(base);

        String nombreUnico = UUID.randomUUID() + "." + obtenerExtensionSegura(file.getOriginalFilename());
        Path rutaDestino = base.resolve(nombreUnico).normalize();
        validarDentroDeRaiz(rutaDestino);

        Files.copy(file.getInputStream(), rutaDestino, StandardCopyOption.REPLACE_EXISTING);

        return uploadRoot().relativize(rutaDestino).toString();
    }

    public Path obtenerArchivo(String rutaRelativa) {
        return resolverRutaRelativaSegura(rutaRelativa);
    }

    public void eliminarArchivo(String rutaRelativa) throws IOException {
        Path rutaArchivo = obtenerArchivo(rutaRelativa);
        Files.deleteIfExists(rutaArchivo);
    }

    public boolean existeArchivo(String rutaRelativa) {
        Path rutaArchivo = obtenerArchivo(rutaRelativa);
        return Files.exists(rutaArchivo);
    }

    private Path uploadRoot() {
        return Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    private Path resolverRutaRelativaSegura(String rutaRelativa) {
        if (rutaRelativa == null || rutaRelativa.isBlank()) {
            throw new BadRequestException("La ruta del archivo no es valida.");
        }

        Path relativa = Paths.get(rutaRelativa).normalize();
        if (relativa.isAbsolute() || relativa.startsWith("..") || rutaRelativa.contains("\0")) {
            throw new BadRequestException("La ruta del archivo no esta permitida.");
        }

        Path resuelta = uploadRoot().resolve(relativa).normalize();
        validarDentroDeRaiz(resuelta);
        return resuelta;
    }

    private void validarDentroDeRaiz(Path ruta) {
        if (!ruta.startsWith(uploadRoot())) {
            throw new BadRequestException("La ruta del archivo sale del directorio permitido.");
        }
    }

    private String obtenerExtensionSegura(String nombreOriginal) {
        if (nombreOriginal == null || !nombreOriginal.contains(".")) {
            throw new BadRequestException("El archivo no tiene una extension valida.");
        }

        String nombre = Paths.get(nombreOriginal).getFileName().toString();
        String extension = nombre.substring(nombre.lastIndexOf(".") + 1).toLowerCase();
        if (!extension.matches("[a-z0-9]{1,10}")) {
            throw new BadRequestException("La extension del archivo no es valida.");
        }
        return extension;
    }

    private String sanitizarSegmentoRuta(String valor, String valorPorDefecto) {
        String base = valor == null || valor.isBlank() ? valorPorDefecto : valor;
        String normalizado = Normalizer.normalize(base, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^a-zA-Z0-9._-]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^\\.+", "")
                .replaceAll("\\.+$", "");
        return normalizado.isBlank() ? valorPorDefecto : normalizado;
    }
}

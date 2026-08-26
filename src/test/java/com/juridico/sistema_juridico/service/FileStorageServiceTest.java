package com.juridico.sistema_juridico.service;

import com.juridico.sistema_juridico.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void guardarArchivoSanitizaCategoriaYPermaneceDentroDeUploadDir() throws Exception {
        FileStorageService service = serviceWithUploadDir();
        MockMultipartFile file = new MockMultipartFile(
                "file", "..\\acuse.pdf", "application/pdf", "contenido".getBytes());

        String rutaRelativa = service.guardarArchivo(file, UUID.randomUUID(), "../carpeta insegura");
        Path rutaGuardada = service.obtenerArchivo(rutaRelativa);

        assertTrue(rutaGuardada.toAbsolutePath().normalize().startsWith(tempDir.toAbsolutePath().normalize()));
        assertTrue(Files.exists(rutaGuardada));
    }

    @Test
    void obtenerArchivoRechazaTraversal() {
        FileStorageService service = serviceWithUploadDir();

        assertThrows(BadRequestException.class, () -> service.obtenerArchivo("../application.properties"));
    }

    @Test
    void obtenerArchivoRechazaRutaAbsoluta() {
        FileStorageService service = serviceWithUploadDir();

        assertThrows(BadRequestException.class, () -> service.obtenerArchivo(tempDir.resolve("x.pdf").toString()));
    }

    private FileStorageService serviceWithUploadDir() {
        FileStorageService service = new FileStorageService();
        ReflectionTestUtils.setField(service, "uploadDir", tempDir.toString());
        return service;
    }
}

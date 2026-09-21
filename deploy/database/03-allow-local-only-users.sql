-- Permite cuentas de acceso local para personal sin correo institucional.
-- PostgreSQL permite varios valores NULL en una columna UNIQUE.
ALTER TABLE usuarios
    ALTER COLUMN email DROP NOT NULL;

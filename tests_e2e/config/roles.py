"""
Matriz de Roles y Permisos (RBAC) para el Sistema Juridico SIAJ
Define qué menús, rutas y acciones corresponden a cada uno de los 7 roles del sistema.
"""

ROLES_CONFIG = {
    "ADMIN": {
        "username": "admin",
        "password": "admin123",
        "label": "Administrador General",
        "allowed_menus": [
            "Dashboard", "Expedientes", "Términos", "Audiencias", 
            "Calendario", "Mis Tareas", "Concluidos", "Catálogos", 
            "Usuarios", "Migración Inteligente", "Auditoria Tecnica"
        ],
        "hidden_menus": [],
        "allowed_routes": [
            "/dashboard", "/expedientes", "/terminos", "/audiencias", 
            "/calendario", "/tareas", "/agenda", "/catalogos", 
            "/usuarios", "/admin/importar", "/admin/auditoria"
        ],
        "forbidden_routes": []
    },
    "DIRECTOR_PRINCIPAL": {
        "username": "director.principal",
        "password": "12345",
        "label": "Director Principal",
        "allowed_menus": [
            "Dashboard", "Expedientes", "Términos", "Audiencias", 
            "Calendario", "Mis Tareas", "Concluidos", "Catálogos", "Usuarios"
        ],
        "hidden_menus": ["Migración Inteligente", "Auditoria Tecnica"],
        "allowed_routes": [
            "/dashboard", "/expedientes", "/terminos", "/audiencias", 
            "/calendario", "/tareas", "/agenda", "/catalogos", "/usuarios"
        ],
        "forbidden_routes": ["/admin/importar", "/admin/auditoria"]
    },
    "DIRECTOR": {
        "username": "director",
        "password": "12345",
        "label": "Director de Área",
        "allowed_menus": [
            "Dashboard", "Expedientes", "Términos", "Audiencias", 
            "Calendario", "Mis Tareas", "Concluidos", "Catálogos", "Usuarios"
        ],
        "hidden_menus": ["Migración Inteligente", "Auditoria Tecnica"],
        "allowed_routes": [
            "/dashboard", "/expedientes", "/terminos", "/audiencias", 
            "/calendario", "/tareas", "/agenda", "/catalogos", "/usuarios"
        ],
        "forbidden_routes": ["/admin/importar", "/admin/auditoria"]
    },
    "SUBDIRECTOR": {
        "username": "subdirector",
        "password": "12345",
        "label": "Subdirector",
        "allowed_menus": [
            "Dashboard", "Expedientes", "Términos", "Audiencias", 
            "Calendario", "Mis Tareas", "Concluidos", "Catálogos", "Usuarios"
        ],
        "hidden_menus": ["Migración Inteligente", "Auditoria Tecnica"],
        "allowed_routes": [
            "/dashboard", "/expedientes", "/terminos", "/audiencias", 
            "/calendario", "/tareas", "/agenda", "/catalogos", "/usuarios"
        ],
        "forbidden_routes": ["/admin/importar", "/admin/auditoria"]
    },
    "GERENTE": {
        "username": "gerente",
        "password": "12345",
        "label": "Gerente",
        "allowed_menus": [
            "Dashboard", "Expedientes", "Términos", "Audiencias", 
            "Calendario", "Mis Tareas", "Concluidos", "Catálogos", "Usuarios"
        ],
        "hidden_menus": ["Migración Inteligente", "Auditoria Tecnica"],
        "allowed_routes": [
            "/dashboard", "/expedientes", "/terminos", "/audiencias", 
            "/calendario", "/tareas", "/agenda", "/catalogos", "/usuarios"
        ],
        "forbidden_routes": ["/admin/importar", "/admin/auditoria"]
    },
    "JEFE_DEPTO": {
        "username": "jefe",
        "password": "12345",
        "label": "Jefe de Departamento",
        "allowed_menus": [
            "Dashboard", "Expedientes", "Términos", "Audiencias", 
            "Calendario", "Mis Tareas", "Concluidos", "Catálogos", "Usuarios"
        ],
        "hidden_menus": ["Migración Inteligente", "Auditoria Tecnica"],
        "allowed_routes": [
            "/dashboard", "/expedientes", "/terminos", "/audiencias", 
            "/calendario", "/tareas", "/agenda", "/catalogos", "/usuarios"
        ],
        "forbidden_routes": ["/admin/importar", "/admin/auditoria"]
    },
    "ABOGADO": {
        "username": "abogado",
        "password": "12345",
        "label": "Abogado Operativo",
        "allowed_menus": [
            "Dashboard", "Expedientes", "Términos", "Audiencias", 
            "Calendario", "Mis Tareas", "Concluidos", "Catálogos"
        ],
        "hidden_menus": ["Usuarios", "Migración Inteligente", "Auditoria Tecnica"],
        "allowed_routes": [
            "/dashboard", "/expedientes", "/terminos", "/audiencias", 
            "/calendario", "/tareas", "/agenda", "/catalogos"
        ],
        "forbidden_routes": ["/usuarios", "/admin/importar", "/admin/auditoria"]
    }
}

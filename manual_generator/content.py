"""
Contenido textual del Manual de Usuario SIAJ.
Todas las descripciones, introducciones y pasos fueron redactados
para el rol Abogado.
"""

INTRO_SISTEMA = """
El Sistema Integral de Asuntos Jurídicos (SIAJ) es la plataforma oficial de
Financiera para el Bienestar para la gestión, seguimiento y control de los
asuntos legales de la institución. Permite registrar expedientes, programar
audiencias, monitorear términos procesales mediante semáforos de vencimiento,
gestionar documentos y mantener una trazabilidad completa de cada caso.
""".strip()

INTRO_OBJETIVO = """
Este manual tiene como objetivo guiar al usuario con perfil Abogado en el uso
correcto del sistema, describiendo cada módulo, sus funciones y los pasos
necesarios para realizar las operaciones más comunes.
""".strip()

INTRO_ALCANCE = """
El presente documento aplica a todos los usuarios del SIAJ con rol de Abogado
asignado por el administrador del sistema. Las funcionalidades del rol
Administrador no se documentan en este manual.
""".strip()

REQUISITOS_NAVEGADOR = """
Para garantizar el correcto funcionamiento de la interfaz visual, la recepción de notificaciones en tiempo real y la seguridad en la transmisión de datos, el SIAJ requiere el uso de un navegador web moderno y actualizado.

Navegadores recomendados y versiones mínimas soportadas:
• Google Chrome: Versión 90 o superior (Navegador recomendado)
• Microsoft Edge: Versión 90 o superior (Navegador recomendado)
• Mozilla Firefox: Versión 88 o superior
• Apple Safari: Versión 14 o superior

Nota importante de compatibilidad: Se prohíbe el uso de navegadores obsoletos como Internet Explorer o versiones desactualizadas de navegadores que no soporten estándares modernos de JavaScript (ES6+), WebSockets y cifrado TLS 1.2+. El uso de navegadores obsoletos provocará errores visuales, fallos en la autenticación e imposibilidad de cargar documentos.
""".strip()

GLOSARIO = [
    ("Expediente",      "Conjunto de documentos y actuaciones que conforman un asunto jurídico."),
    ("Audiencia",       "Diligencia judicial o administrativa programada dentro de un expediente."),
    ("Término procesal","Plazo legal establecido para realizar una actuación dentro de un proceso."),
    ("Semáforo",        "Indicador visual de color que refleja el estado de vencimiento de un término."),
    ("Trazabilidad",    "Registro cronológico de todas las acciones realizadas sobre un expediente."),
    ("SIAJ",            "Sistema Integral de Asuntos Jurídicos."),
    ("Gerencia",        "Unidad organizacional a la que pertenece un expediente o usuario."),
    ("JWT",             "Token de autenticación segura que mantiene la sesión del usuario."),
]

# ─── CONTENIDO POR PANTALLA ──────────────────────────────────────────────────
# Cada clave corresponde al slug de SCREENS en config.py

SCREENS_CONTENT = {

    "00_login": {
        "titulo":      "Acceso al Sistema",
        "descripcion": (
            "La pantalla de inicio de sesión es el punto de entrada al SIAJ. "
            "El usuario debe ingresar sus credenciales institucionales utilizando un navegador "
            "web moderno compatible para acceder a los módulos autorizados según su perfil."
        ),
        "elementos": [
            ("Campo Usuario",    "Ingrese su nombre de usuario institucional asignado."),
            ("Campo Contraseña", "Ingrese su contraseña. Para garantizar la seguridad del acceso, la contraseña idónea debe cumplir con estándares de complejidad: mínimo 8 caracteres de longitud, combinando letras mayúsculas, letras minúsculas, números y al menos un carácter especial (ej. @, #, $, %, &)."),
            ("Botón Ingresar",   "Valida las credenciales e inicia la sesión en el sistema."),
            ("Enlace ¿Olvidaste tu contraseña?", "Permite solicitar el restablecimiento de contraseña al correo registrado."),
        ],
        "pasos": [
            "Abra su navegador web actualizado (Google Chrome v90+, Microsoft Edge v90+, Mozilla Firefox v88+ o Safari v14+) y acceda a la dirección: siaj.finabien.gob.mx",
            "Ingrese su nombre de usuario en el campo 'Usuario'.",
            "Ingrese su contraseña idónea en el campo 'Contraseña'.",
            "Haga clic en el botón 'Ingresar'.",
            "Si las credenciales son correctas, será redirigido al tablero principal.",
            "En caso de error, verifique sus datos o contacte al administrador del sistema.",
        ],
        "nota": (
            "Por seguridad, la sesión expira automáticamente tras 30 minutos de inactividad "
            "continua. Si esto ocurre, el sistema cerrará la sesión y lo redirigirá a la pantalla de inicio de sesión."
        ),
    },

    "01_dashboard": {
        "titulo":      "Tablero Principal (Dashboard)",
        "descripcion": (
            "El tablero principal ofrece una vista consolidada del estado general de los "
            "asuntos jurídicos asignados al abogado. Presenta indicadores clave de rendimiento "
            "(KPIs), semáforos de vencimiento y accesos directos a los módulos del sistema."
        ),
        "elementos": [
            ("Panel de KPIs",           "Muestra conteos totales de expedientes activos, audiencias próximas y términos por vencer."),
            ("Semáforos de Vencimiento","Indicadores visuales (rojo, amarillo, verde) sobre el estado de los términos procesales."),
            ("Menú lateral (Sidebar)",  "Navegación principal hacia todos los módulos accesibles por el rol Abogado."),
            ("Notificaciones",          "Campana en la barra superior que muestra alertas y avisos del sistema."),
            ("Datos del usuario",       "Esquina superior derecha: nombre, rol activo y opción de cerrar sesión."),
        ],
        "pasos": [
            "Al iniciar sesión, el sistema lo dirige automáticamente al tablero principal.",
            "Revise los KPIs superiores para obtener un resumen del estado de sus asuntos.",
            "Identifique mediante los semáforos qué expedientes o términos requieren atención inmediata.",
            "Utilice el menú lateral izquierdo para navegar a cualquier módulo del sistema.",
            "Haga clic en el ícono de campana para revisar notificaciones pendientes.",
        ],
        "nota": None,
    },

    "02_expedientes": {
        "titulo":      "Módulo de Expedientes",
        "descripcion": (
            "El módulo de Expedientes es el núcleo del SIAJ. Permite registrar, consultar "
            "y gestionar todos los asuntos jurídicos de la institución. Cada expediente "
            "agrupa audiencias, términos procesales, documentos y su trazabilidad completa."
        ),
        "elementos": [
            ("Listado de expedientes",  "Tabla paginada con todos los expedientes asignados al abogado y su gerencia."),
            ("Buscador",               "Filtro por número de expediente, parte, tipo de asunto u otros criterios."),
            ("Botón Ver detalle",      "Abre la ficha completa del expediente seleccionado."),
            ("Semáforo por expediente","Indicador de color que refleja el estado del término más crítico del expediente."),
            ("Paginación",             "Controles para navegar entre páginas cuando el listado supera los registros visibles."),
        ],
        "pasos": [
            "Haga clic en 'Expedientes' en el menú lateral izquierdo.",
            "El sistema cargará el listado de expedientes asignados a su gerencia.",
            "Use el buscador para filtrar por número, contraparte o tipo de asunto.",
            "Haga clic en el botón de detalle (ojo) del expediente que desea consultar.",
            "Dentro del detalle podrá ver audiencias, términos, documentos y trazabilidad del caso.",
        ],
        "nota": (
            "El acceso a expedientes de otras gerencias está restringido por política de "
            "seguridad. Solo verá los expedientes correspondientes a su área asignada."
        ),
    },

    "03_audiencias": {
        "titulo":      "Módulo de Audiencias",
        "descripcion": (
            "El módulo de Audiencias permite registrar y consultar todas las diligencias "
            "judiciales y administrativas programadas, facilitando el seguimiento de "
            "fechas, lugares y resultados de cada audiencia."
        ),
        "elementos": [
            ("Listado de audiencias",   "Tabla con todas las audiencias registradas, fecha, expediente relacionado y estatus."),
            ("Filtros de fecha",        "Permite acotar la búsqueda por rango de fechas de celebración."),
            ("Botón Nueva audiencia",   "Abre el formulario para registrar una nueva audiencia (según permisos)."),
            ("Indicador de estado",     "Muestra si la audiencia está: Pendiente, Celebrada o Diferida."),
            ("KPIs superiores",         "Contadores de audiencias próximas en los siguientes 7, 15 y 30 días."),
        ],
        "pasos": [
            "Haga clic en 'Audiencias' en el menú lateral izquierdo.",
            "Visualice el listado de audiencias programadas.",
            "Utilice los filtros de fecha para acotar el rango de consulta.",
            "Haga clic sobre una audiencia para ver su detalle completo.",
            "Para registrar una nueva audiencia, haga clic en el botón correspondiente y complete el formulario.",
            "Guarde los cambios con el botón 'Guardar'.",
        ],
        "nota": None,
    },

    "04_terminos": {
        "titulo":      "Módulo de Términos Procesales y Semáforos",
        "descripcion": (
            "Los términos procesales son los plazos legales dentro de los cuales deben "
            "realizarse actuaciones específicas. El SIAJ calcula automáticamente las fechas "
            "de vencimiento considerando días hábiles y feriados institucionales, y los "
            "representa mediante un sistema de semáforos de color para facilitar la "
            "identificación de prioridades."
        ),
        "elementos": [
            ("Listado de términos",       "Tabla con todos los términos activos, su fecha de inicio, vencimiento y estado."),
            ("Panel de semáforos",        "Banner informativo que explica el significado de cada color del semáforo."),
            ("Semáforo Rojo (Vencido)",   "El término ha superado su fecha límite sin concluirse."),
            ("Semáforo Amarillo (Alerta)","El término está próximo a vencer según el umbral configurado."),
            ("Semáforo Verde (Vigente)",  "El término se encuentra dentro del plazo, sin urgencia inmediata."),
            ("Semáforo Gris (Concluido)", "El término fue atendido y cerrado correctamente."),
            ("Tipo de cálculo",          "Indica si el plazo es Fijo (días naturales) o Dinámico (días hábiles)."),
        ],
        "pasos": [
            "Haga clic en 'Términos' en el menú lateral izquierdo.",
            "Revise el panel de semáforos para identificar términos críticos (rojo y amarillo).",
            "Use los filtros para acotar por expediente, tipo de término o estado del semáforo.",
            "Haga clic en un término para ver su detalle, incluyendo el cálculo de días.",
            "Los términos dinámicos excluyen sábados, domingos y días festivos registrados en el sistema.",
        ],
        "nota": (
            "Los umbrales de cada semáforo (cuántos días antes cambia de verde a amarillo, "
            "y de amarillo a rojo) son configurados por la gerencia o el administrador según "
            "las necesidades de cada área. Consulte a su supervisor si considera que los "
            "umbrales requieren ajuste."
        ),
    },

    "05_trazabilidad": {
        "titulo":      "Módulo de Trazabilidad",
        "descripcion": (
            "El módulo de Trazabilidad registra de forma cronológica e inmutable todas las "
            "acciones realizadas sobre los expedientes: creación, modificaciones, carga de "
            "documentos, cambios de estado y notas. Garantiza la auditoría y transparencia "
            "del proceso jurídico."
        ),
        "elementos": [
            ("Buscador de expediente",  "Ingrese el número de expediente para consultar su historial."),
            ("Línea de tiempo",         "Registro cronológico de todas las acciones con fecha, hora y usuario responsable."),
            ("Tipo de acción",          "Categoría del evento: Creación, Modificación, Documento, Audiencia, etc."),
            ("Usuario responsable",     "Nombre del usuario que realizó la acción registrada."),
            ("Botón Exportar",          "Permite descargar el historial de trazabilidad en formato PDF o Excel."),
        ],
        "pasos": [
            "Haga clic en 'Trazabilidad' en el menú lateral izquierdo.",
            "Ingrese el número del expediente que desea auditar.",
            "El sistema mostrará la línea de tiempo completa del expediente.",
            "Identifique la acción, fecha/hora y usuario responsable de cada evento.",
            "Para exportar el historial, haga clic en el botón 'Exportar' y seleccione el formato deseado.",
        ],
        "nota": (
            "La trazabilidad es de solo lectura. No es posible eliminar ni modificar "
            "los registros históricos. Esto garantiza la integridad del expediente."
        ),
    },

    "06_documentos": {
        "titulo":      "Gestión Documental",
        "descripcion": (
            "El gestor documental del SIAJ permite adjuntar, consultar y descargar los "
            "documentos relacionados con cada expediente. Soporta archivos en formatos PDF, "
            "Word, Excel e imágenes, con un límite máximo de 1.3 GB por archivo."
        ),
        "elementos": [
            ("Listado de documentos",   "Tabla con todos los documentos adjuntos al expediente: nombre, tipo, fecha y usuario."),
            ("Botón Subir documento",   "Abre el explorador de archivos para seleccionar y cargar un nuevo documento."),
            ("Botón Descargar",         "Descarga el documento seleccionado al equipo del usuario."),
            ("Vista previa",            "Para archivos PDF permite visualizarlos directamente en el navegador."),
            ("Filtro por tipo",         "Permite filtrar los documentos por extensión o categoría."),
        ],
        "pasos": [
            "Acceda al detalle de un expediente y localice la sección 'Documentos'.",
            "Para subir un documento, haga clic en 'Subir documento'.",
            "Seleccione el archivo desde su equipo. Los formatos permitidos son: PDF, DOC, DOCX, XLS, XLSX, JPG, PNG.",
            "Espere a que la barra de progreso complete la carga. Para archivos grandes esto puede tardar algunos segundos.",
            "El documento quedará registrado con su nombre, fecha de carga y usuario responsable.",
            "Para descargar un documento existente, haga clic en el ícono de descarga.",
        ],
        "nota": (
            "Formatos NO permitidos por seguridad: .exe, .bat, .sh, .php, .jsp. "
            "Si intenta cargar un archivo con extensión bloqueada, el sistema lo rechazará "
            "y mostrará un mensaje de error."
        ),
    },

    "07_catalogos": {
        "titulo":      "Catálogos del Sistema",
        "descripcion": (
            "El módulo de Catálogos permite consultar los valores de referencia que utiliza "
            "el sistema, como tipos de asunto, tipos de término, juzgados y configuración "
            "de semáforos. El rol Abogado tiene acceso de consulta; la edición está "
            "reservada para roles superiores."
        ),
        "elementos": [
            ("Pestañas de categoría",   "Cada pestaña agrupa un tipo de catálogo: Tipos de asunto, Tipos de término, etc."),
            ("Listado de valores",      "Tabla con los registros activos de cada catálogo."),
            ("Buscador interno",        "Filtro para localizar rápidamente un valor dentro del catálogo."),
            ("Panel de semáforos",      "Pestaña que muestra los umbrales de días configurados por gerencia."),
        ],
        "pasos": [
            "Haga clic en 'Catálogos' en el menú lateral izquierdo.",
            "Seleccione la pestaña del catálogo que desea consultar.",
            "Use el buscador para localizar un valor específico.",
            "Los catálogos son de solo lectura para el rol Abogado.",
        ],
        "nota": (
            "Si requiere agregar o modificar un valor de catálogo, contacte a su jefe "
            "de departamento o al administrador del sistema."
        ),
    },

    "08_perfil": {
        "titulo":      "Perfil de Usuario",
        "descripcion": (
            "La sección de Perfil permite al abogado consultar y actualizar sus datos "
            "personales, cambiar su contraseña y revisar la información de su cuenta "
            "registrada en el sistema."
        ),
        "elementos": [
            ("Datos personales",        "Nombre completo, correo electrónico y área/gerencia asignada."),
            ("Cambio de contraseña",    "Formulario para actualizar la contraseña actual por una nueva."),
            ("Foto de perfil",          "Opción para cargar o actualizar la fotografía del usuario (opcional)."),
            ("Rol asignado",            "Muestra el perfil de acceso con el que está registrado en el sistema."),
        ],
        "pasos": [
            "Haga clic en su nombre o avatar en la esquina superior derecha.",
            "Seleccione 'Mi perfil' del menú desplegable.",
            "Revise y actualice sus datos personales según sea necesario.",
            "Para cambiar su contraseña, complete el formulario de contraseña y haga clic en 'Guardar'.",
            "La contraseña nueva debe tener al menos 8 caracteres y combinar letras y números.",
        ],
        "nota": (
            "El nombre de usuario y el rol asignado no pueden modificarse directamente desde esta pantalla. "
            "La actualización de estos datos se gestiona a través de la estructura jerárquica del sistema: "
            "la información de un Abogado puede ser modificada por su Jefe de Departamento, la de un Jefe "
            "de Departamento por un Gerente, la de un Gerente por un Subdirector o Director, o en su defecto "
            "por el Administrador del Sistema."
        ),
    },

    "09_tareas": {
        "titulo":      "Módulo de Tareas y Recordatorios",
        "descripcion": (
            "El módulo de Tareas permite registrar, organizar y dar seguimiento a los "
            "pendientes, actividades internas y recordatorios del abogado que no forman parte "
            "directa de los términos procesales oficiales. Permite gestionar listas de tareas "
            "personales y de equipo para mantener una agenda organizada."
        ),
        "elementos": [
            ("Listado de tareas",       "Grid de tareas registradas con su título, fecha de vencimiento y prioridad."),
            ("Filtros por estado",      "Permite filtrar tareas por Pendientes, En Proceso, Completadas o Vencidas."),
            ("Botón Nueva tarea",       "Abre el formulario modal para registrar una nueva actividad o recordatorio."),
            ("Indicador de prioridad",  "Etiquetas de color para clasificar la urgencia (Alta, Media, Baja)."),
            ("Check de cumplimiento",   "Permite marcar una tarea como completada directamente desde el listado."),
        ],
        "pasos": [
            "Haga clic en 'Tareas' en el menú lateral izquierdo.",
            "Visualice las actividades y recordatorios organizados por estado de avance.",
            "Para agregar un nuevo recordatorio, haga clic en 'Nueva tarea' y complete el título, fecha y prioridad.",
            "Haga clic en 'Guardar' para registrar la tarea en el sistema.",
            "Al concluir una actividad, marque la casilla de verificación para enviarla al historial de tareas completadas.",
        ],
        "nota": (
            "Las tareas personales son visibles únicamente para el usuario creador y sus supervisores "
            "directos. A diferencia de los términos procesales, las tareas son flexibles y personalizables."
        ),
    },

    "10_usuarios": {
        "titulo":      "Anexo A.1: Módulo de Gestión de Usuarios",
        "descripcion": (
            "El módulo de Usuarios es una función de administración y supervisión que permite "
            "crear, modificar, activar y desactivar cuentas de usuario en el SIAJ, así como "
            "asignar los roles de acceso (Abogado, Jefe de Departamento, Gerente, Subdirector, "
            "Director y Administrador) y adscribirlos a su respectiva Gerencia."
        ),
        "elementos": [
            ("Listado de usuarios",     "Tabla general con nombre, correo, número de empleado, rol asignado, gerencia y estatus."),
            ("Buscador y filtros",      "Permite filtrar usuarios por nombre, rol, gerencia o estado (Activo/Inactivo)."),
            ("Botón Nuevo usuario",     "Abre el formulario de registro para dar de alta una nueva cuenta en el sistema."),
            ("Botón Editar usuario",    "Permite actualizar datos personales, restablecer contraseña o cambiar la gerencia adscrita."),
            ("Switch de Activación",    "Permite habilitar o inhabilitar el acceso al sistema de forma inmediata."),
        ],
        "pasos": [
            "Acceda al módulo 'Usuarios' desde el menú lateral (disponible para supervisores y administradores).",
            "Consulte el listado de personal adscrito a su área o institución.",
            "Para registrar un usuario, haga clic en 'Nuevo usuario', ingrese datos personales, correo, rol y gerencia asignada.",
            "Asigne una contraseña inicial temporal que cumpla con los estándares de seguridad.",
            "Para modificar permisos o cambiar de área a un usuario existente, haga clic en el ícono de edición (lápiz).",
            "Guarde los cambios para que surtan efecto en el siguiente inicio de sesión del usuario.",
        ],
        "nota": (
            "La gestión de usuarios sigue la estructura jerárquica institucional: un Jefe de Departamento "
            "administra a sus Abogados; un Gerente administra a sus Jefes; un Subdirector/Director a sus Gerentes; "
            "y el Administrador tiene control global sobre la plataforma."
        ),
    },

    "11_config_semaforos": {
        "titulo":      "Anexo A.2: Configuración Gerencial de Semáforos Procesales",
        "descripcion": (
            "La pestaña de Configuración de Semáforos dentro del módulo de Catálogos permite "
            "a los niveles gerenciales y de administración personalizar los umbrales de días "
            "de alerta (Semáforo Amarillo) y vencimiento (Semáforo Rojo) según la materia "
            "o naturaleza jurídica particular de cada área."
        ),
        "elementos": [
            ("Pestaña Semáforos",       "Acceso exclusivo dentro de Catálogos para la parametrización de alertas."),
            ("Selección de Gerencia",   "Permite seleccionar la gerencia o área jurídica cuyos umbrales se configurarán."),
            ("Umbral Amarillo (Días)",  "Número de días previos al vencimiento en que el semáforo cambia a Amarillo (Alerta)."),
            ("Umbral Rojo (Días)",      "Número de días previos al vencimiento en que el semáforo cambia a Rojo (Urgencia)."),
            ("Botón Guardar Cambios",   "Aplica la nueva parametrización a todos los términos activos de la gerencia."),
        ],
        "pasos": [
            "Acceda a 'Catálogos' desde el menú lateral e ingrese a la pestaña 'Configuración de Semáforos'.",
            "Seleccione su Gerencia en el menú desplegable.",
            "Defina el rango de días para la alerta Amarilla (ej. 5 días antes de vencer).",
            "Defina el rango de días para la alerta Roja (ej. 2 días antes de vencer).",
            "Haga clic en 'Guardar configuración'. El sistema recalculará automáticamente los colores de los términos.",
        ],
        "nota": (
            "Los cambios en los umbrales de semáforo aplican de manera inmediata para todos los abogados "
            "adscritos a la gerencia seleccionada, garantizando un criterio unificado en el control de tiempos."
        ),
    },
}

# ─── PREGUNTAS FRECUENTES ─────────────────────────────────────────────────────
FAQ = [
    (
        "¿Qué hago si olvidé mi contraseña?",
        "En la pantalla de inicio de sesión, haga clic en '¿Olvidaste tu contraseña?' e ingrese "
        "su correo institucional. Recibirá un enlace para restablecerla. Si no recibe el correo "
        "en 5 minutos, revise su carpeta de spam o contacte al administrador."
    ),
    (
        "¿Por qué no puedo ver ciertos expedientes?",
        "El acceso a expedientes está segmentado por gerencia. Solo tiene visibilidad sobre los "
        "expedientes asignados a su área. Si necesita acceder a un expediente de otra gerencia, "
        "solicítelo a su supervisor o al administrador del sistema."
    ),
    (
        "¿Qué significan los colores del semáforo?",
        "Rojo: término vencido. Amarillo: término próximo a vencer (dentro del umbral de alerta). "
        "Verde: término vigente sin urgencia. Gris: término concluido correctamente."
    ),
    (
        "¿Cómo se calculan los días de un término dinámico?",
        "El sistema cuenta únicamente días hábiles (lunes a viernes), excluyendo los días "
        "festivos registrados en el catálogo institucional. Los términos fijos cuentan días "
        "naturales sin excepciones."
    ),
    (
        "¿Puedo cargar cualquier tipo de archivo?",
        "No. El sistema acepta únicamente: PDF, DOC, DOCX, XLS, XLSX, JPG y PNG. "
        "Archivos ejecutables o de script son rechazados automáticamente por seguridad."
    ),
    (
        "¿La sesión expira automáticamente?",
        "Sí. Por seguridad, la sesión se cierra automáticamente tras 30 minutos de inactividad. "
        "Guarde su trabajo periódicamente para evitar pérdida de datos."
    ),
    (
        "¿Puedo exportar la trazabilidad de un expediente?",
        "Sí. En el módulo de Trazabilidad, utilice el botón 'Exportar' para descargar el "
        "historial completo en formato PDF o Excel."
    ),
]

# ─── DATOS DE CONTACTO ────────────────────────────────────────────────────────
CONTACTO = {
    "area":   "Coordinación de Tecnologías de la Información",
    "correo": "siaj@finabien.gob.mx",
    "sistema":"siaj.finabien.gob.mx",
}

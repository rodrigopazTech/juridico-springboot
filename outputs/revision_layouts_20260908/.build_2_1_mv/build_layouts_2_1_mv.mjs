import fs from "node:fs/promises";
import path from "node:path";
import { FileBlob, SpreadsheetFile, Workbook } from "@oai/artifact-tool";

const OUTPUT_DIR = path.resolve("..", "Layouts_2_1_MV");
const PREVIEW_DIR = path.resolve("previews");
const FONT = "Aptos";
const COLORS = {
  navy: "#17365D",
  blue: "#1F4E78",
  teal: "#0F6B78",
  lightBlue: "#D9EAF7",
  input: "#FFF2CC",
  border: "#B4C6E7",
  gray: "#F2F2F2",
  white: "#FFFFFF",
  text: "#1F2937",
};

const controlFields = [
  ["layout_version", "Texto", 10, "Sí", "Valor fijo 2.1-MV.", "2.1-MV", "2.1-MV", "El valor debe ser exactamente '2.1-MV'."],
  ["fuente_codigo", "Texto", 50, "Sí", "Fuente registrada y estable entre lotes.", "Código registrado", "FUENTE_LEGADO_01", "La fuente '<valor>' no está registrada."],
  ["lote_clave", "Texto", 100, "Sí", "Única para la entrega; no identifica registros.", "Clave de lote", "CORTE_2026_09_10_01", "La clave de lote '<valor>' ya fue utilizada con contenido diferente."],
  ["catalogo_version", "Texto", 50, "Sí", "Debe coincidir con la versión publicada.", "Versión publicada", "CAT-2026-09", "La versión de catálogo '<valor>' no está publicada o no coincide con el libro."],
  ["fecha_corte", "Fecha", "No aplica", "Sí", "Fecha real de Excel.", "Fecha válida", "2026-09-10", "El valor debe ser una fecha real de Excel."],
  ["responsable_entrega", "Texto", 200, "Sí", "No vacío.", "Texto", "María López Hernández", "El valor es obligatorio."],
  ["email_responsable", "Texto", 100, "Sí", "Correo válido; sólo contacto.", "Correo válido", "maria.lopez@cliente.mx", "El valor debe ser un correo electrónico válido."],
];

const fields = {
  GERENCIAS: [
    ["gerencia_codigo", "Texto", 100, "Sí", "Único y estable.", "Código aprobado", "GER_001", "El código de gerencia '<valor>' está duplicado o ya existe."],
    ["nombre", "Texto", 200, "Sí", "No vacío; unicidad del destino.", "Texto", "Gerencia Jurídica Laboral", "El nombre de gerencia '<valor>' viola la regla de unicidad del destino."],
    ["descripcion", "Texto", 32767, "No", "No truncar.", "Texto o vacío", "Atención de asuntos laborales.", "La longitud máxima permitida es 32767 caracteres."],
    ["activo", "Booleano", "No aplica", "Sí", "Sólo true/false.", "true; false", "true", "El valor debe ser true o false."],
  ],
  MATERIAS: [
    ["materia_codigo", "Texto", 100, "Sí", "Único en el ámbito aprobado.", "Código aprobado", "MAT_001", "El código de materia '<valor>' está duplicado en su ámbito."],
    ["gerencia_codigo", "Texto", 100, "Sí", "Existe en GERENCIAS.", "Código GERENCIAS", "GER_001", "La gerencia '<valor>' no existe en GERENCIAS."],
    ["nombre", "Texto", 100, "Sí", "No vacío; unicidad del destino.", "Texto", "Laboral", "El nombre de materia '<valor>' viola la regla de unicidad del destino."],
    ["descripcion", "Texto", 32767, "No", "No truncar.", "Texto o vacío", "Materia laboral.", "La longitud máxima permitida es 32767 caracteres."],
    ["activo", "Booleano", "No aplica", "Sí", "Sólo true/false.", "true; false", "true", "El valor debe ser true o false."],
  ],
  SEDES: [
    ["sede_codigo", "Texto", 100, "Sí", "Único.", "Código aprobado", "SEDE_001", "El código de sede '<valor>' está duplicado o ya existe."],
    ["nombre", "Texto", 100, "Sí", "Unicidad del destino.", "Texto", "Sede Central", "El nombre de sede '<valor>' viola la regla de unicidad del destino."],
    ["gerencia_codigo", "Texto", 100, "No", "Vacío significa global; si existe, referencia GERENCIAS.", "Código GERENCIAS o vacío", "GER_001", "La gerencia '<valor>' no existe en GERENCIAS."],
    ["activo", "Booleano", "No aplica", "Sí", "Sólo true/false.", "true; false", "true", "El valor debe ser true o false."],
  ],
  PRIORIDADES: [
    ["prioridad_codigo", "Texto", 50, "Sí", "Único; valores V1 aprobados.", "ALTA; MEDIA; BAJA", "ALTA", "La prioridad '<valor>' no está permitida para V1."],
    ["nombre", "Texto", 50, "Sí", "No vacío.", "Nombre publicado", "Alta", "El valor es obligatorio."],
    ["color", "Texto", 100, "No", "Valor técnico suministrado por implementación.", "Valor publicado o vacío", "", "El color de prioridad no coincide con el valor técnico publicado."],
    ["activo", "Booleano", "No aplica", "Sí", "Sólo true/false.", "true; false", "true", "El valor debe ser true o false."],
  ],
  TIPOS_EXPEDIENTE: [
    ["tipo_expediente_codigo", "Texto", 100, "Sí", "Único y estable.", "Código aprobado", "TIPO_EXP_001", "El código de tipo de expediente '<valor>' está duplicado o ya existe."],
    ["nombre", "Texto", 100, "Sí", "Unicidad del destino.", "Texto", "Tipo de expediente aprobado", "El nombre de tipo de expediente '<valor>' viola la regla de unicidad del destino."],
    ["descripcion", "Texto", 32767, "No", "Distinguir de materia y etapa; no truncar.", "Texto o vacío", "Clasificación aprobada del expediente.", "La longitud máxima permitida es 32767 caracteres."],
    ["gerencia_codigo", "Texto", 100, "No", "Vacío global; si existe, referencia GERENCIAS.", "Código GERENCIAS o vacío", "GER_001", "La gerencia '<valor>' no existe en GERENCIAS."],
    ["activo", "Booleano", "No aplica", "Sí", "Sólo true/false.", "true; false", "true", "El valor debe ser true o false."],
  ],
  ORGANOS: [
    ["organo_codigo", "Texto", 100, "Sí", "Único y estable.", "Código aprobado", "ORG_001", "El código de órgano '<valor>' está duplicado o ya existe."],
    ["gerencia_codigo", "Texto", 100, "No", "Vacío global; si existe, referencia GERENCIAS.", "Código GERENCIAS o vacío", "GER_001", "La gerencia '<valor>' no existe en GERENCIAS."],
    ["nombre", "Texto", 300, "Sí", "No vacío.", "Texto", "Órgano jurisdiccional de ejemplo", "El valor es obligatorio."],
    ["sede_codigo", "Texto", 100, "No", "Si existe, referencia SEDES.", "Código SEDES o vacío", "SEDE_001", "La sede '<valor>' no existe en SEDES."],
    ["tipo", "Texto", 50, "No", "Lista controlada publicada.", "Valor publicado o vacío", "", "El tipo de órgano '<valor>' no pertenece a la lista publicada."],
    ["activo", "Booleano", "No aplica", "Sí", "Sólo true/false.", "true; false", "true", "El valor debe ser true o false."],
  ],
  TIPOS_AUDIENCIA: [
    ["tipo_audiencia_codigo", "Texto", 100, "Sí", "Único conforme al destino.", "Código aprobado", "AUD_001", "El código de tipo de audiencia '<valor>' está duplicado o ya existe."],
    ["gerencia_codigo", "Texto", 100, "Sí", "Referencia GERENCIAS.", "Código GERENCIAS", "GER_001", "La gerencia '<valor>' no existe en GERENCIAS."],
    ["nombre", "Texto", 100, "Sí", "No vacío.", "Texto", "Tipo de audiencia aprobado", "El valor es obligatorio."],
    ["descripcion", "Texto", 32767, "No", "No truncar.", "Texto o vacío", "Descripción del tipo de audiencia.", "La longitud máxima permitida es 32767 caracteres."],
    ["activo", "Booleano", "No aplica", "Sí", "Sólo true/false.", "true; false", "true", "El valor debe ser true o false."],
  ],
  TIPOS_TERMINO: [
    ["tipo_termino_codigo", "Texto", 100, "Sí", "Único y estable.", "Código aprobado", "TERM_001", "El código de tipo de término '<valor>' está duplicado o ya existe."],
    ["nombre", "Texto", 100, "Sí", "Unicidad del destino.", "Texto", "Tipo de término aprobado", "El nombre de tipo de término '<valor>' viola la regla de unicidad del destino."],
    ["descripcion", "Texto", 32767, "No", "Distinguir del asunto; no truncar.", "Texto o vacío", "Regla aplicable al tipo de término.", "La longitud máxima permitida es 32767 caracteres."],
    ["dias_predeterminados", "Entero", "No aplica", "Sí", "Mayor que cero; informativo en migración.", "Entero > 0", 5, "El valor debe ser un entero mayor que cero."],
    ["tipo_dias", "Texto", 20, "Sí", "Lista controlada; no recalcula históricos.", "HABILES; NATURALES", "HABILES", "El valor debe ser HABILES o NATURALES."],
    ["semaforo_rojo_dias", "Entero", "No aplica", "Sí", "Mayor o igual a cero.", "Entero >= 0", 1, "El valor debe ser un entero mayor o igual a cero."],
    ["semaforo_amarillo_dias", "Entero", "No aplica", "Sí", "Mayor que semaforo_rojo_dias.", "Entero > rojo", 3, "El valor debe ser mayor que semaforo_rojo_dias."],
    ["materia_codigo", "Texto", 100, "No", "Si existe, referencia MATERIAS y es compatible con gerencia.", "Código MATERIAS o vacío", "MAT_001", "La materia '<valor>' no existe o no es compatible con la gerencia indicada."],
    ["gerencia_codigo", "Texto", 100, "No", "Si existe, referencia GERENCIAS.", "Código GERENCIAS o vacío", "GER_001", "La gerencia '<valor>' no existe en GERENCIAS."],
    ["activo", "Booleano", "No aplica", "Sí", "Sólo true/false.", "true; false", "true", "El valor debe ser true o false."],
  ],
  USUARIOS: [
    ["usuario_clave_origen", "Texto", 100, "Sí", "Única por fuente.", "Clave de origen", "USR_0001", "La clave de usuario '<valor>' está duplicada para la fuente."],
    ["operacion", "Texto", 15, "Sí", "ALTA o ACTUALIZACION.", "ALTA; ACTUALIZACION", "ALTA", "El valor debe ser ALTA o ACTUALIZACION."],
    ["gerencia_codigo", "Texto", 100, "Condicional", "Obligatoria para GERENTE, JEFE_DEPTO y ABOGADO.", "Código GERENCIAS", "GER_001", "La gerencia es obligatoria para el rol '<rol>'."],
    ["nombre_completo", "Texto", 200, "Sí", "No vacío.", "Texto", "Ana Pérez García", "El valor es obligatorio."],
    ["email", "Texto", 100, "No", "Correo válido y único si existe; no es llave.", "Correo o vacío", "ana.perez@cliente.mx", "El correo electrónico no tiene un formato válido."],
    ["username", "Texto", 80, "Condicional", "Obligatorio sin login externo; único.", "Texto o vacío", "aperez", "El username es obligatorio cuando no existe login externo."],
    ["numero_empleado", "Texto", 40, "No", "Único si existe; preservar ceros.", "Texto o vacío", "000184", "El número de empleado '<valor>' ya está asignado."],
    ["rol", "Texto", 50, "Sí", "Rol migrable; ADMIN excluido.", "Roles publicados", "ABOGADO", "El rol '<valor>' no es migrable."],
    ["puesto", "Texto", 160, "No", "Texto libre.", "Texto o vacío", "Abogada responsable", "La longitud máxima permitida es 160 caracteres."],
    ["telefono_institucional", "Texto", 120, "No", "Texto libre.", "Texto o vacío", "55 0000 0000 ext. 123", "La longitud máxima permitida es 120 caracteres."],
    ["telefono_movil", "Texto", 80, "No", "Texto libre.", "Texto o vacío", "55 0000 0000", "La longitud máxima permitida es 80 caracteres."],
    ["ubicacion_oficina", "Texto", 180, "No", "No confundir con sede jurisdiccional.", "Texto o vacío", "Edificio Central, piso 4", "La longitud máxima permitida es 180 caracteres."],
    ["materias_codigos", "Texto", 1000, "No", "Códigos separados por punto y coma; compatibles con gerencia.", "Códigos MATERIAS", "MAT_001;MAT_002", "La materia '<código>' no existe, está inactiva o no corresponde a la gerencia del usuario."],
    ["permisos_adicionales", "Texto", 1000, "No", "Sólo extras no heredados; códigos otorgables.", "Códigos publicados", "PERMISO_01;PERMISO_02", "El permiso '<código>' no existe o no puede otorgarse como permiso adicional."],
    ["activo", "Booleano", "No aplica", "Sí", "Sólo true/false.", "true; false", "true", "El valor debe ser true o false."],
  ],
  EXPEDIENTES: [
    ["clave_origen", "Texto", 100, "Sí", "Única por fuente/entidad.", "Clave de origen", "EXP_000001", "La clave de origen '<valor>' está duplicada para EXPEDIENTE y la fuente."],
    ["operacion", "Texto", 15, "Sí", "ALTA o ACTUALIZACION.", "ALTA; ACTUALIZACION", "ALTA", "El valor debe ser ALTA o ACTUALIZACION."],
    ["gerencia_codigo", "Texto", 100, "Sí", "Existe y es compatible con materia/responsable.", "Código GERENCIAS", "GER_001", "La gerencia '<valor>' no existe, está inactiva o no es compatible."],
    ["numero_expediente", "Texto", 50, "Sí", "Número oficial; no es llave única.", "Texto", "123/2024", "El número de expediente es obligatorio."],
    ["materia_codigo", "Texto", 100, "Sí", "Existe y pertenece a gerencia.", "Código MATERIAS", "MAT_001", "La materia '<valor>' no existe, está inactiva o no pertenece a la gerencia."],
    ["descripcion", "Texto", 32767, "Sí", "No vacía ni truncada.", "Texto", "Descripción histórica íntegra.", "La longitud máxima permitida es 32767 caracteres."],
    ["etapa_procesal", "Texto", 30, "Sí", "Lista controlada.", "TRAMITE; LAUDO; FIRME; CONCLUIDO", "TRAMITE", "La etapa procesal '<valor>' no está permitida."],
    ["tipo_expediente_codigo", "Texto", 100, "Sí", "Existe, activo y aplicable.", "Código TIPOS_EXPEDIENTE", "TIPO_EXP_001", "El tipo de expediente '<valor>' no existe, está inactivo o no aplica."],
    ["prioridad_codigo", "Texto", 50, "No", "Existe y activo.", "Código PRIORIDADES o vacío", "ALTA", "La prioridad '<valor>' no existe o está inactiva."],
    ["organo_codigo", "Texto", 100, "No", "Existe; no resolver por similitud.", "Código ORGANOS o vacío", "ORG_001", "El órgano '<valor>' no existe."],
    ["sede_codigo", "Texto", 100, "No", "Existe; coincide con sede del órgano.", "Código SEDES o vacío", "SEDE_001", "La sede '<valor>' no existe o no coincide con el órgano."],
    ["partes", "Texto", 32767, "No", "Conservar íntegro como texto.", "Texto o vacío", "Actor: Persona A; Demandado: Institución B", "La longitud máxima permitida es 32767 caracteres."],
    ["abogado_responsable_clave_origen", "Texto", 100, "Condicional", "Usuario existente y con alcance.", "Clave USUARIOS o vacío", "USR_0001", "El usuario '<valor>' no existe o no tiene alcance."],
  ],
  AUDIENCIAS: [
    ["clave_origen", "Texto", 100, "Sí", "Única por fuente/entidad.", "Clave de origen", "AUD_EVT_0001", "La clave de origen '<valor>' está duplicada para AUDIENCIA y la fuente."],
    ["operacion", "Texto", 15, "Sí", "ALTA o ACTUALIZACION.", "ALTA; ACTUALIZACION", "ALTA", "El valor debe ser ALTA o ACTUALIZACION."],
    ["gerencia_codigo", "Texto", 100, "Sí", "Coincide con expediente.", "Código GERENCIAS", "GER_001", "La gerencia '<valor>' no coincide con la gerencia del expediente."],
    ["expediente_clave_origen", "Texto", 100, "Sí", "Existe en equivalencias.", "Clave EXPEDIENTES", "EXP_000001", "No existe equivalencia de expediente para la clave '<valor>'."],
    ["tipo_audiencia_codigo", "Texto", 100, "Sí", "Existe y aplica a gerencia.", "Código TIPOS_AUDIENCIA", "AUD_001", "El tipo de audiencia '<valor>' no existe, está inactivo o no aplica."],
    ["fecha_audiencia", "Fecha", "No aplica", "Sí", "Fecha real; no determina estado.", "Fecha válida", "2026-10-15", "El valor debe ser una fecha real de Excel."],
    ["hora_audiencia", "Hora", "No aplica", "Sí", "Hora real hh:mm.", "Hora válida", "10:30", "El valor debe ser una hora real de Excel en formato hh:mm."],
    ["estatus_audiencia", "Texto", 30, "Sí", "Lista controlada.", "PENDIENTE; CON_ACTA; CONCLUIDA", "PENDIENTE", "El estatus de audiencia '<valor>' no está permitido."],
    ["sala_lugar", "Texto", 500, "No", "Recinto, no órgano.", "Texto o vacío", "Sala 3, planta baja", "La longitud máxima permitida es 500 caracteres."],
    ["es_virtual", "Booleano", "No aplica", "Sí", "Sólo true/false.", "true; false", "false", "El valor debe ser true o false."],
    ["url_reunion", "Texto", 2048, "Condicional", "Obligatoria para audiencia futura virtual; URL válida.", "URL o vacío", "https://reunion.ejemplo.mx/sala/123", "La URL de reunión es obligatoria para una audiencia virtual futura."],
    ["abogado_compareciente_clave_origen", "Texto", 100, "No", "Usuario existente; puede estar inactivo para historia.", "Clave USUARIOS o vacío", "USR_0001", "No existe un usuario para la clave de compareciente '<valor>'."],
    ["usuario_delegado_clave_origen", "Texto", 100, "No", "Usuario existente y con alcance si sigue activa.", "Clave USUARIOS o vacío", "USR_0002", "El usuario delegado está inactivo o no tiene alcance."],
    ["fecha_desahogo", "Fecha", "No aplica", "Condicional", "Obligatoria para CONCLUIDA; prohibida para PENDIENTE.", "Fecha o vacío", "2026-09-01", "La fecha de desahogo es obligatoria para una audiencia CONCLUIDA."],
    ["estado_acta_codigo", "Texto", 30, "Sí", "Lista controlada.", "Estados de documento", "RECIBIDO", "El estado de acta '<valor>' no está permitido."],
    ["motivo_falta_acta_codigo", "Texto", 50, "Condicional", "Obligatorio para NO_DISPONIBLE o NO_COMPARTIDO.", "Motivos de falta", "CLIENTE_NO_COMPARTE", "El motivo de falta de acta es obligatorio para el estado indicado."],
    ["acta_archivo", "Texto", 500, "Condicional", "Obligatorio si RECIBIDO; ruta relativa existente.", "Ruta o vacío", "audiencias/AUD_EVT_0001/acta.pdf", "El archivo de acta es obligatorio cuando el estado es RECIBIDO."],
    ["observaciones", "Texto", 32767, "No", "No truncar.", "Texto o vacío", "Registro histórico validado.", "La longitud máxima permitida es 32767 caracteres."],
  ],
  TERMINOS: [
    ["clave_origen", "Texto", 100, "Sí", "Única por fuente/entidad.", "Clave de origen", "TER_EVT_0001", "La clave de origen '<valor>' está duplicada para TERMINO y la fuente."],
    ["operacion", "Texto", 15, "Sí", "ALTA o ACTUALIZACION.", "ALTA; ACTUALIZACION", "ALTA", "El valor debe ser ALTA o ACTUALIZACION."],
    ["gerencia_codigo", "Texto", 100, "Sí", "Coincide con expediente.", "Código GERENCIAS", "GER_001", "La gerencia '<valor>' no coincide con la gerencia del expediente."],
    ["expediente_clave_origen", "Texto", 100, "Sí", "Existe en equivalencias.", "Clave EXPEDIENTES", "EXP_000001", "No existe equivalencia de expediente para la clave '<valor>'."],
    ["tipo_termino_codigo", "Texto", 100, "Sí", "Existe y aplica a materia/gerencia.", "Código TIPOS_TERMINO", "TERM_001", "El tipo de término '<valor>' no existe, está inactivo o no aplica."],
    ["asunto", "Texto", 32767, "Sí", "Actuación concreta; no es el tipo.", "Texto", "Presentar contestación dentro del plazo.", "El valor es obligatorio."],
    ["fecha_vencimiento", "Fecha", "No aplica", "Sí", "Fecha histórica; no recalcular.", "Fecha válida", "2026-09-30", "El valor debe ser una fecha real de Excel."],
    ["fecha_ingreso", "Fecha", "No aplica", "No", "Fecha histórica; no usar fecha de carga.", "Fecha o vacío", "2026-09-20", "El valor debe ser una fecha real de Excel o quedar vacío."],
    ["estatus_termino", "Texto", 30, "Sí", "Lista controlada.", "Estatus de término", "PROYECTISTA", "El estatus de término '<valor>' no está permitido."],
    ["prioridad_codigo", "Texto", 50, "No", "Existe y activo.", "Código PRIORIDADES o vacío", "ALTA", "La prioridad '<valor>' no existe o está inactiva."],
    ["abogado_responsable_clave_origen", "Texto", 100, "Condicional", "Obligatorio para términos abiertos.", "Clave USUARIOS", "USR_0001", "El abogado responsable es obligatorio para un término abierto."],
    ["fecha_presentacion", "Fecha", "No aplica", "Condicional", "Obligatoria para PRESENTADO; condicionada para CONCLUIDO.", "Fecha o vacío", "2026-09-28", "La fecha de presentación es obligatoria para un término PRESENTADO."],
    ["estado_acuse_codigo", "Texto", 30, "Sí", "Lista controlada.", "Estados de documento", "RECIBIDO", "El estado de acuse '<valor>' no está permitido."],
    ["motivo_falta_acuse_codigo", "Texto", 50, "Condicional", "Obligatorio para NO_DISPONIBLE o NO_COMPARTIDO.", "Motivos de falta", "NO_LOCALIZADO", "El motivo de falta de acuse es obligatorio para el estado indicado."],
    ["acuse_archivo", "Texto", 500, "Condicional", "Obligatorio si RECIBIDO; ruta relativa existente.", "Ruta o vacío", "terminos/TER_EVT_0001/acuse.pdf", "El archivo de acuse es obligatorio cuando el estado es RECIBIDO."],
    ["observaciones", "Texto", 32767, "No", "No truncar; requerida para CONCLUIDO sin presentación.", "Texto o vacío", "Concluido por causa distinta de presentación.", "Las observaciones son obligatorias para un término CONCLUIDO sin fecha de presentación."],
  ],
  DOCUMENTOS_EXPEDIENTE: [
    ["clave_origen", "Texto", 100, "Sí", "Única por fuente para DOCUMENTO_EXPEDIENTE.", "Clave de origen", "DOC_EXP_000001", "La clave de origen '<valor>' está duplicada para DOCUMENTO_EXPEDIENTE y la fuente."],
    ["operacion", "Texto", 15, "Sí", "ALTA o ACTUALIZACION.", "ALTA; ACTUALIZACION", "ALTA", "El valor debe ser ALTA o ACTUALIZACION."],
    ["expediente_clave_origen", "Texto", 100, "Sí", "Existe en equivalencias de Expedientes para la misma fuente.", "Clave EXPEDIENTES", "EXP_000001", "No existe equivalencia de expediente para la clave '<valor>' y la fuente del lote."],
    ["documento_archivo", "Texto", 500, "Sí", "Ruta relativa; archivo existente, no vacío y recuperable; nombre final <=255.", "Ruta relativa", "documentos/EXP_000001/demanda_inicial.pdf", "El archivo no existe, está vacío, no puede recuperarse o su nombre supera 255 caracteres."],
    ["descripcion", "Texto", 32767, "No", "Conservar íntegra; no truncar.", "Texto o vacío", "Demanda inicial histórica del expediente.", "La longitud máxima permitida es 32767 caracteres."],
  ],
};

const fixedLists = {
  OPERACIONES_CARGA: ["ALTA", "ACTUALIZACION"],
  BOOLEANOS: ["true", "false"],
  ROLES_MIGRABLES: ["DIRECTOR", "SUBDIRECTOR", "GERENTE", "JEFE_DEPTO", "ABOGADO", "DIRECTOR_PRINCIPAL"],
  ETAPAS_PROCESALES: ["TRAMITE", "LAUDO", "FIRME", "CONCLUIDO"],
  ESTATUS_AUDIENCIA: ["PENDIENTE", "CON_ACTA", "CONCLUIDA"],
  ESTATUS_TERMINO: ["PROYECTISTA", "REVISION", "GERENCIA", "DIRECCION", "LIBERADO", "PRESENTADO", "CONCLUIDO"],
  ESTADOS_DOCUMENTO: ["RECIBIDO", "PENDIENTE_ENTREGA", "NO_DISPONIBLE", "NO_COMPARTIDO", "NO_APLICA"],
  MOTIVOS_FALTA_DOCUMENTO: ["NO_LOCALIZADO", "EXTRAVIADO", "NO_GENERADO", "CLIENTE_NO_COMPARTE", "OTRO"],
  TIPOS_DIAS: ["HABILES", "NATURALES"],
  PRIORIDADES_V1: ["ALTA", "MEDIA", "BAJA"],
};

const layouts = [
  {
    file: "Layout_Catalogos_V2_MV.xlsx",
    title: "Catálogos de migración 2.1-MV",
    dataSheets: ["GERENCIAS", "MATERIAS", "SEDES", "PRIORIDADES", "TIPOS_EXPEDIENTE", "ORGANOS", "TIPOS_AUDIENCIA", "TIPOS_TERMINO"],
    instructions: [
      "Complete primero CONTROL.",
      "Capture GERENCIAS antes de MATERIAS y SEDES antes de ORGANOS.",
      "Use códigos estables; no cambie nombres de hojas ni columnas.",
      "Publique el libro completo antes de capturar Usuarios.",
      "No incluya SIN_CLASIFICAR sin aprobación del cliente.",
    ],
  },
  { file: "Layout_Usuarios_V2_MV.xlsx", title: "Usuarios de migración 2.1-MV", dataSheets: ["CAPTURA"], fieldKey: "USUARIOS", instructions: ["Complete CONTROL.", "Use una sola gerencia por usuario.", "Use ALTA o ACTUALIZACION.", "No capture contraseñas, tokens ni códigos de recuperación.", "Use sólo permisos adicionales no heredados por el rol."] },
  { file: "Layout_Expedientes_V2_MV.xlsx", title: "Expedientes de migración 2.1-MV", dataSheets: ["CAPTURA"], fieldKey: "EXPEDIENTES", instructions: ["Complete CONTROL.", "Cargue Catálogos y Usuarios antes de Expedientes.", "Use clave_origen como identidad; numero_expediente no es llave única.", "No trunque descripción ni partes.", "Use referencias exactas de catálogos y usuarios."] },
  { file: "Layout_Audiencias_V2_MV.xlsx", title: "Audiencias de migración 2.1-MV", dataSheets: ["CAPTURA"], fieldKey: "AUDIENCIAS", instructions: ["Complete CONTROL.", "Cargue Expedientes antes de Audiencias.", "Cada audiencia admite un Acta principal.", "Use rutas relativas para acta_archivo.", "No infiera el estatus a partir de la fecha."] },
  { file: "Layout_Terminos_V2_MV.xlsx", title: "Términos de migración 2.1-MV", dataSheets: ["CAPTURA"], fieldKey: "TERMINOS", instructions: ["Complete CONTROL.", "Cargue Expedientes antes de Términos.", "Cada término admite un Acuse principal.", "Conserve fecha_vencimiento sin recalcular.", "Use rutas relativas para acuse_archivo."] },
  { file: "Layout_Documentos_Expediente_V2_MV.xlsx", title: "Documentos de Expediente 2.1-MV", dataSheets: ["CAPTURA"], fieldKey: "DOCUMENTOS_EXPEDIENTE", instructions: ["Complete CONTROL.", "Cargue Expedientes antes de Documentos de Expediente.", "Cada fila representa un archivo adicional.", "Use una clave_origen distinta por documento y una ruta relativa existente.", "Los documentos se asociarán directamente al expediente y quedarán en la raíz del Gestor Documental."] },
];

function columnLetter(index) {
  let n = index + 1;
  let s = "";
  while (n > 0) {
    const r = (n - 1) % 26;
    s = String.fromCharCode(65 + r) + s;
    n = Math.floor((n - 1) / 26);
  }
  return s;
}

function styleHeader(range) {
  range.format = {
    fill: COLORS.blue,
    font: { name: FONT, bold: true, color: COLORS.white, size: 10 },
    wrapText: true,
    verticalAlignment: "center",
    horizontalAlignment: "center",
    borders: { preset: "all", style: "thin", color: COLORS.border },
  };
  range.format.rowHeight = 32;
}

function styleBody(range) {
  range.format = {
    font: { name: FONT, color: COLORS.text, size: 10 },
    verticalAlignment: "top",
    borders: { preset: "all", style: "thin", color: "#D9E2F3" },
  };
}

function addTableSheet(workbook, sheetName, fieldRows, tableName) {
  const sheet = workbook.worksheets.add(sheetName);
  sheet.showGridLines = false;
  sheet.tabColor = COLORS.teal;
  const headers = fieldRows.map((r) => r[0]);
  const last = columnLetter(headers.length - 1);
  sheet.getRange(`A1:${last}2`).values = [headers, headers.map(() => null)];
  styleHeader(sheet.getRange(`A1:${last}1`));
  styleBody(sheet.getRange(`A2:${last}2`));
  sheet.getRange(`A2:${last}2`).format.fill = COLORS.input;
  sheet.freezePanes.freezeRows(1);
  const table = sheet.tables.add(`A1:${last}2`, true, tableName);
  table.style = "TableStyleMedium2";
  table.showFilterButton = true;
  headers.forEach((header, i) => {
    const field = fieldRows[i];
    const width = ["descripcion", "partes", "asunto", "observaciones"].includes(header) ? 42
      : header.includes("archivo") || header === "url_reunion" ? 38
      : Math.min(30, Math.max(15, header.length + 3));
    sheet.getRange(`${columnLetter(i)}:${columnLetter(i)}`).format.columnWidth = width;
    if (field[1] === "Texto") sheet.getRange(`${columnLetter(i)}2:${columnLetter(i)}5000`).format.numberFormat = "@";
    if (field[1] === "Fecha") sheet.getRange(`${columnLetter(i)}2:${columnLetter(i)}5000`).format.numberFormat = "yyyy-mm-dd";
    if (field[1] === "Hora") sheet.getRange(`${columnLetter(i)}2:${columnLetter(i)}5000`).format.numberFormat = "hh:mm";
  });
  applyValidations(sheet, headers, sheetName);
  return sheet;
}

function setListValidation(sheet, headers, field, values, message) {
  const idx = headers.indexOf(field);
  if (idx < 0) return;
  sheet.getRange(`${columnLetter(idx)}2:${columnLetter(idx)}5000`).dataValidation = {
    allowBlank: true,
    list: { inCellDropDown: true, source: values },
    errorAlert: { style: "stop", title: "Valor no permitido", message },
  };
}

function applyValidations(sheet, headers, sheetName) {
  setListValidation(sheet, headers, "operacion", fixedLists.OPERACIONES_CARGA, "Use ALTA o ACTUALIZACION.");
  setListValidation(sheet, headers, "activo", fixedLists.BOOLEANOS, "Use true o false.");
  setListValidation(sheet, headers, "es_virtual", fixedLists.BOOLEANOS, "Use true o false.");
  setListValidation(sheet, headers, "rol", fixedLists.ROLES_MIGRABLES, "Seleccione un rol migrable autorizado.");
  setListValidation(sheet, headers, "etapa_procesal", fixedLists.ETAPAS_PROCESALES, "Seleccione una etapa procesal permitida.");
  setListValidation(sheet, headers, "estatus_audiencia", fixedLists.ESTATUS_AUDIENCIA, "Seleccione un estatus de audiencia permitido.");
  setListValidation(sheet, headers, "estatus_termino", fixedLists.ESTATUS_TERMINO, "Seleccione un estatus de término permitido.");
  setListValidation(sheet, headers, "estado_acta_codigo", fixedLists.ESTADOS_DOCUMENTO, "Seleccione un estado documental permitido.");
  setListValidation(sheet, headers, "estado_acuse_codigo", fixedLists.ESTADOS_DOCUMENTO, "Seleccione un estado documental permitido.");
  setListValidation(sheet, headers, "motivo_falta_acta_codigo", fixedLists.MOTIVOS_FALTA_DOCUMENTO, "Seleccione un motivo permitido.");
  setListValidation(sheet, headers, "motivo_falta_acuse_codigo", fixedLists.MOTIVOS_FALTA_DOCUMENTO, "Seleccione un motivo permitido.");
  setListValidation(sheet, headers, "tipo_dias", fixedLists.TIPOS_DIAS, "Use HABILES o NATURALES.");
  if (sheetName === "PRIORIDADES") setListValidation(sheet, headers, "prioridad_codigo", fixedLists.PRIORIDADES_V1, "Use ALTA, MEDIA o BAJA.");
  for (const [field, min, message] of [
    ["dias_predeterminados", 1, "Capture un entero mayor que cero."],
    ["semaforo_rojo_dias", 0, "Capture un entero mayor o igual a cero."],
    ["semaforo_amarillo_dias", 0, "Capture un entero mayor o igual a cero; el Backend validará que sea mayor al rojo."],
  ]) {
    const idx = headers.indexOf(field);
    if (idx >= 0) {
      sheet.getRange(`${columnLetter(idx)}2:${columnLetter(idx)}5000`).dataValidation = {
        allowBlank: false,
        rule: { type: "whole", operator: "greaterThanOrEqual", formula1: min },
        errorAlert: { style: "stop", title: "Entero inválido", message },
      };
    }
  }
}

function addControl(workbook) {
  const sheet = workbook.worksheets.add("CONTROL");
  sheet.showGridLines = false;
  sheet.tabColor = COLORS.navy;
  const headers = controlFields.map((r) => r[0]);
  const last = columnLetter(headers.length - 1);
  sheet.getRange(`A1:${last}2`).values = [headers, ["2.1-MV", null, null, null, null, null, null]];
  styleHeader(sheet.getRange(`A1:${last}1`));
  styleBody(sheet.getRange(`A2:${last}2`));
  sheet.getRange(`A2:${last}2`).format.fill = COLORS.input;
  sheet.getRange("A2:D2").format.numberFormat = "@";
  sheet.getRange("E2").format.numberFormat = "yyyy-mm-dd";
  sheet.getRange("F2:G2").format.numberFormat = "@";
  sheet.freezePanes.freezeRows(1);
  const table = sheet.tables.add(`A1:${last}2`, true, "ControlTable");
  table.style = "TableStyleMedium2";
  sheet.getRange("A:A").format.columnWidth = 16;
  sheet.getRange("B:D").format.columnWidth = 24;
  sheet.getRange("E:E").format.columnWidth = 15;
  sheet.getRange("F:F").format.columnWidth = 28;
  sheet.getRange("G:G").format.columnWidth = 30;
  sheet.getRange("A2:A2").dataValidation = { allowBlank: false, list: { inCellDropDown: true, source: ["2.1-MV"] }, errorAlert: { style: "stop", title: "Versión inválida", message: "Use 2.1-MV." } };
}

function addInstructions(workbook, layout) {
  const sheet = workbook.worksheets.add("INSTRUCCIONES");
  sheet.showGridLines = false;
  sheet.tabColor = "#70AD47";
  sheet.mergeCells("A1:F1");
  sheet.getRange("A1").values = [[layout.title]];
  sheet.getRange("A1:F1").format = { fill: COLORS.navy, font: { name: FONT, bold: true, color: COLORS.white, size: 16 }, verticalAlignment: "center" };
  sheet.getRange("A1:F1").format.rowHeight = 34;
  sheet.getRange("A3:B3").values = [["Paso", "Instrucción"]];
  const rows = layout.instructions.map((x, i) => [i + 1, x]);
  rows.push([rows.length + 1, "No cambie nombres, orden de hojas ni encabezados."]);
  rows.push([rows.length + 1, "Capture códigos y claves como texto; fechas y horas como valores reales de Excel."]);
  rows.push([rows.length + 1, "Corrija todos los errores de prevalidación antes de solicitar la carga."]);
  sheet.getRange(`A4:B${3 + rows.length}`).values = rows;
  styleHeader(sheet.getRange("A3:B3"));
  styleBody(sheet.getRange(`A4:B${3 + rows.length}`));
  sheet.getRange("A:A").format.columnWidth = 10;
  sheet.getRange("B:B").format.columnWidth = 85;
  sheet.getRange(`B4:B${3 + rows.length}`).format.wrapText = true;
  sheet.getRange(`A4:B${3 + rows.length}`).format.autofitRows();
  sheet.getRange("A13:B16").values = [
    ["Secuencia general", "Catálogos → Usuarios → Expedientes → Audiencias, Términos y Documentos de Expediente → archivos asociados → conciliación."],
    ["Identidad", "fuente_codigo + entidad + clave_origen."],
    ["Resultados", "IMPORTADO, ACTUALIZADO, OMITIDO_JUSTIFICADO o RECHAZADO."],
    ["Soporte", "Use los mensajes de error de DICCIONARIO para corregir cada fila."],
  ];
  styleBody(sheet.getRange("A13:B16"));
  sheet.getRange("A13:A16").format = { fill: COLORS.lightBlue, font: { name: FONT, bold: true, color: COLORS.navy } };
  sheet.getRange("B13:B16").format.wrapText = true;
}

function addDictionary(workbook, layout) {
  const sheet = workbook.worksheets.add("DICCIONARIO");
  sheet.showGridLines = false;
  sheet.tabColor = "#A5A5A5";
  const headers = ["Hoja", "Campo", "Tipo", "Longitud máxima", "Obligatorio", "Validación", "Valores permitidos", "Ejemplo válido", "Mensaje de error"];
  const rows = [];
  for (const row of controlFields) rows.push(["CONTROL", ...row]);
  for (const dataSheet of layout.dataSheets) {
    const key = layout.fieldKey ?? dataSheet;
    for (const row of fields[key]) rows.push([dataSheet, ...row]);
  }
  const last = columnLetter(headers.length - 1);
  sheet.getRange(`A1:${last}${rows.length + 1}`).values = [headers, ...rows];
  styleHeader(sheet.getRange(`A1:${last}1`));
  styleBody(sheet.getRange(`A2:${last}${rows.length + 1}`));
  sheet.freezePanes.freezeRows(1);
  const table = sheet.tables.add(`A1:${last}${rows.length + 1}`, true, "DictionaryTable");
  table.style = "TableStyleMedium2";
  const widths = [22, 36, 13, 16, 14, 48, 32, 36, 55];
  widths.forEach((w, i) => sheet.getRange(`${columnLetter(i)}:${columnLetter(i)}`).format.columnWidth = w);
  sheet.getRange(`A2:${last}${rows.length + 1}`).format.wrapText = true;
  sheet.getRange(`A2:${last}${rows.length + 1}`).format.autofitRows();
}

function addReferences(workbook, layout) {
  const sheet = workbook.worksheets.add("CATALOGOS_REFERENCIA");
  sheet.showGridLines = false;
  sheet.tabColor = "#7F6000";
  const rows = [];
  for (const [list, values] of Object.entries(fixedLists)) {
    for (const value of values) rows.push([list, value, "Valor controlado 2.1-MV"]);
  }
  const dynamic = layout.file === "Layout_Catalogos_V2_MV.xlsx" ? [] : ["GERENCIAS", "MATERIAS", "SEDES", "PRIORIDADES", "TIPOS_EXPEDIENTE", "ORGANOS", "TIPOS_AUDIENCIA", "TIPOS_TERMINO", "PERMISOS"];
  for (const list of dynamic) rows.push([list, "", "Se publica desde la versión aprobada de Layout_Catalogos_V2_MV.xlsx."]);
  sheet.getRange(`A1:C${rows.length + 1}`).values = [["CATALOGO", "VALOR_AUTORIZADO", "DESCRIPCION"], ...rows];
  styleHeader(sheet.getRange("A1:C1"));
  styleBody(sheet.getRange(`A2:C${rows.length + 1}`));
  sheet.getRange(`A2:C${rows.length + 1}`).format.fill = COLORS.gray;
  sheet.freezePanes.freezeRows(1);
  const table = sheet.tables.add(`A1:C${rows.length + 1}`, true, "ReferenceCatalogTable");
  table.style = "TableStyleMedium2";
  sheet.getRange("A:A").format.columnWidth = 32;
  sheet.getRange("B:B").format.columnWidth = 32;
  sheet.getRange("C:C").format.columnWidth = 70;
  sheet.getRange(`C2:C${rows.length + 1}`).format.wrapText = true;
}

async function buildLayout(layout) {
  const workbook = Workbook.create();
  addControl(workbook);
  for (const dataSheet of layout.dataSheets) {
    const key = layout.fieldKey ?? dataSheet;
    addTableSheet(workbook, dataSheet, fields[key], `Data_${dataSheet}`);
  }
  addInstructions(workbook, layout);
  addDictionary(workbook, layout);
  addReferences(workbook, layout);
  workbook.recalculate();

  const sheetInspection = await workbook.inspect({ kind: "sheet", include: "id,name", maxChars: 8000 });
  console.log(`SHEETS ${layout.file}\n${sheetInspection.ndjson}`);
  const errorInspection = await workbook.inspect({
    kind: "match",
    searchTerm: "#REF!|#DIV/0!|#VALUE!|#NAME\\?|#N/A|#NUM!|#NULL!|#SPILL!|#CALC!",
    options: { useRegex: true, maxResults: 100 },
    summary: `formula error scan ${layout.file}`,
  });
  console.log(`ERROR_SCAN ${layout.file}\n${errorInspection.ndjson}`);

  for (const sheetName of ["CONTROL", ...layout.dataSheets, "INSTRUCCIONES", "DICCIONARIO", "CATALOGOS_REFERENCIA"]) {
    const preview = await workbook.render({ sheetName, autoCrop: "all", scale: 1, format: "png" });
    const safe = `${path.parse(layout.file).name}__${sheetName}.png`;
    await fs.writeFile(path.join(PREVIEW_DIR, safe), new Uint8Array(await preview.arrayBuffer()));
  }

  const output = await SpreadsheetFile.exportXlsx(workbook);
  await output.save(path.join(OUTPUT_DIR, layout.file));
}

await fs.mkdir(OUTPUT_DIR, { recursive: true });
await fs.mkdir(PREVIEW_DIR, { recursive: true });
for (const layout of layouts) await buildLayout(layout);
for (const layout of layouts) {
  const savedPath = path.join(OUTPUT_DIR, layout.file);
  const saved = await FileBlob.load(savedPath);
  const reopened = await SpreadsheetFile.importXlsx(saved);
  const expectedSheets = ["CONTROL", ...layout.dataSheets, "INSTRUCCIONES", "DICCIONARIO", "CATALOGOS_REFERENCIA"];
  for (const sheetName of expectedSheets) reopened.worksheets.getItem(sheetName);
  const controlHeaders = reopened.worksheets.getItem("CONTROL").getRange("A1:G1").values[0];
  if (JSON.stringify(controlHeaders) !== JSON.stringify(controlFields.map((r) => r[0]))) {
    throw new Error(`CONTROL header mismatch: ${layout.file}`);
  }
  if (reopened.worksheets.getItem("CONTROL").getRange("A2").values[0][0] !== "2.1-MV") {
    throw new Error(`CONTROL version mismatch: ${layout.file}`);
  }
  for (const dataSheet of layout.dataSheets) {
    const key = layout.fieldKey ?? dataSheet;
    const expectedHeaders = fields[key].map((r) => r[0]);
    const last = columnLetter(expectedHeaders.length - 1);
    const actualHeaders = reopened.worksheets.getItem(dataSheet).getRange(`A1:${last}1`).values[0];
    if (JSON.stringify(actualHeaders) !== JSON.stringify(expectedHeaders)) {
      throw new Error(`Header mismatch: ${layout.file}/${dataSheet}`);
    }
    if (reopened.worksheets.getItem(dataSheet).tables.items.length !== 1) {
      throw new Error(`Expected one data table: ${layout.file}/${dataSheet}`);
    }
  }
  console.log(`VERIFIED ${layout.file}: ${expectedSheets.length} sheets, headers and tables OK`);
}
console.log(`CREATED ${layouts.length} workbooks in ${OUTPUT_DIR}`);

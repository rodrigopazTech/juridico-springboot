from pathlib import Path
import re, json, hashlib

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'outputs/layouts_siaj_20260907'
OUT.mkdir(parents=True, exist_ok=True)
JAVA = ROOT / 'src/main/java/com/juridico/sistema_juridico'
sql = (ROOT / 'db_schema_actual.sql').read_text(encoding='utf-8')
tables = {}
for m in re.finditer(r'CREATE TABLE public\.(\w+) \(\n(.*?)\n\);', sql, re.S):
    name, body = m.groups()
    cols = []
    for line in body.splitlines():
        line = line.strip().rstrip(',')
        if line.startswith('CONSTRAINT'): continue
        c, typ = line.split(' ', 1)
        length = re.search(r'character varying\((\d+)\)', typ)
        dtype = 'Texto' if 'character varying' in typ or typ.startswith('text') else 'UUID' if typ.startswith('uuid') else 'Booleano' if typ.startswith('boolean') else 'Fecha y hora' if typ.startswith('timestamp') else 'Fecha' if typ.startswith('date') else 'Hora' if typ.startswith('time') else 'Entero 64 bits' if typ.startswith('bigint') else 'Entero'
        cols.append(dict(name=c, type=dtype, length=length.group(1) if length else ('Sin límite declarado' if dtype=='Texto' else 'No aplica'), required='Sí (BD)' if 'NOT NULL' in typ else 'No', raw=line, source='db_schema_actual.sql', enum=[]))
    tables[name] = dict(cols=cols, body=body, constraints=[], deps=[], source='db_schema_actual.sql')
for m in re.finditer(r'ALTER TABLE ONLY public\.(\w+)\s+ADD CONSTRAINT (.*?);', sql, re.S):
    name, rule = m.groups()
    if name not in tables: continue
    tables[name]['constraints'].append(rule)
    fk = re.search(r'FOREIGN KEY \((.*?)\) REFERENCES public\.(\w+)\((.*?)\)', rule)
    if fk:
        c, target, key = fk.groups()
        tables[name]['deps'].append(f'{c} → {target}.{key}')
        for col in tables[name]['cols']:
            if col['name']==c: col['fk']=target
for name,t in tables.items():
    for col in t['cols']:
        for line in t['body'].splitlines():
            if 'CHECK' in line and re.search(r'\(\('+re.escape(col['name'])+r'\)', line):
                col['enum']=list(dict.fromkeys(re.findall(r"'([^']+)'::",line)))

def col(name, typ='Texto', length='Sin límite declarado', required='No', example='', rule='', enum=None):
    return dict(name=name,type=typ,length=str(length),required=required,example=example,rule=rule,enum=enum or [],source='Propuesta de layout')
def add_model_table(name, filename, columns, deps=(), constraints=()):
    tables[name]=dict(cols=columns, body='', constraints=list(constraints),deps=list(deps),source=filename)
    for c in columns: c['source']=filename

add_model_table('cat_tipos_termino','Entity/catalogo/TipoTermino.java',[
    col('nombre',length=100,required='Sí (JPA)'),col('descripcion'),col('dias_predeterminados','Entero','No aplica','Sí (JPA)','15'),col('tipo_dias',length=20,required='Sí (JPA)',example='HABILES',enum=['HABILES','NATURALES']),col('semaforo_rojo_dias','Entero','No aplica',example='3'),col('semaforo_amarillo_dias','Entero','No aplica',example='7'),col('activo','Booleano','No aplica',example='true'),col('materia_id','Entero','No aplica'),col('gerencia_id','Entero','No aplica')],['materia_id → materias.id','gerencia_id → gerencias.id'],['PK id INTEGER generado (JPA)','UNIQUE nombre (JPA)'])
add_model_table('cat_etapas_procesales','Entity/catalogo/EtapaCatalogo.java',[
    col('nombre',length=120,required='Sí (JPA)'),col('clave',length=80,rule='Se genera desde nombre si se omite.'),col('orden','Entero','No aplica','Sí (JPA)','1'),col('descripcion'),col('activo','Booleano','No aplica',example='true'),col('cierra_expediente','Booleano','No aplica',example='false'),col('etapa_legacy',length=30,example='TRAMITE',enum=['TRAMITE','LAUDO','FIRME','CONCLUIDO']),col('materia_id','Entero','No aplica','Sí (JPA)'),col('parent_id','Entero','No aplica')],['materia_id → materias.id','parent_id → cat_etapas_procesales.id'],['PK id INTEGER generado (JPA)','UNIQUE (materia_id, nombre, parent_id) (JPA); verificar semántica de NULL para raíces'])
add_model_table('expediente_etapa_periodos','Entity/expediente/ExpedienteEtapaPeriodo.java',[
    col('expediente_id','UUID','No aplica','Sí (JPA)'),col('etapa_procesal',length=30,required='Sí (JPA)',enum=['TRAMITE','LAUDO','FIRME','CONCLUIDO']),col('etapa_catalogo_id','Entero','No aplica'),col('fecha_inicio','Fecha y hora','No aplica','Sí (JPA)'),col('fecha_fin','Fecha y hora','No aplica'),col('usuario_inicio_id','Entero','No aplica'),col('usuario_fin_id','Entero','No aplica'),col('motivo_cambio')],['expediente_id → expedientes.id','etapa_catalogo_id → cat_etapas_procesales.id','usuario_inicio_id / usuario_fin_id → usuarios.id'],['PK id BIGINT generado (JPA)'])
tables['expedientes']['cols'].append(col('etapa_catalogo_id','Entero','No aplica',rule='Sólo JPA; no existe en el dump. Etapa de la materia del expediente.'))
tables['expedientes']['deps'].append('etapa_catalogo_id → cat_etapas_procesales.id (JPA)')
for c in [col('tipo_termino_id','Entero','No aplica'),col('es_tiempo_dinamico','Booleano','No aplica',example='false'),col('tipo_dias_calculado',length=20,example='HABILES',enum=['HABILES','NATURALES']),col('desglose_dias_texto')]:
    c['rule']='Sólo JPA; no existe en el dump. Validar despliegue y cálculo antes de importar.'
    tables['terminos']['cols'].append(c)
tables['terminos']['deps'].append('tipo_termino_id → cat_tipos_termino.id (JPA)')

names={
'gerencias':'Gerencias','materias':'Materias','cat_estados':'Estados y sedes','cat_prioridad':'Prioridades','cat_tipo_expediente':'Tipos de expediente','organos_jurisdiccionales':'Órganos jurisdiccionales','tipos_audiencia':'Tipos de audiencia','cat_tipos_termino':'Tipos de término','cat_etapas_procesales':'Etapas y subetapas procesales','usuarios':'Usuarios','usuario_gerencias':'Asignación de gerencias a usuarios','usuario_materias':'Asignación de materias a usuarios','usuario_permisos_adicionales':'Permisos adicionales','expedientes':'Expedientes','audiencia_contextos':'Contextos de audiencia','termino_contextos':'Contextos de término','audiencias':'Audiencias','terminos':'Términos','colaboradores_expediente':'Colaboradores de expediente','carpetas':'Carpetas','documentos':'Documentos de expediente','termino_archivos':'Archivos de término','comentarios':'Comentarios y observaciones','recordatorios':'Tareas y recordatorios','eventos_calendario':'Eventos de calendario','configuraciones_alerta':'Configuraciones de alertas','configuraciones_semaforo':'Configuraciones de semáforo','preferencias_notificacion':'Preferencias generales de notificación','preferencias_notificacion_evento':'Preferencias por evento','expediente_etapa_periodos':'Historial de etapas de expediente','audiencias_desahogadas':'Audiencias desahogadas','terminos_presentados':'Términos presentados'}
rules={
'gerencias':['Nombre obligatorio y único en el dump. Cargar antes de materias y usuarios.'],
'materias':['La gerencia es obligatoria en BD. Homologar por nombre y gerencia; el dump conserva unicidad global del nombre: no asumir que puede repetirse entre gerencias.'],
'cat_estados':['Catálogo usado como sede; no existe FK desde expedientes.sede. Homologar el texto por catálogo vigente. Nombre no es NOT NULL en dump; se exige en el layout como regla propuesta.'],
'cat_prioridad':['ALTA, MEDIA y BAJA son semillas de DataInitializer; el catálogo en BD es editable. Exportar IDs vigentes, no asumir 1/2/3. El campo color contiene clases CSS en las semillas, no necesariamente hexadecimal.'],
'cat_tipo_expediente':['Nombre único en dump. Confirmar ámbito global o por gerencia antes de crear duplicados.'],
'organos_jurisdiccionales':['Representa autoridades jurisdiccionales, incluidos juzgados y salas cuando corresponda. tipo es texto sin enum/CHECK en el dump. No equiparar automáticamente organismos administrativos con órganos jurisdiccionales.'],
'tipos_audiencia':['Unicidad (nombre, gerencia_id) en dump. Respetar ámbito de gerencia y visibilidad por usuario; exportar catálogo activo.'],
'cat_tipos_termino':['Modelo actual no presente en dump. Nombre obligatorio y único; validación del servicio rechaza duplicados. HABILES/NATURALES provienen del modelo, no de un CHECK del dump. Valores predeterminados: 15 días, rojo 3, amarillo 7, activo true. Validar calendario inhábil y cálculo antes de habilitar carga.'],
'cat_etapas_procesales':['Modelo no presente en dump. Padre de la misma materia, máximo dos niveles; no autorreferencia. Una etapa con hijos no se convierte en subetapa (CatalogosController). clave se normaliza desde nombre si se omite.'],
'usuarios':['email es obligatorio en dump, pero opcional en JPA y migración 03-allow-local-only-users.sql. Confirmar aplicación de esa migración.','Username se normaliza/genera por UsuarioIdentityService; username y número de empleado son únicos ignorando mayúsculas. Número de empleado se recorta y pasa a mayúsculas; email a minúsculas.','Alta mediante UsuarioService, respetando jerarquía del operador y alcance de gerencias/materias. Contraseñas, hashes y vínculos Microsoft no se solicitan en Excel; provisionar credencial/activación mediante el flujo autorizado.','El modelo usa usuario_gerencias; gerencia_id legado del dump requiere reconciliación.'],
'usuario_gerencias':['Una fila por usuario y gerencia, sin duplicar el par. No enviar listas separadas por comas en una celda.'],
'usuario_materias':['Una fila por usuario y materia; materia coherente con gerencias asignadas. Verificar alcance en servicios de autorización.'],
'usuario_permisos_adicionales':['Permisos de PermisoSistema; sólo asignación administrativa. No usar la carga para eludir jerarquías.'],
'expedientes':['Número obligatorio, máximo 50. No hay UNIQUE(numero) en el dump. No usar sólo el número para conciliar expedientes homónimos.','Materia y gerencia son NOT NULL en dump y DTO; JPA no declara la misma obligatoriedad. Exigir ambas y coherencia entre ellas.','Descripción obligatoria; etapa enum obligatorio con valor inicial TRAMITE en JPA. partes es texto libre sin estructura normalizada de actores/demandados.','El asistente deriva expedientes de audiencias y términos; este layout independiente necesita importador. es_temporal corresponde a manejo interno, no a captura ordinaria.'],
'audiencias':['Fecha y hora obligatorias en BD; tipo requerido por controlador aunque nullable en BD. expediente_id NOT NULL en dump.','Estado: PENDIENTE, CON_ACTA, CONCLUIDA. Para estados con acta/desahogo, validar evidencia mediante el flujo correspondiente.','Contexto es único por audiencia. El esquema admite contexto ASUNTO_UNICO pero no permite expediente_id nulo: no habilitar audiencia independiente sin resolver esa contradicción.','Propuesta de calidad: si es_virtual=true exigir URL de reunión; validar que compareciente y delegado tengan acceso al expediente.'],
'terminos':['Asunto obligatorio en controlador aunque nullable en BD. Vencimiento obligatorio.','Puede vincularse a expediente o a contexto de ASUNTO_UNICO. Para asunto único, controlador exige materia, gerencia y descripción. No exigir siempre expediente a partir del DTO legado asuntoId Integer.','Estados: PROYECTISTA, REVISION, GERENCIA, DIRECCION, LIBERADO, PRESENTADO, CONCLUIDO. Conservar transición y permisos del flujo. PRESENTADO debe acompañarse de presentación y acuse validados.','prioridad_id es relación usada por JPA. Las columnas prioridad y archivo_word del dump no están mapeadas en Termino actual; no cargarlas directamente.','Cálculo dinámico y tipo de término son añadidos JPA: validar esquema desplegado, tipo de días y calendario; no inferir vencimiento legal automáticamente desde esta documentación.'],
'audiencia_contextos':['Captura complementaria de materia, gerencia, descripción y partes. No compartir contexto entre audiencias: UNIQUE contexto_id en audiencias. Puede generarse como snapshot desde expediente.'],
'termino_contextos':['Captura complementaria para origen EXPEDIENTE o ASUNTO_UNICO. No compartir contexto entre términos: UNIQUE contexto_id en terminos.'],
'colaboradores_expediente':['Expediente, usuario y fecha de expiración obligatorios. permiso_nivel es texto sin CHECK; exportar valores usados por el flujo antes de habilitarlo. Propuesta: impedir duplicación de colaboración activa del mismo usuario/expediente.'],
'carpetas':['Padre previamente cargado, mismo expediente y sin ciclos (regla de importación propuesta). Carpeta de sistema, orden técnico y fecha de creación se generan mediante servicio.'],
'documentos':['Entregar manifiesto más archivos reales. Nombre/ruta/tamaño internos se generan al almacenar; el texto de una ruta no sube el archivo.','FileStorageService limita documentos a 150 MiB, imágenes a 30 MiB y audio/video a 1 GiB. Verificar además límite HTTP/proxy de destino.','Carpeta debe corresponder al expediente. No importar flags de papelera ni rutas absolutas del equipo de origen como rutas del servidor.'],
'termino_archivos':['Manifiesto y binarios. BORRADOR/ACUSE son valores del enum. Etapa usa estados de término. Validar permisos y flujo al subir acuse.','Ruta, tamaño, MIME y autor se registran por el servicio de almacenamiento; preservar metadatos históricos sólo mediante migración controlada.'],
'comentarios':['entidad_id es texto polimórfico sin FK: validar manualmente tipo y existencia de entidad. Usuario y fecha deben conservarse sólo para migración histórica autorizada.','etapa_procesal acepta sólo enum de expediente; no confundir con estados de término.'],
'recordatorios':['Título, fecha, hora y usuario obligatorios. DTO usa prioridad normal/urgent, mientras BD usa prioridad_id: definir equivalencia explícita, no asumir identidad de códigos.'],
'eventos_calendario':['categoría es texto sin CHECK. Confirmar valores del servicio antes de importar. No duplicar eventos derivados de audiencias/términos.','Propuesta: fecha_fin >= fecha_inicio. entidad_id es Integer: no admite directamente UUID de expediente.'],
'configuraciones_alerta':['Una fila por día de anticipación. Servicio admite días 0..365. Tipo TERMINO o AUDIENCIA.','Nivel SISTEMA, GERENCIA, USUARIO o ASUNTO; identificar gerencia/usuario/asunto según nivel. Definir precedencia mediante servicio, no insertar reglas contradictorias.'],
'configuraciones_semaforo':['Servicio sólo admite SISTEMA y GERENCIA aunque CHECK incluye USUARIO y ASUNTO. Gerencia requerida para nivel GERENCIA.','rojo_hasta >= 0 y amarillo_hasta > rojo_hasta. Persistir usando servicio para conservar unicidad lógica por tipo/nivel/gerencia.'],
'preferencias_notificacion':['Una preferencia por usuario según UNIQUE. Modelo general previo al esquema por evento: confirmar cuál prevalece.'],
'preferencias_notificacion_evento':['Par (usuario_id, evento) único. Validar contra enum y CHECK; no cambiar notificaciones de seguridad obligatorias por un importador genérico sin comprobar servicio.'],
'expediente_etapa_periodos':['Histórico técnico generado por cambios de etapa. Sólo migración histórica, no alta ordinaria. Propuesta: fin >= inicio, sin períodos superpuestos y un período abierto por expediente.'],
'audiencias_desahogadas':['Tabla de sincronización/resultado. Generar mediante flujo de desahogo; no carga paralela de usuario. Migración histórica debe conciliar audiencia y fechas. No hay UNIQUE audiencia_id en dump.'],
'terminos_presentados':['Tabla de sincronización/resultado. Generar mediante flujo de presentación; no carga paralela de usuario. Migración histórica debe conciliar término, fecha y acuse. No hay UNIQUE termino_id en dump.']}
descriptions={
'expedientes':'Registro central del asunto jurídico y sus relaciones organizacionales, responsables y etapa.',
'audiencias':'Programación y seguimiento de comparecencias vinculadas a un expediente.',
'terminos':'Control de actuaciones y vencimientos, responsables y estado de revisión/presentación.',
'usuarios':'Cuentas, identidad laboral, roles y alcance de acceso al sistema.',
'organos_jurisdiccionales':'Catálogo de autoridades ante las que se tramita el expediente.',
'documentos':'Manifiesto para incorporar archivos al repositorio del expediente.',
'cat_estados':'Catálogo de estados/sedes utilizado para homologar ubicaciones de los asuntos.'}
auto={'id','created_at','updated_at','fecha_creacion','fecha_subida','nombre_archivo','ruta_archivo','tamanio_bytes','tipo_mime','usuario_subida_id','usuario_creador_id','creado_por_id','ultimo_acceso','password_hash','force_password_change','password_updated_at','es_temporal','es_sistema','sincronizado_at','usuario_nombre','abogado_responsable_nombre','carpeta_original_id','carpeta_original_ruta','eliminado','eliminado_definitivo','fecha_eliminacion','fecha_eliminacion_definitiva','usuario_eliminacion_id'}
examples={'numero':'EXP-001/2026','nombre':'Valor del catálogo','nombre_completo':'Ana Pérez López','email':'ana.perez@example.org','username':'ana.perez','numero_empleado':'000123','descripcion':'Seguimiento del asunto de ejemplo','partes':'Actor: Persona A; Demandado: Entidad B','sede':'Ciudad de México','sala_lugar':'Sala 1','url_reunion':'https://example.org/reunion/123','titulo':'Revisar escrito','asunto':'Presentar contestación','color':'bg-red-100 text-red-800','archivo_relativo':'documentos/acuse-001.pdf','ruta_relativa':'documentos/acuse-001.pdf','nombre_original':'acuse-001.pdf','permiso_nivel':'Pendiente de catálogo','categoria':'Pendiente de catálogo','entidad_tipo':'EXPEDIENTE','entidad_id':'00000000-0000-4000-8000-000000000001','puesto':'Abogada','telefono_movil':'5550000000','telefono_institucional':'5550000000 ext. 100','ubicacion_oficina':'Oficina 1'}
def example(c):
    return c.get('example') or (c['enum'][0] if c['enum'] else examples.get(c['name']) or {'UUID':'00000000-0000-4000-8000-000000000001','Entero':'1','Entero 64 bits':'1','Booleano':'true','Fecha':'2026-09-15','Hora':'09:30','Fecha y hora':'2026-09-15T09:30:00'}.get(c['type'],'Texto de ejemplo'))
def describe(c):
    if c.get('fk'): return 'Referencia a '+names.get(c['fk'],c['fk'])
    return {'partes':'Identificación textual de las partes procesales','email':'Correo de acceso o contacto','username':'Nombre de acceso local','fecha_vencimiento':'Fecha límite registrada para la actuación','fecha_ingreso':'Fecha de ingreso de la actuación','es_virtual':'Indica audiencia por videoconferencia','parent_id':'Etapa superior','archivo_relativo':'Ruta relativa dentro del paquete de archivos','etapa_catalogo_id':'Etapa configurable de la materia','tipo_termino_id':'Tipo configurable para plazo'}.get(c['name'],c['name'].replace('_',' ').capitalize())
layouts=[]
for key,label in names.items():
    t=tables[key]; cs=[]
    for original in t['cols']:
        c=original.copy(); n=c['name']
        if n in auto or n.startswith('microsoft_') or (key=='usuarios' and n=='gerencia_id') or (key=='terminos' and n in {'prioridad','archivo_word'}): continue
        if key in {'cat_estados'} and n=='nombre': c['required']='Sí (propuesta)'
        if key=='usuarios' and n=='email': c['required']='Condicional: ver discrepancia BD/JPA'
        if key=='audiencias' and n=='tipo_audiencia_id': c['required']='Sí (controlador)'
        if key=='terminos' and n=='asunto': c['required']='Sí (controlador)'
        if key=='terminos' and n in {'expediente_id','contexto_id'}: c['required']='Condicional: origen del asunto'
        if key=='configuraciones_semaforo' and n=='nivel': c['enum']=['SISTEMA','GERENCIA']
        r=[c.get('rule','')]
        if c.get('fk'): r.append('Debe existir en '+c['fk']+'; consultar hoja Catalogos.')
        if c['enum']: r.append('Permitidos: '+', '.join(c['enum'])+'.')
        if c['type']=='Fecha': r.append('AAAA-MM-DD, fecha real; no fórmulas.')
        if c['type']=='Hora': r.append('HH:mm, reloj de 24 horas.')
        if c['type']=='Fecha y hora': r.append('AAAA-MM-DDTHH:mm:ss; confirmar zona del servidor.')
        if c['type']=='Booleano': r.append('true o false; no sustituir vacío por false sin regla.')
        if c['type'].startswith('Entero'): r.append('Sin decimales; IDs del ambiente destino.')
        if c['type']=='Texto': r.append('Texto literal; conservar ceros iniciales en identificadores.')
        c['rule']=' '.join(x for x in r if x)
        c['description']=describe(c); c['example']=example(c)
        cs.append(c)
    if key in {'documentos','termino_archivos'}:
        c=col('archivo_relativo',length='255 (propuesta)',required='Sí (propuesta)',example='documentos/acuse-001.pdf',rule='Archivo real dentro del paquete; sin rutas absolutas ni ../. El importador almacena y genera metadatos.')
        c['description']='Archivo a incorporar'; cs.append(c)
    status='Diseño propuesto; requiere importador'
    if key in {'expediente_etapa_periodos','audiencias_desahogadas','terminos_presentados'}: status='Sólo migración histórica controlada'
    if key in {'audiencias','terminos'}: status='Layout completo propuesto; API histórica parcial existente'
    if key in {'cat_etapas_procesales','cat_tipos_termino'}: status='JPA sin tabla en dump; validar despliegue'
    layouts.append(dict(key=key,name=label,file='SIAJ_'+key+'_v1.xlsx',sheet=key[:31],description=descriptions.get(key,'Captura de '+label.lower()+' para carga inicial o migración controlada.'),status=status,cols=cs,rules=rules.get(key,[]),deps=t['deps'],constraints=t['constraints'],source=t['source'],excluded=[c['name'] for c in t['cols'] if c['name'] not in [x['name'] for x in cs]]))

# Contratos separados para las rutas realmente implementadas.
legacy=[('NumeroExpediente',50,'Sí','EXP-001/2026'),('Materia',100,'Recomendado','Laboral'),('OrganoJurisdiccional','Sin límite declarado','No','Tribunal de ejemplo'),('EmailAbogadoResponsable',100,'Recomendado','ana.perez@example.org'),('Actuacion','Sin límite declarado','Sí (propuesta)','Presentar contestación'),('FechaVencimiento','No aplica','Sí','2026-09-15'),('Prioridad',50,'Recomendado','MEDIA')]
cs=[]
for n,l,r,e in legacy:
    cs.append(dict(name=n,description=n,type='Fecha' if n=='FechaVencimiento' else 'Texto',length=str(l),required=r,example=e,rule='Columna posicional; conservar este orden. Primera hoja y primera fila de encabezados.',enum=[],source='service/procesal/TerminoImportService.java'))
layouts.append(dict(key='terminos_excel_legado',name='Términos: Excel legado',file='SIAJ_terminos_excel_legado_v1.xlsx',sheet='terminos_excel_legado',description='Contrato de siete columnas leído por TerminoImportService; servicio existente sin invocación desde controlador encontrada.',status='Servicio existente; ruta de carga no confirmada',cols=cs,deps=['Usuarios por email','Gerencias','Materias','Prioridades','Expedientes por número'],constraints=[],excluded=[],source='service/procesal/TerminoImportService.java',rules=['XLSX auténtico; primera hoja, encabezado fila 1 y datos desde fila 2. Filas sin número se omiten.','Fecha nativa Excel o texto AAAA-MM-DD/dd/MM/aaaa; preferir ISO. Número de expediente siempre texto.','Si expediente no existe, crea expediente en TRAMITE con descripción automática; toma gerencia del abogado o la primera disponible y materia por nombre o la primera disponible. Esto puede asignar incorrectamente: exigir homologación previa.','Email inexistente deja abogado nulo. Prioridad desconocida cae en MEDIA o null. Crea un nuevo término por fila sin deduplicación; no reimportar el mismo archivo.']))
common=[('numeroExpediente',50,'Sí (propuesta)','EXP-001/2026'),('gerencia',200,'Sí o global del lote','Gerencia Laboral y Penal'),('materia',100,'Sí (propuesta)','Laboral'),('descripcion','Sin límite declarado','Recomendado','Asunto de ejemplo'),('partes','Sin límite declarado','Recomendado','Actor: Persona A; Demandado: Entidad B'),('sede',150,'No','Ciudad de México'),('organoJurisdiccional',300,'No','Tribunal de ejemplo'),('abogadoResponsable',200,'Recomendado','Ana Pérez López')]
for kind,extra in [('audiencias',[('tipoAudiencia',100,'Sí (propuesta)','Conciliación'),('fechaAudiencia','No aplica','Sí (propuesta)','2026-09-15'),('horaAudiencia','No aplica','Sí (propuesta)','09:30'),('sala',490,'No','Sala 1'),('abogadoCompareciente',200,'No','Ana Pérez López'),('esEnLinea','No aplica','No','false'),('enlaceReunion',490,'Condicional: virtual','https://example.org/reunion/123'),('actaDocumento',490,'No','acta-001.pdf'),('observaciones','Sin límite declarado','No','Observación de ejemplo')]),('terminos',[('asunto',490,'Sí (propuesta)','Presentar contestación'),('fechaVencimiento','No aplica','Sí (propuesta)','2026-09-15'),('fechaIngreso','No aplica','Recomendado','2026-09-07'),('prioridad',50,'Recomendado','MEDIA'),('acuse',490,'No','acuse-001.pdf'),('acuseNombre',490,'No','Acuse de recibo'),('observaciones','Sin límite declarado','No','Observación de ejemplo')])]:
    cs=[]
    for n,l,r,e in common+extra:
        typ='Fecha' if n.startswith('fecha') else 'Hora' if n.startswith('hora') else 'Booleano' if n=='esEnLinea' else 'Texto'
        cs.append(dict(name=n,description=n,type=typ,length=str(l),required=r,example=e,enum=[],source='service/procesal/LoteMigracionService.java',rule='Clave JSON exacta. Nombres de catálogos/usuarios homologados en asistente. '+('Formato ISO AAAA-MM-DD obligatorio en la propuesta.' if typ=='Fecha' else 'Formato HH:mm.' if typ=='Hora' else '')))
    layouts.append(dict(key=kind+'_api_historica',name=kind.capitalize()+': API histórica',file='SIAJ_'+kind+'_api_historica_v1.json',sheet=kind+'_api_historica',description='Contrato recomendado dentro de las claves leídas por la API y el asistente. La hoja Excel requiere transformación a JSON, no se carga directamente.',status='API existente; homologación e inyección posteriores',cols=cs,deps=['Catálogos homologados en lote','Gerencia global si falta en filas','Usuarios','Expedientes derivados'],constraints=[],excluded=[],source='service/procesal/LoteMigracionService.java',rules=['POST /api/migration/lote/'+kind+' admite lista de objetos; X-API-KEY configurada. Recepción no equivale a persistencia final.','El asistente homologa catálogos, revisa expedientes, audiencias y términos antes de inyectar.','Fechas vacías o inválidas se sustituyen por HOY y hora inválida por 00:00; prevalidarlas y rechazar errores antes de enviar.','La inyección asigna CONCLUIDO/CONCLUIDA si la fecha es anterior a HOY; en otro caso PROYECTISTA/PENDIENTE. No conserva un estado histórico enviado arbitrariamente.','Deduplica términos por expediente+asunto+vencimiento y audiencias por expediente+fecha+hora; esto no prueba identidad de negocio y puede omitir eventos distintos.','Campos de archivos son referencias textuales; no transfieren binarios. Longitudes 490 reflejan truncamiento del importador en campos señalados.','Para términos usar asunto, sin enviar simultáneamente prestacion/actuacion; la vista e inyección no resuelven esos alias de la misma manera. archivoWord se asigna a acuseNombre en el importador, no al documento de borrador.']))

# Entidades solicitadas que no están normalizadas.
for key,label,cols,destination in [
('actores','Actores',[col('expediente_id','UUID','No aplica','Sí (propuesta)'),col('nombre_actor',length='Por definir',required='Sí (propuesta)')],'expedientes.partes'),
('demandados','Demandados',[col('expediente_id','UUID','No aplica','Sí (propuesta)'),col('nombre_demandado',length='Por definir',required='Sí (propuesta)')],'expedientes.partes'),
('organismos','Organismos',[col('nombre_organismo',length='Por definir',required='Sí (propuesta)'),col('funcion_en_asunto',length='Por definir',required='Sí (propuesta)')],'Sin destino inequívoco'),
('salas','Salas',[col('expediente_id','UUID','No aplica','Sí (propuesta)'),col('sala',length='Sin límite SQL; 490 en migración',required='Sí (propuesta)')],'audiencias.sala_lugar o organos_jurisdiccionales, según significado'),
('juzgados','Juzgados',[col('nombre',length=300,required='Sí (propuesta)'),col('gerencia_id','Entero','No aplica'),col('sede',length=100),col('tipo',length=50,example='JUZGADO')],'organos_jurisdiccionales')]:
    for c in cols: c.update(description=describe(c),example=example(c))
    layouts.append(dict(key=key,name=label,file='SIAJ_'+key+'_levantamiento_v1.xlsx',sheet=key,description='Levantamiento preliminar, sin tabla independiente encontrada. Destino posible: '+destination+'.',status='Levantamiento; no importable como entidad independiente',cols=cols,deps=['Expedientes u órganos jurisdiccionales según significado'],constraints=['No existe PK ni unicidad específica confirmada para esta entidad'],excluded=[],source='Inventario SQL y entidades JPA',rules=['No importar esta hoja directamente. Validar modelo y correspondencia funcional.','Para actores y demandados, el texto partes permite múltiples nombres pero pierde identificación estructurada; no inventar RFC, CURP, domicilios, personalidad o llaves únicas.','Para salas/juzgados, usar el layout de órganos si representan autoridad; sala_lugar si se trata del recinto. JUZGADO es ejemplo propuesto, no valor de catálogo confirmado.'] if key in {'actores','demandados','salas','juzgados'} else ['Confirmar si se trata de parte demandada, autoridad jurisdiccional o unidad administrativa. Definir atributos, claves, relaciones y catálogo antes de implementar.']))

def escape(v): return str(v).replace('|','\\|').replace('\n',' ')
def mdtable(headers, rows): return '| '+' | '.join(headers)+' |\n| '+' | '.join(['---']*len(headers))+' |\n'+''.join('| '+' | '.join(escape(v) for v in r)+' |\n' for r in rows)
intro='''# SIAJ: layouts, plantillas y formatos de carga masiva

Fecha de análisis: 7 de septiembre de 2026. Versión de propuesta: 1.0.

## Alcance y criterio de evidencia

Se revisaron el dump `db_schema_actual.sql`, los modelos JPA actuales, DTO, controladores, servicios de importación, catálogos y scripts de despliegue locales. Se incluye el estado actual del directorio de trabajo, con cambios locales, no sólo el último commit. No se conectó a una base de datos ni se ejecutó una importación. Los ejemplos son ficticios. Los nombres e IDs vigentes del ambiente destino deben exportarse antes de distribuir las plantillas definitivas.

**Los layouts completos son contratos de captura propuestos, no una afirmación de que SIAJ ya tenga importadores para todas las entidades.** Las rutas históricas existentes tienen contratos separados. No renombrar un CSV a .xlsx: un Excel debe ser un archivo OOXML real.

En las tablas: **Sí (BD)** proviene de NOT NULL del dump; **Sí (JPA)** de modelo no confirmado en BD; **Sí (controlador)** de validación observada; **Sí (propuesta)** añade calidad de captura; **Condicional** requiere aplicar la regla indicada. Un valor opcional en BD no garantiza que la interfaz lo acepte. Longitud en caracteres; para UUID/enteros/fechas no aplica longitud de texto. TEXT no tiene límite declarado en el esquema; Excel limita cada celda a 32,767 caracteres, por lo que textos mayores requieren JSON/archivo y adaptación del importador.

## Instrucciones comunes para usuarios

1. Capturar una fila por registro y conservar encabezados exactos. No incluir subtotales, celdas combinadas, fórmulas, macros ni filas decorativas dentro de datos.
2. El libro entregado concentra las hojas de captura. Las hojas Resumen, Diccionario, Catalogos y Ejemplos son de consulta; no se importan. Los ejemplos están separados de la captura vacía.
3. Los archivos individuales sugeridos se obtienen copiando la hoja indicada a un libro nuevo como primera hoja. Para el Excel legado conservar exclusivamente sus siete columnas. El libro consolidado no debe enviarse completo al importador legado.
4. Fechas AAAA-MM-DD, horas HH:mm, booleanos true/false. Identificadores y números de expediente como texto para preservar ceros. Vacío representa ausencia, nunca la palabra NULL ni N/A.
5. En layouts propuestos, columnas *_id son referencias del ambiente destino: el equipo entrega catálogo con id y nombre. No reutilizar IDs de otra instalación. Para altas relacionadas en el mismo lote, cargar padres, recuperar IDs y después completar hijos.
6. Propuesta para automatizar cargas relacionadas: mantener tabla técnica de equivalencias (entidad, clave_origen, id_destino, lote); no inventar IDs ni usar nombres ambiguos. Este mecanismo todavía requiere implementación.
7. CSV alternativo sólo con adaptador: UTF-8, coma como separador, comillas dobles para textos con comas/saltos y escape doble de comillas. Los importadores XLSX existentes no leen CSV por cambiar la extensión.
8. Archivos adjuntos: manifiesto más carpeta de binarios, rutas relativas; validar existencia y nombre. Los campos internos de ruta, tamaño, MIME, IDs generados y auditoría los completa el servicio.
9. Rechazar duplicados, referencias inexistentes y catálogos inactivos antes de aplicar cambios (política propuesta). No sobrescribir registros por coincidencia parcial; separar altas de actualizaciones con clave inequívoca.

## Orden de carga propuesto

Gerencias → materias y catálogos básicos → usuarios → asignaciones de gerencias/materias y permisos → etapas/tipos de término → expedientes → contextos → audiencias/términos → carpetas (padres antes de hijos) → documentos/archivos → colaboradores/comentarios → recordatorios/eventos manuales → configuraciones/preferencias. El usuario creador de catálogos lo aporta la sesión administrativa para evitar ciclos. Los históricos derivados se generan por el flujo, o se concilian al final de una migración histórica específica.

## Matriz resumen

'''
doc=intro+mdtable(['Entidad / proceso','Archivo sugerido','Hoja de captura','Estado'],[[l['name'],l['file'],l['sheet'],l['status']] for l in layouts])
for l in layouts:
    doc+='\n# Entidad: '+l['name']+'\n\n## Descripción\n\n'+l['description']+'\n\nEstado: **'+l['status']+'**.\n\n## Archivo sugerido\n\n`'+l['file']+'`. Formato: '+('JSON UTF-8; hoja Excel sólo para captura y conversión.' if l['file'].endswith('.json') else 'Excel XLSX; CSV sólo con adaptador. También disponible como hoja `'+l['sheet']+'` del libro consolidado.')+'\n\n## Estructura del layout\n\n'
    doc+=mdtable(['Campo','Descripción','Tipo de dato','Obligatorio','Longitud máxima','Ejemplo','Validaciones'],[[c['name'],c['description'],c['type'],c['required'],c['length'],c['example'],c.get('rule','') or 'Validar definición funcional antes de importar.'] for c in l['cols']])
    doc+='\n## Dependencias\n\n'+('\n'.join('- '+d+'. Cargar previamente o resolver en el mismo proceso controlado.' for d in l['deps']) if l['deps'] else '- Sin FK externa confirmada; consultar alcance funcional y catálogos.')+'\n\n## Reglas de negocio\n\n'+'\n'.join('- '+r for r in l['rules'])+'\n'
    if l['constraints']: doc+='\nLlaves y restricciones observadas:\n\n'+'\n'.join('- `'+r+'`.' for r in l['constraints'])+'\n'
    doc+='\n## Observaciones para el usuario\n\n- Consultar hoja Catalogos y reglas comunes. Valores de ejemplo no son registros existentes.\n- '+('No se entrega un importador ejecutable; este contrato requiere validación y adaptación técnica.' if 'existente' not in l['status'] else 'Confirmar la ruta y ejecutar primero una carga de ensayo con conciliación de resultados.')+'\n'
    if l['excluded']: doc+='- Campos fuera de captura ordinaria: `'+ '`, `'.join(l['excluded'])+'`. Son generados, derivados, legados o exclusivos de migración técnica; ver inventario.\n'
    doc+='- Evidencia: `'+l['source']+'`'+(' y servicios/controladores indicados en las reglas.' if l['source']=='db_schema_actual.sql' else '.')+'\n'

catalog=[]
for l in layouts:
    for c in l['cols']:
        if c['enum']: catalog.append([l['key'],c['name'],', '.join(c['enum']),'Valores cerrados según CHECK/modelo/servicio indicado; ver regla de entidad'])
        if c.get('fk'): catalog.append([l['key'],c['name'],'Exportar id, nombre, activo y ámbito de '+c['fk'],'Cargar previamente; IDs reales pendientes del ambiente destino'])
catalog += [['cat_prioridad','nombre','ALTA, MEDIA, BAJA','Semillas DataInitializer; no lista inmutable de BD'],['materias','nombre','Civil, Mercantil, Fiscal, Administrativo, Laboral, Penal, Amparo, Transparencia','Semillas DataInitializer; confirmar catálogo vigente'],['cat_tipos_termino','tipo_dias','HABILES, NATURALES','Convención JPA; falta confirmar calendario inhábil'],['recordatorios','prioridad DTO','normal, urgent','No equivalen automáticamente a IDs de cat_prioridad'],['organos_jurisdiccionales','tipo','Pendiente de catálogo vigente','Texto libre; JUZGADO/SALA no son enum confirmado'],['colaboradores_expediente','permiso_nivel','Pendiente de valores vigentes','Texto libre sin CHECK'],['eventos_calendario','categoria','Pendiente de valores vigentes','Texto libre sin CHECK']]
doc+='\n# Catálogos y valores permitidos\n\nLos valores enumerados son códigos exactos. Para catálogos editables la hoja describe qué exportar; no contiene IDs ficticios presentados como válidos.\n\n'+mdtable(['Entidad','Campo','Valores o fuente','Carga previa / observación'],catalog)

covered=set(names)
excluded=[]
for k,t in tables.items():
    if k in covered: continue
    reason='Uso técnico interno; no carga de usuario final.'
    if k.startswith('bk_'): reason='Respaldo histórico: no restaurar como datos maestros ni incluir en carga ordinaria.'
    if k in {'auditoria_eventos','auditoria_sesiones','actividad_expedientes','actividad_usuarios','bitacora_datos'}: reason='Auditoría/trazabilidad generada por operaciones; sólo conservación histórica técnica, sin edición por usuarios.'
    if k in {'password_reset_tokens','push_subscriptions'}: reason='Tokens/credenciales/suscripciones de seguridad; se generan o registran en sesión, no se migran por Excel.'
    if k in {'lotes_migracion','registros_migracion','catalogos_migracion'}: reason='Staging y homologación generados por asistente/API; no insertar mediante plantilla de usuario.'
    if k=='documentos_expediente': reason='Tabla presente en dump sin entidad Documento correspondiente (actual usa documentos). Validar si es legado antes de migrar; no duplicar archivos.'
    excluded.append([k,'Sin archivo de carga de usuario',reason])
excluded.append(['manual_recovery_codes (JPA/script)','Sin archivo de carga de usuario','Códigos de recuperación de seguridad; no distribuir ni importar en plantilla.'])
doc+='\n# Matriz de entidades técnicas y excluidas\n\n'+mdtable(['Tabla / entidad','Archivo','Justificación'],excluded)
gaps=[
['G01','Esquema destino','SQL/JPA difieren: email, multi-gerencia, tipos de término, etapas y períodos, columnas dinámicas de término. DBA debe entregar DDL vigente y versión desplegada.','DBA / técnico'],
['G02','Plantillas descargables','AdminImportController devuelve bytes de encabezados separados por comas con extensión .xlsx; generar OOXML real y probar lector.','Técnico'],
['G03','Rutas Excel','ImportService de audiencias sólo itera filas y no guarda. TerminoImportService tiene lógica, pero no se encontró controlador que lo invoque. Confirmar o implementar ruta.','Técnico'],
['G04','Calidad de migración','parseDate usa HOY y parseTime 00:00 ante error; rechazar entrada inválida y registrar error por fila.','Técnico / funcional'],
['G05','Estados históricos','No concluir por antigüedad de fecha: acordar conservación de estado, desahogo, presentación y evidencia documental; modificar el importador.','Funcional / técnico'],
['G06','Conciliación','La inyección captura excepciones por registro y después marca PROCESADO; el estado del lote no garantiza persistencia. Conciliar cantidades y claves contra destino.','Técnico / datos'],
['G07','Duplicados','Número de expediente no es único en BD. Definir clave de negocio y política de coincidencia por materia/gerencia/órgano/partes; no fusionar homónimos.','Funcional / datos'],
['G08','Entidades no normalizadas','Actores, demandados y organismos carecen de estructura inequívoca; definir identificación, cardinalidad, atributos y reglas. Salas y juzgados requieren precisar autoridad vs recinto.','Funcional'],
['G09','Catálogos vigentes','No se dispone de datos del ambiente destino. Exportar id, nombre, activo, gerencia/materia y usuario propietario; resolver valores obsoletos y duplicados.','Administrador / datos'],
['G10','Asuntos únicos','Audiencia exige expediente en dump/controlador. Validar cómo se crean expedientes temporales y contextos; no importar audiencias huérfanas.','Técnico / funcional'],
['G11','Plazos y calendario','Definir calendario de inhábiles, tipo de días, fecha base y zona horaria. No existe evidencia suficiente para garantizar cálculo de vencimientos jurídicos.','Funcional'],
['G12','Almacenamiento','Validar raíces, límites HTTP/proxy, extensiones y flujo de acta/acuse. Referencias JSON no suben binarios.','Técnico'],
['G13','Semántica histórica','Definir autores/fechas originales, actualizaciones vs altas, reintentos, lotes atómicos y reversión. Propuesta de clave_origen requiere implementación.','Funcional / técnico'],
['G14','Campos incompatibles','DTO asuntoId Integer no equivale al expediente UUID; prioridad/archivo_word del dump y archivoWord del JSON no equivalen al modelo de archivos actual.','Técnico'],
['G15','Configuración','Validar valores de permiso_nivel, categoría de evento, entidad_tipo, equivalencia normal/urgent y precedencia de preferencias general/por evento.','Funcional / técnico']]
doc+='\n# Información faltante y validaciones pendientes\n\n'+mdtable(['ID','Elemento','Información faltante o acción','Responsable'],gaps)
doc+='''
# Protocolo propuesto de aceptación de carga

1. Congelar versión de plantilla y exportación de catálogos del destino.
2. Validar encabezados, tipos, longitudes, obligatorios, fechas reales y referencias antes de persistir. No truncar ni sustituir datos sin error explícito.
3. Reportar por fila: archivo, hoja, número de fila, clave de origen, campo, valor rechazado, código, mensaje y acción. Este reporte es un requisito propuesto, no una función confirmada.
4. Ensayar altas, repetición del lote, homónimos, referencia ausente, catálogo inactivo, fecha inválida, asunto único, usuario sin correo y documento ausente.
5. Conciliar recibidos = aceptados + rechazados + omitidos justificados. Verificar IDs, relaciones, estados y adjuntos realmente persistidos, no sólo el indicador PROCESADO.
6. Emitir acta de carga con lote, versión, responsable, totales y pendientes; conservar equivalencias y fuente original. No se ha ejecutado este protocolo contra una instancia desde este análisis.

# Fuentes locales y trazabilidad

Los máximos y NOT NULL provienen del dump, salvo campos expresamente marcados JPA o propuesta. Las reglas de aplicación se sustentan en los archivos siguientes; no constituyen asesoría sobre el cómputo jurídico de plazos.

'''
sources=['db_schema_actual.sql','src/main/java/com/juridico/sistema_juridico/controller/AdminImportController.java','src/main/java/com/juridico/sistema_juridico/controller/api/MigrationApiController.java','src/main/java/com/juridico/sistema_juridico/service/procesal/LoteMigracionService.java','src/main/java/com/juridico/sistema_juridico/service/procesal/TerminoImportService.java','src/main/java/com/juridico/sistema_juridico/service/ImportService.java','src/main/java/com/juridico/sistema_juridico/controller/AudienciasController.java','src/main/java/com/juridico/sistema_juridico/controller/TerminosController.java','src/main/java/com/juridico/sistema_juridico/controller/CatalogosController.java','src/main/java/com/juridico/sistema_juridico/service/UsuarioService.java','src/main/java/com/juridico/sistema_juridico/service/configuracion/ConfiguracionAlertaService.java','src/main/java/com/juridico/sistema_juridico/service/configuracion/ConfiguracionSemaforoService.java','src/main/java/com/juridico/sistema_juridico/service/FileStorageService.java','src/main/java/com/juridico/sistema_juridico/config/DataInitializer.java','deploy/database/03-allow-local-only-users.sql']
for s in sources: doc+='- ['+s+']('+ (ROOT/s).as_posix()+')\n'
(OUT/'SIAJ_Layouts_Carga_Masiva_v1.md').write_text(doc,encoding='utf-8')
inventory='# SIAJ: inventario físico de referencia\n\nEsquema local; no certifica esquema desplegado. PK/FK/UNIQUE/CHECK conservados para trazabilidad técnica. No es una plantilla de carga SQL.\n\n'
for k,t in tables.items():
    inventory+='## '+k+'\n\nFuente: '+t['source']+'\n\n'
    inventory+=mdtable(['Campo','Tipo','Máximo','Nulabilidad / obligatorio','Definición observada'],[[c['name'],c['type'],c['length'],c['required'],c.get('raw',c.get('rule','Modelo JPA/propuesta marcada en documento'))] for c in t['cols']])
    inventory+='\n'+ '\n'.join('- `'+x+'`' for x in t['constraints'])+'\n'
    for ln in t['body'].splitlines():
        if 'CHECK' in ln: inventory+='- `'+ln.strip()+'`\n'
inventory+='\n## Índices únicos adicionales\n\n'+ '\n'.join('- `'+m.group(0)+'`' for m in re.finditer(r'CREATE UNIQUE INDEX .*?;',sql))+'\n'
(OUT/'SIAJ_Inventario_Datos_v1.md').write_text(inventory,encoding='utf-8')
(OUT/'especificacion.json').write_text(json.dumps(dict(layouts=layouts,catalog=catalog,gaps=gaps,excluded=excluded),ensure_ascii=False,indent=2),encoding='utf-8')
for l in layouts:
    if l['file'].endswith('.json'):
        values={c['name']:(False if c['type']=='Booleano' else c['example']) for c in l['cols']}
        (OUT/l['file']).write_text(json.dumps([values],ensure_ascii=False,indent=2),encoding='utf-8')
(OUT/'fuentes_sha256.json').write_text(json.dumps({s:hashlib.sha256((ROOT/s).read_bytes()).hexdigest() for s in sources},indent=2),encoding='utf-8')
print(json.dumps({'layouts':len(layouts),'tablas_inventario':len(tables),'campos_captura':sum(len(l['cols']) for l in layouts),'documento':str(OUT/'SIAJ_Layouts_Carga_Masiva_v1.md')},ensure_ascii=False))

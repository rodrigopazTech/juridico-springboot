/**
 * CalendarioModule.js
 * Gestiona la lógica de visualización y eventos del calendario jurídico.
 */
export class CalendarioModule {
    constructor() {
        this.currentDate = new Date();
        this.view = 'month'; // 'day', 'week', 'month'
        this.events = [];
        this.names = ["Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"];
        this.dayNames = ["Domingo", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado"];
        
        // Configuración de filtros
        this.filters = {
            tipo: 'todos',
            gerenciaId: null,
            usuarioId: null
        };
        
        // Permisos del usuario
        this.userPermissions = {
            puedeVerGerencia: false,
            puedeVerUsuario: false,
            userRole: 'ANONYMOUS',
            userGerenciaId: null
        };
    }

    async init() {
        console.log("Iniciando Calendario Jurídico...");
        
        // Cargar permisos del usuario
        await this.loadUserPermissions();
        
        // Configurar filtros según permisos
        this.setupFilters();
        
        this.setupEventListeners();
        await this.loadEvents();
        this.render();
    }

    async loadUserPermissions() {
        try {
            const response = await fetch('/api/calendario/usuario-actual');
            if (response.ok) {
                const userInfo = await response.json();
                if (userInfo) {
                    // Intentar obtener permisos desde el objeto window
                    if (window.calendarioFilters) {
                        this.userPermissions = window.calendarioFilters;
                    }
                    
                    // Si es GERENTE, setear su gerencia por defecto
                    if (this.userPermissions.userRole === 'GERENTE' && this.userPermissions.userGerenciaId) {
                        this.filters.gerenciaId = this.userPermissions.userGerenciaId;
                    }
                }
            }
        } catch (error) {
            console.warn("No se pudo cargar permisos del usuario:", error);
        }
    }

    setupFilters() {
        // Cargar gerencias si el usuario tiene permiso
        if (this.userPermissions.puedeVerGerencia) {
            this.loadGerencias();
        }
        
        // Cargar usuarios si el usuario tiene permiso
        if (this.userPermissions.puedeVerUsuario) {
            // Si es GERENTE, cargar solo usuarios de su gerencia
            const gerenciaId = this.userPermissions.userRole === 'GERENTE' 
                ? this.userFilters.gerenciaId 
                : null;
            this.loadUsuarios(gerenciaId);
        }
    }

    async loadGerencias() {
        try {
            const response = await fetch('/api/calendario/gerencias');
            if (response.ok) {
                const gerencias = await response.json();
                const select = document.getElementById('filterGerencia');
                if (select && gerencias.length > 0) {
                    select.innerHTML = '<option value="">Todas las gerencias</option>';
                    gerencias.forEach(g => {
                        const option = document.createElement('option');
                        option.value = g.id;
                        option.textContent = g.nombre;
                        select.appendChild(option);
                    });
                }
            }
        } catch (error) {
            console.warn("No se pudieron cargar gerencias:", error);
        }
    }

    async loadUsuarios(gerenciaId) {
        try {
            let url = '/api/calendario/usuarios';
            if (gerenciaId) {
                url += `?gerenciaId=${gerenciaId}`;
            }
            
            const response = await fetch(url);
            if (response.ok) {
                const usuarios = await response.json();
                const select = document.getElementById('filterUsuario');
                if (select && usuarios.length > 0) {
                    select.innerHTML = '<option value="">Todos los usuarios</option>';
                    usuarios.forEach(u => {
                        const option = document.createElement('option');
                        option.value = u.id;
                        option.textContent = u.nombreCompleto || u.nombre;
                        select.appendChild(option);
                    });
                }
            }
        } catch (error) {
            console.warn("No se pudieron cargar usuarios:", error);
        }
    }

    setupEventListeners() {
        // Selector de vistas (Día, Semana, Mes)
        document.querySelectorAll('.view-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                this.view = e.target.dataset.view;
                this.updateViewButtons(e.target);
                this.render();
            });
        });

        // Botones de navegación (Anterior, Siguiente)
        document.getElementById('btnPrev')?.addEventListener('click', () => this.navigate(-1));
        document.getElementById('btnNext')?.addEventListener('click', () => this.navigate(1));

        // Filtros
        document.getElementById('btnApplyFilters')?.addEventListener('click', () => this.applyFilters());
        document.getElementById('btnClearFilters')?.addEventListener('click', () => this.clearFilters());
        
        // Cambiar usuarios cuando cambia gerencia
        document.getElementById('filterGerencia')?.addEventListener('change', (e) => {
            if (this.userPermissions.puedeVerUsuario) {
                this.loadUsuarios(e.target.value);
            }
        });
    }

    applyFilters() {
        // Actualizar filtros
        const tipoSelect = document.getElementById('filterTipo');
        const gerenciaSelect = document.getElementById('filterGerencia');
        const usuarioSelect = document.getElementById('filterUsuario');
        
        this.filters.tipo = tipoSelect ? tipoSelect.value : 'todos';
        
        // Solo actualizar gerencia si el usuario tiene permiso
        if (this.userPermissions.puedeVerGerencia && gerenciaSelect) {
            this.filters.gerenciaId = gerenciaSelect.value ? parseInt(gerenciaSelect.value) : null;
        }
        
        // Solo actualizar usuario si el usuario tiene permiso
        if (this.userPermissions.puedeVerUsuario && usuarioSelect) {
            this.filters.usuarioId = usuarioSelect.value ? parseInt(usuarioSelect.value) : null;
        }
        
        // Si es ABOGADO, siempre filtrar por sí mismo
        if (this.userPermissions.userRole === 'ABOGADO') {
            this.filters.usuarioId = this.userPermissions.userGerenciaId; // userGerenciaId contiene el ID del usuario en este caso
        }
        
        // Recargar eventos con filtros
        this.loadEvents();
    }

    clearFilters() {
        // Resetear filtros
        const tipoSelect = document.getElementById('filterTipo');
        const gerenciaSelect = document.getElementById('filterGerencia');
        const usuarioSelect = document.getElementById('filterUsuario');
        
        if (tipoSelect) tipoSelect.value = 'todos';
        if (gerenciaSelect) gerenciaSelect.value = '';
        if (usuarioSelect) usuarioSelect.value = '';
        
        // Resetear filtros internos
        this.filters = {
            tipo: 'todos',
            gerenciaId: null,
            usuarioId: null
        };
        
        // Si es GERENTE, setear su gerencia por defecto
        if (this.userPermissions.userRole === 'GERENTE' && this.userPermissions.userGerenciaId) {
            this.filters.gerenciaId = this.userPermissions.userGerenciaId;
        }
        
        // Recargar eventos
        this.loadEvents();
    }

    async loadEvents() {
        try {
            // Construir URL con parámetros de filtro
            let url = '/api/calendario/eventos?tipo=' + this.filters.tipo;
            
            if (this.filters.gerenciaId) {
                url += '&gerenciaId=' + this.filters.gerenciaId;
            }
            
            if (this.filters.usuarioId) {
                url += '&usuarioId=' + this.filters.usuarioId;
            }
            
            const response = await fetch(url);
            if (!response.ok) throw new Error("Error al obtener eventos");
            this.events = await response.json();
            console.log("Eventos cargados:", this.events.length);
        } catch (error) {
            console.error("Error cargando eventos:", error);
            this.events = [];
        }
    }

    updateViewButtons(activeBtn) {
        document.querySelectorAll('.view-btn').forEach(btn => {
            btn.classList.remove('bg-white', 'shadow-sm');
        });
        activeBtn.classList.add('bg-white', 'shadow-sm');
    }

    render() {
        // Ocultar todos los contenedores de vista
        document.querySelectorAll('.calendar-view').forEach(v => v.classList.add('hidden'));

        // Renderizar según la vista activa
        if (this.view === 'month') {
            document.getElementById('calendarMonthView').classList.remove('hidden');
            this.renderMonth();
        } else if (this.view === 'week') {
            document.getElementById('calendarWeekView').classList.remove('hidden');
            this.renderWeek();
        } else if (this.view === 'day') {
            document.getElementById('calendarDayView').classList.remove('hidden');
            this.renderDay();
        }
    }

    renderMonth() {
        const grid = document.getElementById('monthGrid');
        const periodLabel = document.getElementById('currentPeriod');
        if (!grid || !periodLabel) return;

        grid.innerHTML = '';
        const year = this.currentDate.getFullYear();
        const month = this.currentDate.getMonth();

        periodLabel.innerText = `${this.names[month]} ${year}`;

        // Lógica de fechas
        const firstDay = new Date(year, month, 1).getDay();
        const daysInMonth = new Date(year, month + 1, 0).getDate();
        const startingDay = firstDay === 0 ? 6 : firstDay - 1; // Ajuste para que empieze en Lunes

        // Días del mes anterior (huecos)
        for (let i = 0; i < startingDay; i++) {
            grid.appendChild(this.createDayCell('', false, false));
        }

        // Días del mes actual
        for (let day = 1; day <= daysInMonth; day++) {
            const dateStr = `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
            
            const isToday = day === new Date().getDate() && 
                            month === new Date().getMonth() && 
                            year === new Date().getFullYear();

            const cell = this.createDayCell(day, true, isToday);
            
            // Filtrar eventos de este día
            const dayEvents = this.events.filter(e => e.fecha === dateStr);
            const eventsContainer = cell.querySelector('.events-container');

            dayEvents.forEach(event => {
                const eventEl = this.createEventElement(event);
                eventsContainer.appendChild(eventEl);
            });

            grid.appendChild(cell);
        }
    }

    renderWeek() {
        const periodLabel = document.getElementById('currentPeriod');
        const hoursColumn = document.getElementById('hoursColumn');
        const weekDays = document.getElementById('weekDays');
        
        if (!periodLabel || !hoursColumn || !weekDays) return;

        const year = this.currentDate.getFullYear();
        const month = this.currentDate.getMonth();
        const dayOfWeek = this.currentDate.getDay();
        
        // Calcular fecha del lunes de la semana actual
        const startOfWeek = new Date(year, month, this.currentDate.getDate() - (dayOfWeek === 0 ? 6 : dayOfWeek - 1));
        const endOfWeek = new Date(year, month, startOfWeek.getDate() + 6);

        // Actualizar etiqueta del período
        periodLabel.innerText = `${startOfWeek.getDate()} - ${endOfWeek.getDate()} ${this.names[month]} ${year}`;

        // Generar columna de horas (6:00 AM a 10:00 PM)
        hoursColumn.innerHTML = '';
        for (let hour = 6; hour <= 22; hour++) {
            const hourDiv = document.createElement('div');
            hourDiv.className = 'h-20 border-b border-gray-100 text-xs text-gray-500 text-center py-1';
            hourDiv.innerText = `${hour.toString().padStart(2, '0')}:00`;
            hoursColumn.appendChild(hourDiv);
        }

        // Generar columnas de días
        weekDays.innerHTML = '';
        for (let i = 0; i < 7; i++) {
            const currentDay = new Date(startOfWeek);
            currentDay.setDate(startOfWeek.getDate() + i);
            
            const dateStr = this.formatDate(currentDay);
            const isToday = this.isToday(currentDay);
            const dayEvents = this.events.filter(e => e.fecha === dateStr);

            const dayColumn = document.createElement('div');
            dayColumn.className = 'relative border-r border-gray-200';
            
            // Header del día
            const dayHeader = document.createElement('div');
            dayHeader.className = `sticky top-0 bg-white z-10 border-b border-gray-100 p-2 text-center ${isToday ? 'bg-blue-50' : ''}`;
            dayHeader.innerHTML = `
                <div class="text-xs text-gray-500">${['Lun','Mar','Mié','Jue','Vie','Sáb','Dom'][i]}</div>
                <div class="text-lg font-bold ${isToday ? 'text-gob-guinda' : 'text-gray-700'}">${currentDay.getDate()}</div>
            `;
            dayColumn.appendChild(dayHeader);

            // Contenedor de eventos del día
            const eventsContainer = document.createElement('div');
            eventsContainer.className = 'p-1 space-y-1';
            
            dayEvents.forEach(event => {
                const eventEl = this.createEventElement(event);
                eventEl.classList.add('text-xs', 'mb-1');
                eventsContainer.appendChild(eventEl);
            });

            dayColumn.appendChild(eventsContainer);
            weekDays.appendChild(dayColumn);
        }
    }

    renderDay() {
        const dayTitle = document.getElementById('dayTitle');
        const daySubtitle = document.getElementById('daySubtitle');
        const dayEventsContainer = document.getElementById('dayEventsContainer');
        
        if (!dayTitle || !daySubtitle || !dayEventsContainer) return;

        const year = this.currentDate.getFullYear();
        const month = this.currentDate.getMonth();
        const day = this.currentDate.getDate();
        const dateStr = this.formatDate(this.currentDate);
        const dayOfWeek = this.currentDate.getDay();

        // Actualizar título
        dayTitle.innerText = this.names[month];
        daySubtitle.innerText = `${this.dayNames[dayOfWeek]} ${day} de ${year}`;

        // Filtrar eventos del día
        const dayEvents = this.events.filter(e => e.fecha === dateStr);
        
        // Ordenar eventos por hora
        dayEvents.sort((a, b) => {
            const timeA = a.hora || '00:00';
            const timeB = b.hora || '00:00';
            return timeA.localeCompare(timeB);
        });

        // Renderizar eventos
        dayEventsContainer.innerHTML = '';
        
        if (dayEvents.length === 0) {
            dayEventsContainer.innerHTML = `
                <div class="text-center py-12 text-gray-500">
                    <i class="fas fa-calendar-times text-4xl mb-3 text-gray-300"></i>
                    <p class="text-lg">No hay eventos para este día</p>
                </div>
            `;
        } else {
            dayEvents.forEach(event => {
                const eventCard = document.createElement('div');
                eventCard.className = 'bg-white rounded-lg border border-gray-200 p-4 hover:shadow-md transition-shadow cursor-pointer';
                eventCard.onclick = () => this.showEventDetail(event);
                
                eventCard.innerHTML = `
                    <div class="flex items-start gap-4">
                        <div class="w-1 h-full rounded-full event-${event.tipo} self-stretch"></div>
                        <div class="flex-1">
                            <div class="flex justify-between items-start">
                                <h4 class="font-bold text-gray-800 text-lg">${event.titulo}</h4>
                                <span class="event-tag event-${event.tipo} text-xs px-2 py-1">${event.tipo}</span>
                            </div>
                            <div class="mt-2 space-y-1 text-sm text-gray-600">
                                <div class="flex items-center gap-2">
                                    <i class="fas fa-clock text-gray-400 w-4"></i>
                                    <span>${event.hora || 'Sin hora definida'}</span>
                                </div>
                                ${event.expediente ? `
                                <div class="flex items-center gap-2">
                                    <i class="fas fa-folder text-gray-400 w-4"></i>
                                    <span>${event.expediente}</span>
                                </div>
                                ` : ''}
                                ${event.usuarioNombre ? `
                                <div class="flex items-center gap-2">
                                    <i class="fas fa-user text-gray-400 w-4"></i>
                                    <span>${event.usuarioNombre}</span>
                                </div>
                                ` : ''}
                            </div>
                        </div>
                    </div>
                `;
                dayEventsContainer.appendChild(eventCard);
            });
        }
    }

    createDayCell(day, isCurrentMonth, isToday) {
        const div = document.createElement('div');
        div.className = `calendar-day-cell ${isCurrentMonth ? '' : 'day-off'} ${isToday ? 'day-today' : ''}`;
        
        div.innerHTML = `
            <div class="flex justify-end">
                <span class="day-number-circle ${isToday ? 'today-circle' : 'text-gray-500'}">
                    ${day}
                </span>
            </div>
            <div class="events-container"></div>
        `;
        return div;
    }

    createEventElement(event) {
        const eventEl = document.createElement('div');
        eventEl.className = `event-tag event-${event.tipo}`;
        eventEl.innerText = event.titulo;
        eventEl.title = event.titulo;
        eventEl.onclick = (e) => {
            e.stopPropagation();
            this.showEventDetail(event);
        };
        return eventEl;
    }

    showEventDetail(event) {
        const modal = document.getElementById('modalEventDetail');
        const content = document.getElementById('modalContent');
        if (!modal) return;

        document.getElementById('modalTitle').innerText = event.titulo;
        document.getElementById('modalDate').innerText = event.fecha;
        document.getElementById('modalTime').innerText = event.hora || '--:--';
        
        const typeBadge = document.getElementById('modalType');
        typeBadge.innerText = event.tipo;
        typeBadge.className = `px-2 py-1 rounded text-xs font-bold uppercase event-${event.tipo}`;

        // Mostrar información extra según tipo
        let extraInfoHtml = '';
        if (event.expediente) {
            extraInfoHtml += `<p class="text-sm"><strong>Expediente:</strong> ${event.expediente}</p>`;
        }
        if (event.usuarioNombre) {
            extraInfoHtml += `<p class="text-sm"><strong>Usuario:</strong> ${event.usuarioNombre}</p>`;
        }
        if (event.gerenciaNombre) {
            extraInfoHtml += `<p class="text-sm"><strong>Gerencia:</strong> ${event.gerenciaNombre}</p>`;
        }
        
        const extraInfo = document.getElementById('modalExtraInfo');
        extraInfo.innerHTML = extraInfoHtml;

        modal.classList.remove('hidden');
        modal.classList.add('flex');
        setTimeout(() => content.classList.remove('scale-95', 'opacity-0'), 10);
    }

    closeModal() {
        const modal = document.getElementById('modalEventDetail');
        const content = document.getElementById('modalContent');
        content.classList.add('scale-95', 'opacity-0');
        setTimeout(() => {
            modal.classList.add('hidden');
            modal.classList.remove('flex');
        }, 300);
    }

    navigate(direction) {
        if (this.view === 'month') {
            this.currentDate.setMonth(this.currentDate.getMonth() + direction);
        } else if (this.view === 'week') {
            this.currentDate.setDate(this.currentDate.getDate() + (direction * 7));
        } else {
            this.currentDate.setDate(this.currentDate.getDate() + direction);
        }
        this.render();
    }

    formatDate(date) {
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`;
    }

    isToday(date) {
        const today = new Date();
        return date.getDate() === today.getDate() &&
               date.getMonth() === today.getMonth() &&
               date.getFullYear() === today.getFullYear();
    }
}


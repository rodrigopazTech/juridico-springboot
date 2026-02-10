/**
 * CalendarioModule.js
 * Gestiona la lógica de visualización, navegación y filtrado.
 */
export class CalendarioModule {
    constructor() {
        this.currentDate = new Date();
        this.view = 'month'; // 'day', 'week', 'month'
        this.events = [];
        this.filteredEvents = [];
        this.names = ["Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"];
    }

    async init() {
        console.log("Iniciando Calendario...");
        this.setupEventListeners();
        await this.loadEvents();
        this.render();
    }

    setupEventListeners() {
        // Navegación (Anterior/Siguiente)
        document.getElementById('btnPrev')?.addEventListener('click', () => this.navigate(-1));
        document.getElementById('btnNext')?.addEventListener('click', () => this.navigate(1));

        // Selector de Vistas
        document.querySelectorAll('.view-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                this.view = e.target.dataset.view;
                this.updateViewButtons(e.target);
                this.render();
            });
        });

        // Botón Aplicar Filtros
        document.getElementById('btnApplyFilters')?.addEventListener('click', () => {
            this.applyFilters();
        });

        // Botón Limpiar Filtros
        document.getElementById('btnClearFilters')?.addEventListener('click', () => {
            document.getElementById('filterTipo').value = 'todos';
            const filterGerencia = document.getElementById('filterGerencia');
            if (filterGerencia && !filterGerencia.disabled) {
                filterGerencia.value = 'todos';
            }
            const filterUsuario = document.getElementById('filterUsuario');
            if (filterUsuario) {
                filterUsuario.value = 'todos';
            }
            this.filteredEvents = [...this.events];
            this.render();
        });
    }

    async loadEvents() {
        try {
            const response = await fetch('/api/calendario/eventos');
            if (!response.ok) throw new Error("Error en la respuesta del servidor");
            this.events = await response.json();
            this.filteredEvents = [...this.events];
        } catch (error) {
            console.error("Error cargando eventos:", error);
            this.events = [];
        }
    }

    applyFilters() {
        const tipoSelected = document.getElementById('filterTipo').value;
        const gerenciaSelected = document.getElementById('filterGerencia')?.value || 'todos';
        const usuarioSelected = document.getElementById('filterUsuario')?.value || 'todos';

        this.filteredEvents = this.events.filter(event => {
            const matchTipo = tipoSelected === 'todos' || event.tipo === tipoSelected;
            // Si el select de gerencia no existe o es 'todos', pasa. Si no, compara strings.
            const matchGerencia = gerenciaSelected === 'todos' || event.gerenciaNombre === gerenciaSelected;
            // Filtro por usuario
            const matchUsuario = usuarioSelected === 'todos' || 
                (event.usuarioId && event.usuarioId.toString() === usuarioSelected);
            
            return matchTipo && matchGerencia && matchUsuario;
        });

        console.log(`Filtrado: ${this.filteredEvents.length} eventos encontrados.`);
        this.render();
    }

    updateViewButtons(activeBtn) {
        document.querySelectorAll('.view-btn').forEach(btn => {
            btn.classList.remove('bg-white', 'shadow-sm');
        });
        activeBtn.classList.add('bg-white', 'shadow-sm');
    }

    render() {
        // Ocultar todas las vistas
        document.querySelectorAll('.calendar-view').forEach(v => v.classList.add('hidden'));

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

        const firstDay = new Date(year, month, 1).getDay();
        const daysInMonth = new Date(year, month + 1, 0).getDate();
        
        // Ajuste para calendario que empieza en Lunes (ISO)
        const startingDay = firstDay === 0 ? 6 : firstDay - 1;

        // Celdas vacías (mes anterior)
        for (let i = 0; i < startingDay; i++) {
            grid.appendChild(this.createDayCell('', false, false));
        }

        // Días del mes actual
        for (let day = 1; day <= daysInMonth; day++) {
            const dateStr = `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
            const isToday = day === new Date().getDate() && month === new Date().getMonth() && year === new Date().getFullYear();
            
            const cell = this.createDayCell(day, true, isToday);
            const eventsContainer = cell.querySelector('.events-container');

            // Filtrar eventos de este día específico (de los ya filtrados por el combo)
            const dayEvents = this.filteredEvents.filter(e => e.fecha === dateStr);

            dayEvents.forEach(event => {
                const eventEl = document.createElement('div');
                eventEl.className = `event-tag event-${event.tipo}`;
                eventEl.innerText = event.titulo;
                eventEl.onclick = (e) => {
                    e.stopPropagation();
                    this.showEventDetail(event);
                };
                eventsContainer.appendChild(eventEl);
            });

            grid.appendChild(cell);
        }
    }

    renderWeek() {
        const periodLabel = document.getElementById('currentPeriod');
        if (!periodLabel) return;

        // Calcular inicio de la semana (Lunes)
        const year = this.currentDate.getFullYear();
        const month = this.currentDate.getMonth();
        const day = this.currentDate.getDate();
        const currentDayOfWeek = new Date(year, month, day).getDay();
        const mondayOffset = currentDayOfWeek === 0 ? -6 : 1 - currentDayOfWeek;
        const monday = new Date(year, month, day + mondayOffset);

        // Actualizar label del período
        const sunday = new Date(monday);
        sunday.setDate(monday.getDate() + 6);
        
        const formatDate = (d) => {
            return `${d.getDate()}/${d.getMonth() + 1}/${d.getFullYear()}`;
        };
        
        periodLabel.innerText = `${formatDate(monday)} - ${formatDate(sunday)}`;

        // Generar columna de horas
        const hoursColumn = document.getElementById('hoursColumn');
        if (hoursColumn) {
            hoursColumn.innerHTML = '';
            for (let hour = 0; hour < 24; hour++) {
                const hourCell = document.createElement('div');
                hourCell.className = 'hour-cell border-b border-gray-200 text-xs text-gray-500 text-center py-2';
                hourCell.innerText = `${String(hour).padStart(2, '0')}:00`;
                hourCell.style.height = '60px';
                hourCell.style.minHeight = '60px';
                hoursColumn.appendChild(hourCell);
            }
        }

        // Generar columnas de días
        const weekDays = document.getElementById('weekDays');
        if (weekDays) {
            weekDays.innerHTML = '';
            const dayNames = ['Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado', 'Domingo'];
            
            for (let i = 0; i < 7; i++) {
                const currentDate = new Date(monday);
                currentDate.setDate(monday.getDate() + i);
                const dateStr = currentDate.toISOString().split('T')[0];
                const isToday = this.isToday(dateStr);
                const dayNumber = currentDate.getDate();
                const monthName = this.names[currentDate.getMonth()];
                
                const dayColumn = document.createElement('div');
                dayColumn.className = 'day-column relative border-r border-gray-200';
                
                // Header del día
                const dayHeader = document.createElement('div');
                dayHeader.className = `day-header text-center py-2 border-b border-gray-200 sticky top-0 bg-white z-10 ${isToday ? 'bg-red-50' : ''}`;
                dayHeader.innerHTML = `
                    <div class="text-xs font-bold text-gray-500 uppercase">${dayNames[i]}</div>
                    <div class="text-lg font-bold ${isToday ? 'text-gob-guinda' : 'text-gray-700'}">${dayNumber}</div>
                    <div class="text-xs text-gray-400">${monthName}</div>
                `;
                dayColumn.appendChild(dayHeader);

                // Container de eventos por hora
                const dayContent = document.createElement('div');
                dayContent.className = 'day-content relative';
                dayContent.style.height = '1440px'; // 24 * 60px
                
                // Filtrar eventos de este día
                const dayEvents = this.filteredEvents.filter(e => e.fecha === dateStr);
                
                dayEvents.forEach(event => {
                    const eventEl = document.createElement('div');
                    eventEl.className = `event-tag event-${event.tipo} absolute left-1 right-1 rounded px-1 py-0.5 text-xs cursor-pointer z-5`;
                    
                    // Parsear hora
                    let hour = 0;
                    let minute = 0;
                    if (event.hora && event.hora.includes(':')) {
                        const parts = event.hora.split(':');
                        hour = parseInt(parts[0], 10);
                        minute = parseInt(parts[1], 10);
                    }
                    
                    // Calcular posición
                    const top = (hour * 60 + minute) * (60 / 60); // 60px por hora
                    const height = event.tipo === 'audiencia' ? 60 : 40;
                    
                    eventEl.style.top = `${top}px`;
                    eventEl.style.height = `${height}px`;
                    eventEl.innerHTML = `
                        <div class="font-semibold truncate">${event.titulo}</div>
                        <div class="text-xs opacity-75">${event.hora}</div>
                    `;
                    
                    eventEl.onclick = (e) => {
                        e.stopPropagation();
                        this.showEventDetail(event);
                    };
                    
                    dayContent.appendChild(eventEl);
                });
                
                dayColumn.appendChild(dayContent);
                weekDays.appendChild(dayColumn);
            }
        }
    }

    renderDay() {
        const year = this.currentDate.getFullYear();
        const month = this.currentDate.getMonth();
        const day = this.currentDate.getDate();
        const dateStr = `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
        
        const dayTitle = document.getElementById('dayTitle');
        const daySubtitle = document.getElementById('daySubtitle');
        const dayEventsContainer = document.getElementById('dayEventsContainer');
        
        if (dayTitle) {
            const today = new Date();
            const isTodayDate = this.isToday(dateStr);
            const dayNames = ['Domingo', 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado'];
            const currentDayOfWeek = new Date(year, month, day).getDay();
            
            dayTitle.innerText = dayNames[currentDayOfWeek] + ' ' + day;
            dayTitle.className = `font-bold text-lg ${isTodayDate ? 'text-gob-guinda' : 'text-gray-700'}`;
        }
        
        if (daySubtitle) {
            daySubtitle.innerText = this.names[month] + ' ' + year;
        }
        
        if (dayEventsContainer) {
            dayEventsContainer.innerHTML = '';
            
            // Filtrar eventos del día
            const dayEvents = this.filteredEvents.filter(e => e.fecha === dateStr);
            
            if (dayEvents.length === 0) {
                dayEventsContainer.innerHTML = `
                    <div class="text-center text-gray-500 py-8">
                        <i class="fas fa-calendar-times text-4xl mb-4"></i>
                        <p>No hay eventos para este día</p>
                    </div>
                `;
                return;
            }
            
            // Ordenar por hora
            dayEvents.sort((a, b) => {
                if (!a.hora) return 1;
                if (!b.hora) return -1;
                return a.hora.localeCompare(b.hora);
            });
            
            // Mostrar timeline con eventos
            const timeline = document.createElement('div');
            timeline.className = 'relative border-l-2 border-gob-guinda ml-4 space-y-6';
            
            dayEvents.forEach(event => {
                const eventItem = document.createElement('div');
                eventItem.className = 'relative pl-6';
                
                // Punto de la timeline
                const dot = document.createElement('div');
                dot.className = `absolute -left-[9px] top-0 w-4 h-4 rounded-full border-2 border-white ${this.getEventColor(event.tipo)}`;
                
                // Contenido del evento
                const content = document.createElement('div');
                content.className = 'bg-white border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow cursor-pointer';
                content.onclick = () => this.showEventDetail(event);
                
                content.innerHTML = `
                    <div class="flex items-start justify-between mb-2">
                        <div class="flex items-center gap-2">
                            <span class="px-2 py-1 text-xs font-semibold rounded ${this.getEventBadgeClass(event.tipo)}">${event.tipo.toUpperCase()}</span>
                            <span class="text-sm font-bold text-gray-700">${event.hora || '--:--'}</span>
                        </div>
                    </div>
                    <h4 class="font-semibold text-gray-800 mb-1">${event.titulo}</h4>
                    ${event.expediente ? `<p class="text-sm text-gray-500">Expediente: ${event.expediente}</p>` : ''}
                    ${event.usuarioNombre ? `<p class="text-sm text-gray-500">Responsable: ${event.usuarioNombre}</p>` : ''}
                    ${event.gerenciaNombre ? `<p class="text-sm text-gray-500">Gerencia: ${event.gerenciaNombre}</p>` : ''}
                `;
                
                eventItem.appendChild(dot);
                eventItem.appendChild(content);
                timeline.appendChild(eventItem);
            });
            
            dayEventsContainer.appendChild(timeline);
        }
    }

    isToday(dateStr) {
        const today = new Date();
        const todayStr = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;
        return dateStr === todayStr;
    }

    getEventColor(tipo) {
        switch (tipo) {
            case 'audiencia': return 'bg-red-500';
            case 'termino': return 'bg-yellow-500';
            case 'recordatorio': return 'bg-blue-500';
            default: return 'bg-gray-500';
        }
    }

    getEventBadgeClass(tipo) {
        switch (tipo) {
            case 'audiencia': return 'bg-red-100 text-red-700';
            case 'termino': return 'bg-yellow-100 text-yellow-700';
            case 'recordatorio': return 'bg-blue-100 text-blue-700';
            default: return 'bg-gray-100 text-gray-700';
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

    navigate(direction) {
        if (this.view === 'month') {
            this.currentDate.setMonth(this.currentDate.getMonth() + direction);
        } else if (this.view === 'week') {
            this.currentDate.setDate(this.currentDate.getDate() + (direction * 7));
        } else if (this.view === 'day') {
            this.currentDate.setDate(this.currentDate.getDate() + direction);
        }
        this.render();
    }

    showEventDetail(event) {
        const modal = document.getElementById('modalEventDetail');
        if (!modal) return;
        
        // Título
        document.getElementById('modalTitle').innerText = 'Detalle del Evento';
        
        // Tipo de evento con color
        const modalType = document.getElementById('modalType');
        modalType.innerText = event.tipo || 'Sin tipo';
        modalType.className = `px-3 py-1 rounded-full text-xs font-bold uppercase text-white ${this.getEventBadgeClass(event.tipo)}`;
        
        // Título del evento
        document.getElementById('modalTitulo').innerText = event.titulo || 'Sin título';
        
        // Fecha
        document.getElementById('modalDate').innerText = this.formatDate(event.fecha);
        
        // Hora
        document.getElementById('modalTime').innerText = event.hora || '--:--';
        
        // Expediente
        const expedienteSection = document.getElementById('modalExpedienteSection');
        const modalExpediente = document.getElementById('modalExpediente');
        if (event.expediente) {
            expedienteSection.classList.remove('hidden');
            modalExpediente.innerText = event.expediente;
        } else {
            expedienteSection.classList.add('hidden');
        }
        
        // Responsable
        const usuarioSection = document.getElementById('modalUsuarioSection');
        const modalUsuario = document.getElementById('modalUsuario');
        if (event.usuarioNombre) {
            usuarioSection.classList.remove('hidden');
            modalUsuario.innerText = event.usuarioNombre;
        } else {
            usuarioSection.classList.add('hidden');
        }
        
        // Gerencia
        const gerenciaSection = document.getElementById('modalGerenciaSection');
        const modalGerencia = document.getElementById('modalGerencia');
        if (event.gerenciaNombre) {
            gerenciaSection.classList.remove('hidden');
            modalGerencia.innerText = event.gerenciaNombre;
        } else {
            gerenciaSection.classList.add('hidden');
        }
        
        // Detalles
        const detallesSection = document.getElementById('modalDetallesSection');
        const modalDetalles = document.getElementById('modalDetalles');
        if (event.detalles) {
            detallesSection.classList.remove('hidden');
            modalDetalles.innerText = event.detalles;
        } else {
            detallesSection.classList.add('hidden');
        }
        
        // Mostrar modal
        modal.classList.remove('hidden');
        
        // Animación de entrada
        const content = document.getElementById('modalContent');
        if (content) {
            content.classList.remove('scale-95', 'opacity-0');
        }
    }

    closeModal() {
        const modal = document.getElementById('modalEventDetail');
        if (!modal) return;
        
        const content = document.getElementById('modalContent');
        if (content) {
            content.classList.add('scale-95', 'opacity-0');
        }
        
        setTimeout(() => {
            modal.classList.add('hidden');
        }, 200);
    }

    formatDate(dateStr) {
        if (!dateStr) return 'Sin fecha';
        
        try {
            const parts = dateStr.split('-');
            if (parts.length === 3) {
                const date = new Date(parts[0], parts[1] - 1, parts[2]);
                const options = { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' };
                return date.toLocaleDateString('es-ES', options);
            }
        } catch (e) {
            return dateStr;
        }
        
        return dateStr;
    }
}
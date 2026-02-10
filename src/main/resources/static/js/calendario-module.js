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
        } else {
            console.warn(`Vista ${this.view} no implementada.`);
            // Aquí podrías llamar a renderWeek() o renderDay()
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
        }
        this.render();
    }

    showEventDetail(event) {
        // Asumiendo que usas el modal de detalle proporcionado anteriormente
        const modal = document.getElementById('modalEventDetail');
        if (!modal) return;
        
        document.getElementById('modalTitle').innerText = event.titulo;
        document.getElementById('modalDate').innerText = event.fecha;
        document.getElementById('modalTime').innerText = event.hora || '--:--';
        document.getElementById('modalType').innerText = event.tipo.toUpperCase();
        
        modal.classList.remove('hidden');
        modal.classList.add('flex');
    }
}
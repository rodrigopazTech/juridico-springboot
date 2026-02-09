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
    }

    async init() {
        console.log("Iniciando Calendario Jurídico...");
        this.setupEventListeners();
        await this.loadEvents();
        this.render();
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
    }

    async loadEvents() {
        try {
            // Llamada a tu CalendarioRestController
            const response = await fetch('/api/calendario/eventos');
            if (!response.ok) throw new Error("Error al obtener eventos");
            this.events = await response.json();
        } catch (error) {
            console.error("Error cargando eventos:", error);
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
        const startingDay = firstDay === 0 ? 6 : firstDay - 1; // Ajuste para que empiece en Lunes

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
                const eventEl = document.createElement('div');
                eventEl.className = `event-tag event-${event.tipo}`;
                eventEl.innerText = event.titulo;
                eventEl.title = event.titulo;
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
        const extraInfo = document.getElementById('modalExtraInfo');
        extraInfo.innerHTML = event.expediente ? 
            `<p class="text-sm"><strong>Expediente:</strong> ${event.expediente}</p>` : '';

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

    // Placeholders para otras vistas
    renderWeek() { console.log("Vista semanal no implementada aún."); }
    renderDay() { console.log("Vista diaria no implementada aún."); }
}
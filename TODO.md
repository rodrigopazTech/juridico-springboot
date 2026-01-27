# TODO: Implementar módulo de dashboard

## Información recopilada
- DashboardController.java proporciona datos mock para KPIs (EstadisticasResponse) y gráfica (MetricasResponse con etiquetas, valores, metricaNombre).
- index.html de dashboard muestra KPIs y dos gráficas: una de pastel para "Estatus Global de Expedientes" y una de línea para "Trabajo Completado".
- dashboard.js lee datos desde elementos HTML inyectados por Thymeleaf.
- Referencia: módulo de términos usa estructura similar con fragments, pero dashboard es más simple con gráficas.

## Plan
- [x] Actualizar dashboard.js para leer datos de dashboardData desde elemento HTML (dashboard-data) para las gráficas.
- [x] Verificar que index.html use correctamente los datos del controller (kpis, dashboardData).
- [x] Ajustar estructura de gráficas para que coincida con el controller.

## Archivos dependientes
- src/main/resources/templates/views/dashboard/index.html
- src/main/resources/static/js/dashboard.js

## Pasos de seguimiento
- [] Probar que las gráficas se rendericen correctamente con los datos del controller.
- [] Verificar que los KPIs se muestren correctamente.

tailwind.config = {
  theme: {
    extend: {
      colors: {
        'gob-guinda': '#691228',
        'gob-guindaDark': '#4e0c1d',
        'gob-oro': '#D4C19C',
        'gob-gris': '#545454',
        'gob-plata': '#98989A',
        'gob-verde': '#13322B',
        'gob-verdeDark': '#0C231E',
        'gob-fondo': '#F5F5F5',
        // Colores para eventos del calendario
        'evento-audiencia': '#691228',  // Gob Guinda
        'evento-termino': '#13322B',    // Gob Verde
        'evento-recordatorio': '#D4C19C' // Gob Oro
      },
      fontFamily: {
        'headings': ['Montserrat', 'sans-serif'],
        'sans': ['Noto Sans', 'ui-sans-serif', 'system-ui'],
      },
    },
  },
}

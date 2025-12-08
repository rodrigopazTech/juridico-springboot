/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ["./src/main/resources/templates/**/*.html"], 
  theme: {
    extend: {
      // AQUÍ AGREGAMOS LOS COLORES PERSONALIZADOS
      colors: {
        gob: {
          guinda: '#9D2449',
          guindaDark: '#611232',
          oro: '#B38E5D',
          oroLight: '#DDC9A3',
          gris: '#545454',
          plata: '#98989A',
          verde: '#13322B',
          verdeDark: '#0C231E'
        }
      },
      // Opcional: Si usas fuentes específicas en el HTML
      fontFamily: {
        headings: ['Montserrat', 'sans-serif'],
        sans: ['Noto Sans', 'sans-serif'],
      }
    },
  },
  plugins: [],
}
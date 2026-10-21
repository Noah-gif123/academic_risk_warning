/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{vue,js}'],
  theme: {
    extend: {
      colors: {
        brand: {
          50: '#eff6ff',
          100: '#dbeafe',
          500: '#2b6cb0',
          600: '#2c5282',
          700: '#1a365d'
        }
      }
    }
  },
  plugins: []
}

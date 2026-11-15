/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{vue,js}'],
  theme: {
    extend: {
      colors: {
        brand: {
          50: '#ecfdf8',
          100: '#d9f3ee',
          200: '#b9e8df',
          300: '#7dd3c7',
          400: '#20b7a4',
          500: '#0f9d8a',
          600: '#0b7d6e',
          700: '#075e53'
        }
      }
    }
  },
  plugins: []
}

/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{vue,js}'],
  theme: {
    extend: {
      colors: {
        brand: {
          50: '#eff6ff',
          100: '#dbeafe',
          // 之前缺 400，导致 brand-400 的边框/圆点全部没颜色（时间轴圆点不可见、卡片选中态无边框）
          400: '#4299e1',
          500: '#2b6cb0',
          600: '#2c5282',
          700: '#1a365d'
        }
      }
    }
  },
  plugins: []
}

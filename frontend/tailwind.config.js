/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{vue,js,ts,jsx,tsx}'],
  theme: {
    extend: {
      fontFamily: {
        sans: ['"DM Sans"', 'ui-sans-serif', 'system-ui', 'sans-serif'],
        display: ['Oxanium', 'ui-sans-serif', 'system-ui', 'sans-serif'],
      },
      boxShadow: {
        lb: '0 0 50px -10px rgb(139 92 246 / 0.35)',
        'lb-soft': '0 25px 50px -12px rgb(0 0 0 / 0.55)',
      },
    },
  },
  plugins: [],
}

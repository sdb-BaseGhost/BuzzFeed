/** @type {import('tailwindcss').Config} */
export default {
  content: [
    './index.html',
    './src/**/*.{vue,js,ts,jsx,tsx}'
  ],
  theme: {
    extend: {
      colors: {
        'bg-primary': '#15202b',
        'bg-secondary': '#192734',
        'bg-hover': '#1d2f3f',
        'border-custom': '#38444d',
        'text-primary': '#e7e9ea',
        'text-secondary': '#536471',
        'accent': '#1d9bf0',
        'danger': '#f4212e',
        'success': '#00ba7c'
      }
    }
  },
  plugins: []
}

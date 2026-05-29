/** @type {import('tailwindcss').Config} */
export default {
  content: [
    './index.html',
    './src/**/*.{vue,js,ts,jsx,tsx}'
  ],
  theme: {
    extend: {
      colors: {
        'bg-primary': '#000000',
        'bg-secondary': '#16181c',
        'bg-hover': '#1d1f23',
        'border-custom': '#2f3336',
        'text-primary': '#e7e9ea',
        'text-secondary': '#71767b',
        'accent': '#1d9bf0',
        'danger': '#f4212e',
        'success': '#00ba7c'
      }
    }
  },
  plugins: []
}

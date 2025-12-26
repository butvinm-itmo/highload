/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      fontFamily: {
        'display': ['Cinzel', 'serif'], // Headings, titles - gothic elegance
        'serif': ['Crimson Text', 'Georgia', 'serif'], // Body text - readable mysticism
        'accent': ['Cormorant Garamond', 'serif'], // Special accents - flowing mystery
      },
      colors: {
        // Mystical dark palette
        mystic: {
          50: '#f5f3ff',
          100: '#ede9fe',
          200: '#ddd6fe',
          300: '#c4b5fd',
          400: '#a78bfa',
          500: '#8b5cf6',
          600: '#7c3aed',
          700: '#6d28d9',
          800: '#5b21b6',
          900: '#4c1d95',
          950: '#2e1065',
        },
        cosmic: {
          50: '#fdf4ff',
          100: '#fae8ff',
          200: '#f5d0fe',
          300: '#f0abfc',
          400: '#e879f9',
          500: '#d946ef',
          600: '#c026d3',
          700: '#a21caf',
          800: '#86198f',
          900: '#701a75',
          950: '#4a044e',
        },
        void: {
          50: '#f9fafb',
          100: '#f3f4f6',
          200: '#e5e7eb',
          300: '#d1d5db',
          400: '#9ca3af',
          500: '#6b7280',
          600: '#4b5563',
          700: '#374151',
          800: '#1f2937',
          900: '#111827',
          950: '#030712',
        },
        gold: {
          50: '#fffbeb',
          100: '#fef3c7',
          200: '#fde68a',
          300: '#fcd34d',
          400: '#fbbf24',
          500: '#f59e0b',
          600: '#d97706',
          700: '#b45309',
          800: '#92400e',
          900: '#78350f',
          950: '#451a03',
        },
      },
      backgroundImage: {
        'gradient-mystic': 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
        'gradient-cosmic': 'radial-gradient(ellipse at top, #1e1b4b, transparent), radial-gradient(ellipse at bottom, #3b0764, transparent)',
        'gradient-void': 'linear-gradient(180deg, #0f0f23 0%, #1a1a2e 50%, #0f0f23 100%)',
        'stars': "url('data:image/svg+xml,%3Csvg xmlns=\"http://www.w3.org/2000/svg\" width=\"400\" height=\"400\"%3E%3Ccircle cx=\"50\" cy=\"50\" r=\"1\" fill=\"white\" opacity=\"0.5\"/%3E%3Ccircle cx=\"150\" cy=\"100\" r=\"1\" fill=\"white\" opacity=\"0.3\"/%3E%3Ccircle cx=\"250\" cy=\"150\" r=\"1\" fill=\"white\" opacity=\"0.7\"/%3E%3Ccircle cx=\"350\" cy=\"50\" r=\"1\" fill=\"white\" opacity=\"0.4\"/%3E%3Ccircle cx=\"100\" cy=\"200\" r=\"1\" fill=\"white\" opacity=\"0.6\"/%3E%3Ccircle cx=\"300\" cy=\"250\" r=\"1\" fill=\"white\" opacity=\"0.5\"/%3E%3Ccircle cx=\"200\" cy=\"300\" r=\"1\" fill=\"white\" opacity=\"0.8\"/%3E%3Ccircle cx=\"75\" cy=\"350\" r=\"1\" fill=\"white\" opacity=\"0.4\"/%3E%3C/svg%3E')",
      },
      animation: {
        'shimmer': 'shimmer 3s linear infinite',
        'float': 'float 6s ease-in-out infinite',
        'glow': 'glow 2s ease-in-out infinite alternate',
        'fade-in': 'fadeIn 0.5s ease-in',
        'slide-up': 'slideUp 0.5s ease-out',
        'pulse-slow': 'pulse 4s cubic-bezier(0.4, 0, 0.6, 1) infinite',
      },
      keyframes: {
        shimmer: {
          '0%': { backgroundPosition: '-1000px 0' },
          '100%': { backgroundPosition: '1000px 0' },
        },
        float: {
          '0%, 100%': { transform: 'translateY(0px)' },
          '50%': { transform: 'translateY(-20px)' },
        },
        glow: {
          '0%': { boxShadow: '0 0 5px rgba(139, 92, 246, 0.5), 0 0 10px rgba(139, 92, 246, 0.3)' },
          '100%': { boxShadow: '0 0 20px rgba(139, 92, 246, 0.8), 0 0 30px rgba(139, 92, 246, 0.5)' },
        },
        fadeIn: {
          '0%': { opacity: '0' },
          '100%': { opacity: '1' },
        },
        slideUp: {
          '0%': { transform: 'translateY(10px)', opacity: '0' },
          '100%': { transform: 'translateY(0)', opacity: '1' },
        },
      },
      boxShadow: {
        'mystic': '0 0 20px rgba(139, 92, 246, 0.3), 0 0 40px rgba(139, 92, 246, 0.1)',
        'cosmic': '0 0 30px rgba(217, 70, 239, 0.4), 0 0 60px rgba(217, 70, 239, 0.2)',
        'gold': '0 0 20px rgba(245, 158, 11, 0.3), 0 0 40px rgba(245, 158, 11, 0.1)',
      },
    },
  },
  plugins: [],
}


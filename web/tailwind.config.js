/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        terminal: {
          bg: '#050505',     // Absolute black
          panel: '#0f0f12',   // Deep charcoal
          border: '#1e1e24',  // Stealth border
          text: '#f8f8f8',    // Off-white
          muted: '#6b6b7c',   // Slate muted
          bid: '#00ffaa',     // Neon Cyan-Green
          ask: '#ff3366',     // Electric Pink-Red
          accent: '#facc15',  // Gold highlight
        },
      },
      fontFamily: {
        mono: ['JetBrains Mono', 'ui-monospace', 'SFMono-Regular', 'monospace'],
      },
    },
  },
  plugins: [],
}

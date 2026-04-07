export const theme = {
  colors: {
    background: '#0a0a0b',
    surface: '#121214',
    glass: 'rgba(21, 21, 24, 0.85)', // Darker, more premium glass
    header: 'rgba(26, 54, 93, 0.8)',  // Deep blue container
    primary: '#4fd1ed',              // Cyan (borders/accents)
    secondary: '#4b5563',
    accent: '#f6ad55',               // Gold (names/log headers)
    danger: '#e53e3e',               // Red (HP)
    stamina: '#3182ce',              // Blue (Stamina)
    gold: '#f6ad55',                 // Currency
    gem: '#4fd1ed',                  // Precious gems
    text: {
      primary: '#ffffff',
      secondary: '#a0aec0',
      muted: '#718096',
      gold: '#f6ad55',
      cyan: '#4fd1ed',
    },
    border: {
      cyan: '#4fd1ed',
      gold: '#f6ad55',
      dark: 'rgba(0, 0, 0, 0.6)',
      light: 'rgba(79, 209, 237, 0.2)',
    }
  },
  gradients: {
    hp: ['#e53e3e', '#9b2c2c'],
    stamina: ['#3182ce', '#2c5282'],
    exp: ['#38a169', '#276749'],
    header: ['rgba(26, 54, 93, 0.9)', 'rgba(10, 10, 12, 0.9)'],
  },
  spacing: {
    xs: 4,
    sm: 8,
    md: 16,
    lg: 24,
    xl: 32,
    hud: 15, // Standard padding from screen edges
  },
  borderRadius: {
    xs: 2,
    sm: 4,
    md: 6,
    lg: 10,
    xl: 16,
    full: 9999,
  },
};

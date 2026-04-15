import hudConfig from './hudConfig.json';

/**
 * HUD LAYOUT CONFIGURATION
 * Centralized system for element positioning and scaling.
 */

export const HUD_VARIANTS = {
  DEFAULT: 'DEFAULT',
  COMPACT: 'COMPACT',
};

export const HUD_LAYOUT = {
  [HUD_VARIANTS.DEFAULT]: {
    TOP_BAR: {
      position: 'absolute', top: 0, left: 0, right: 0, zIndex: 100,
    },
    AGENT_PROFILE: {
      position: 'absolute', top: 60, left: 15, zIndex: 90,
      scale: hudConfig.DEFAULT.SCALE,
      dimensions: hudConfig.DEFAULT
    },
    SIDE_MENU: {
      position: 'absolute', right: 15, top: '25%', zIndex: 80,
    },
    AGENT_CONSOLE: {
      position: 'absolute', bottom: 20, left: 15, zIndex: 90,
    },
  },
  
  [HUD_VARIANTS.COMPACT]: {
    TOP_BAR: {
      position: 'absolute', top: 0, left: 0, right: 0, zIndex: 100,
    },
    AGENT_PROFILE: {
      position: 'absolute', top: 50, left: 10, zIndex: 90,
      scale: hudConfig.COMPACT.SCALE,
      dimensions: hudConfig.COMPACT
    },
    SIDE_MENU: {
      position: 'absolute', right: 10, top: '20%', zIndex: 80,
      scale: 0.85,
    },
    AGENT_CONSOLE: {
      position: 'absolute', bottom: 10, left: 10, zIndex: 90,
      scale: 0.85,
    },
  }
};

export const getLayout = (variant = HUD_VARIANTS.DEFAULT) => HUD_LAYOUT[variant];

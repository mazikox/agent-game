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
    AGENT_PROFILE: {
      position: 'absolute', top: 20, left: 20, zIndex: 90,
      scale: hudConfig.DEFAULT.SCALE,
      dimensions: hudConfig.DEFAULT
    },
    AGENT_ACTIONS: {
      position: 'absolute', bottom: 20, left: 20, zIndex: 95,
    },
    AGENT_CONSOLE: {
      position: 'absolute', bottom: 20, left: 110, zIndex: 90,
    },
    ADMIN_PANEL: {
      position: 'absolute', top: 60, left: 450, zIndex: 100,
    },
  },
  
  [HUD_VARIANTS.COMPACT]: {
    AGENT_PROFILE: {
      position: 'absolute', top: 10, left: 10, zIndex: 90,
      scale: hudConfig.COMPACT.SCALE,
      dimensions: hudConfig.COMPACT
    },
    AGENT_ACTIONS: {
      position: 'absolute', bottom: 10, left: 10, zIndex: 95,
      scale: 0.85,
    },
    AGENT_CONSOLE: {
      position: 'absolute', bottom: 10, left: 90, zIndex: 90,
      scale: 0.85,
    },
    ADMIN_PANEL: {
      position: 'absolute', top: 50, left: 320, zIndex: 100,
      scale: 0.85,
    },
  }
};

export const getLayout = (variant = HUD_VARIANTS.DEFAULT) => HUD_LAYOUT[variant];

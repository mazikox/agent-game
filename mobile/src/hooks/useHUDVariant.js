import { useWindowDimensions } from 'react-native';
import { HUD_VARIANTS } from '../theme/hudLayout';

const BREAKPOINT_MOBILE = 800;

/**
 * Hook to determine the active HUD variant based on screen dimensions.
 */
export const useHUDVariant = () => {
  const { width } = useWindowDimensions();

  // Logic to determine variant
  // In the future, this could also check user settings or device type
  const variant = width < BREAKPOINT_MOBILE ? HUD_VARIANTS.COMPACT : HUD_VARIANTS.DEFAULT;

  return variant;
};

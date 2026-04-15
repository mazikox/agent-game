import React from 'react';
import { Animated } from 'react-native';
import { getLayout } from '../../theme/hudLayout';
import { useHUDVariant } from '../../hooks/useHUDVariant';

/**
 * HUD ELEMENT WRAPPER
 * 
 * This component is responsible for positioning HUD elements on the screen
 * based on the global HUD_LAYOUT configuration.
 */
export const HUDElement = ({ id, children, style }) => {
  const variant = useHUDVariant();
  const layout = getLayout(variant);
  const elementStyle = layout[id];

  if (!elementStyle) {
    console.warn(`[HUDElement] No layout configuration found for ID: ${id}`);
    return children;
  }

  // Clone child and pass the layout/dimensions to it if it's a component
  // This allows the child to scale its internal parts (like bar heights)
  const childrenWithProps = React.Children.map(children, child => {
    if (React.isValidElement(child)) {
      return React.cloneElement(child, { hudConfig: elementStyle.dimensions });
    }
    return child;
  });

  return (
    <Animated.View 
      style={[
        elementStyle, 
        style,
        elementStyle.scale ? { transform: [{ scale: elementStyle.scale }] } : {}
      ]} 
      pointerEvents="box-none"
    >
      {childrenWithProps}
    </Animated.View>
  );
};

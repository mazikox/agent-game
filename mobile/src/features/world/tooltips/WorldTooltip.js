import React from 'react';
import { View, StyleSheet, Platform } from 'react-native';
import { CreatureTooltipContent } from './CreatureTooltipContent';
import { PortalTooltipContent } from './PortalTooltipContent';

const TOOLTIP_RENDERERS = {
  creature: CreatureTooltipContent,
  portal: PortalTooltipContent,
};

export const WorldTooltip = ({ hoveredObject, mapWidth, mapHeight, imgW, imgH, cameraOffset, uiOffset = { x: 0, y: 0 } }) => {
  if (!hoveredObject) return null;

  const ContentRenderer = TOOLTIP_RENDERERS[hoveredObject.type];
  if (!ContentRenderer) return null;

  const data = hoveredObject.data;
  const sourceX = data.x ?? data.sourceX ?? 0;
  const sourceY = data.y ?? data.sourceY ?? 0;

  // Calculate base position on the map
  const basePixelX = (sourceX / Math.max(mapWidth, 1)) * imgW + (cameraOffset?.x ?? 0);
  const basePixelY = (sourceY / Math.max(mapHeight, 1)) * imgH + (cameraOffset?.y ?? 0);

  // Apply UI offsets for parent containers (like window headers)
  const finalX = basePixelX + uiOffset.x;
  const finalY = basePixelY + uiOffset.y;

  // Dynamic offset: height/2 (to reach top edge) + 12px margin
  const verticalOffset = (hoveredObject.measuredHeight / 2) + 12;

  return (
    <View
      style={[
        styles.container,
        {
          left: finalX,
          top: finalY - verticalOffset,
          transform: [{ translateX: '-50%' }, { translateY: '-100%' }],
        },
      ]}
      pointerEvents="none"
    >
      <ContentRenderer data={data} />
      <View style={styles.anchor} />
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    position: 'absolute',
    zIndex: 500,
    minWidth: 140,
    maxWidth: 220,
    backgroundColor: 'rgba(12, 12, 16, 0.95)',
    borderRadius: 8,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.12)',
    padding: 10,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 6 },
    shadowOpacity: 0.7,
    shadowRadius: 12,
    ...(Platform.OS === 'web' ? {
      backdropFilter: 'blur(8px)',
      WebkitBackdropFilter: 'blur(8px)',
    } : {}),
  },
  anchor: {
    position: 'absolute',
    bottom: -6,
    alignSelf: 'center',
    left: '50%',
    marginLeft: -6,
    width: 0,
    height: 0,
    borderLeftWidth: 6,
    borderRightWidth: 6,
    borderTopWidth: 6,
    borderLeftColor: 'transparent',
    borderRightColor: 'transparent',
    borderTopColor: 'rgba(12, 12, 16, 0.95)',
  },
});

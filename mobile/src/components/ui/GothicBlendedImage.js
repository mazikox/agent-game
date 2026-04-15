import React from 'react';
import { View, Image as RNImage, Platform } from 'react-native';
import Svg, { Defs, Filter, FeColorMatrix, Image as SvgImage } from 'react-native-svg';

/**
 * GOTHIC BLENDED IMAGE
 * 
 * An "Out of the box" solution for blending images with black backgrounds
 * in React Native Web/Native.
 * 
 * Instead of relying on CSS mix-blend-mode (which fails due to stacking context),
 * this uses an SVG Filter to mathematically convert the luminance of each pixel
 * into an Alpha channel (Black -> Transparent).
 */
export const GothicBlendedImage = ({ 
  source, 
  style, 
  resizeMode = 'contain' 
}) => {
  // Resolve image URI
  const imageUri = typeof source === 'number' 
    ? RNImage.resolveAssetSource(source).uri 
    : source.uri;

  // Extract width/height from style if available, otherwise default
  const flatStyle = Array.isArray(style) ? Object.assign({}, ...style) : style || {};
  const width = flatStyle.width || '100%';
  const height = flatStyle.height || '100%';

  return (
    <View style={style}>
      <Svg width="100%" height="100%" viewBox="0 0 1472 720" preserveAspectRatio="xMidYMid meet">
        <Defs>
          <Filter id="screenBlend">
            {/* 
              MATH: Alpha = R + G + B.
              This converts any pixel with color to non-transparent, 
              and any pixel that is pure black to fully transparent.
            */}
            <FeColorMatrix
              type="matrix"
              values="1 0 0 0 0 
                      0 1 0 0 0 
                      0 0 1 0 0 
                      5 5 5 0 0"
            />
          </Filter>
        </Defs>
        <SvgImage
          href={imageUri}
          width="100%"
          height="100%"
          preserveAspectRatio={resizeMode === 'contain' ? 'xMidYMid meet' : 'xMidYMid slice'}
          filter="url(#screenBlend)"
        />
      </Svg>
    </View>
  );
};

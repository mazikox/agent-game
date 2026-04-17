import React from 'react';
import { View, Image as RNImage, Image } from 'react-native';
import Svg, { Defs, Filter, FeColorMatrix, Image as SvgImage } from 'react-native-svg';

/**
 * GOTHIC BLENDED IMAGE
 * 
 * An "Out of the box" solution for blending images with black backgrounds.
 * Now supports a "isOpaque" mode to bypass the blend filter for solid UI panels.
 */
export const GothicBlendedImage = ({ 
  source, 
  style, 
  resizeMode = 'contain',
  nativeWidth = 1472,  
  nativeHeight = 720,  
  isSolid = false,     // Legacy: Boosts alpha but keeps "Screen" blend logic
  isOpaque = false     // New: Completely bypasses the blend filter for solid rendering
}) => {
  // If opaque mode is requested, return a standard high-fidelity Image
  if (isOpaque) {
    return (
      <View style={style}>
        <Image 
          source={source} 
          style={{ width: '100%', height: '100%' }}
          resizeMode={resizeMode}
        />
      </View>
    );
  }

  // Resolve image URI for SVG filter mode
  const imageUri = typeof source === 'number' 
    ? RNImage.resolveAssetSource(source).uri 
    : source.uri;

  // Matrix for alpha extraction:
  // Default: a = r+g+b (soft blend)
  // Solid: a = 5.0*(r+g+b) - 0.3 (aggressive alpha boost)
  const matrixValues = isSolid 
    ? "1 0 0 0 0 0 1 0 0 0 0 0 1 0 0 5.0 5.0 5.0 0 -0.3"
    : "1 0 0 0 0 0 1 0 0 0 0 0 1 0 0 1.0 1.0 1.0 0 0";

  return (
    <View style={style}>
      <Svg 
        width="100%" 
        height="100%" 
        viewBox={`0 0 ${nativeWidth} ${nativeHeight}`} 
        preserveAspectRatio={resizeMode === 'contain' ? 'xMidYMid meet' : 'xMidYMid slice'}
      >
        <Defs>
          <Filter id="screenBlend">
            <FeColorMatrix 
              type="matrix" 
              values={matrixValues} 
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

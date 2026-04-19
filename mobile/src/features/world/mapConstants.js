/**
 * Configuration for map rendering and camera logic.
 */
export const MAP_SETTINGS = {
  // How many pixels represent ONE world coordinate unit.
  // Example: if agent is at X:50 and SCALE is 40, his pixel position is 2000.
  WORLD_SCALE: 40,
  
  // Size of the agent visual marker in pixels
  AGENT_MARKER_SIZE: 60,
  
  // Size of portal icons
  PORTAL_SIZE: 50,
  
  // Smoothing factor for camera movement (higher = slower/smoother)
  CAMERA_LERP: 0.1,
};

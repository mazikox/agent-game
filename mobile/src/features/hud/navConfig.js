import {
  Briefcase,
  Zap,
  Map as MapIcon,
  ShoppingBag,
  Sword,
  BookOpen,
  Settings,
  Shield
} from 'lucide-react-native';
import { theme } from '../../theme/theme';

export const NAV_ITEMS = [
  { id: 'inventory', label: 'Inventory', icon: Briefcase, color: theme.colors.text.secondary },
  { id: 'skills', label: 'Skills', icon: Zap, color: theme.colors.text.secondary },
  { id: 'combat', label: 'Combat', icon: Sword, color: theme.colors.text.secondary },
  { id: 'map', label: 'Map', icon: MapIcon, color: theme.colors.text.secondary },
  { id: 'shop', label: 'Shop', icon: ShoppingBag, color: theme.colors.text.secondary },
  { id: 'quests', label: 'Quests', icon: BookOpen, color: theme.colors.text.secondary },
  { id: 'settings', label: 'Settings', icon: Settings, color: theme.colors.text.secondary },
  { id: 'admin', label: 'Admin', icon: Shield, color: theme.colors.text.secondary },
];

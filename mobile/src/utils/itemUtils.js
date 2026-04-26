import { BASE_URL } from '../api/agentApi';

export const ITEM_PLACEHOLDER = require('../../assets/items/placeholder.png');

export const getItemImageUrl = (iconUrl) => {
  if (!iconUrl) return null;
  if (iconUrl.startsWith('http')) {
    return iconUrl;
  }
  return `${BASE_URL}${iconUrl}`;
};

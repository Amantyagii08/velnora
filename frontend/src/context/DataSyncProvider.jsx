import { useEffect } from 'react';
import { startDataSync } from '../services/dataSync.js';

export default function DataSyncProvider({ children }) {
  useEffect(() => startDataSync(), []);
  return children;
}

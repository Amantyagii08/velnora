import { useEffect, useRef } from 'react';
import { registerSyncTask } from '../services/dataSync.js';

export default function useDataSyncTask(key, callback, intervalMs, enabled = true) {
  const callbackRef = useRef(callback);
  callbackRef.current = callback;
  useEffect(() => {
    if (!enabled) return undefined;
    return registerSyncTask(key, (context) => callbackRef.current(context), intervalMs);
  }, [key, intervalMs, enabled]);
}

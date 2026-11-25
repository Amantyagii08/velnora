const tasks = new Map();
const requests = new Map();
const generations = new Map();
const KNOWN_TASKS = ['marketplace', 'categories', 'cart', 'orders', 'admin-dashboard', 'seller-shop'];
const DEFAULT_INTERVAL = 12_000;
let schedulerUsers = 0;
let timer = null;

export function singleFlight(key, operation) {
  if (requests.has(key)) return requests.get(key);
  const promise = Promise.resolve().then(operation).finally(() => {
    if (requests.get(key) === promise) requests.delete(key);
  });
  requests.set(key, promise);
  return promise;
}

function runTask(task, invalidate = false) {
  if (tasks.get(task.key) !== task) return Promise.resolve();
  if (task.inFlight) {
    if (invalidate) {
      task.generation += 1;
      task.pending = true;
    }
    return task.inFlight;
  }
  if (invalidate) task.generation += 1;
  const generation = task.generation;
  task.lastRun = Date.now();
  task.inFlight = Promise.resolve()
    .then(() => task.callback({ isCurrent: () => tasks.get(task.key) === task && generation === task.generation }))
    .catch(() => {})
    .finally(() => {
      task.inFlight = null;
      if (task.pending && tasks.get(task.key) === task) {
        task.pending = false;
        runTask(task);
      }
    });
  return task.inFlight;
}

export function registerSyncTask(key, callback, intervalMs = DEFAULT_INTERVAL) {
  const task = { key, callback, intervalMs, lastRun: Date.now(), inFlight: null, pending: false, generation: 0 };
  tasks.set(key, task);
  return () => {
    if (tasks.get(key) === task) tasks.delete(key);
    task.generation += 1;
  };
}

export function requestDataSync(...keys) {
  const requested = new Set(keys.length ? keys : ['all']);
  if (requested.has('all')) {
    for (const key of new Set([...KNOWN_TASKS, ...tasks.keys()])) generations.set(key, (generations.get(key) || 0) + 1);
  } else {
    for (const key of requested) generations.set(key, (generations.get(key) || 0) + 1);
  }
  return Promise.all([...tasks.values()]
    .filter((task) => requested.has('all') || requested.has(task.key))
    .map((task) => runTask(task, true)));
}

export function getSyncGeneration(key) { return generations.get(key) || 0; }

function pollDueTasks() {
  if (typeof document !== 'undefined' && document.visibilityState !== 'visible') return;
  const now = Date.now();
  for (const task of tasks.values()) {
    if (!task.inFlight && now - task.lastRun >= task.intervalMs) runTask(task);
  }
}

function handleVisibility() {
  if (document.visibilityState === 'visible') requestDataSync('all');
}

export function startDataSync() {
  schedulerUsers += 1;
  if (schedulerUsers === 1) {
    timer = window.setInterval(pollDueTasks, 1_000);
    document.addEventListener('visibilitychange', handleVisibility);
  }
  return () => {
    schedulerUsers = Math.max(0, schedulerUsers - 1);
    if (schedulerUsers === 0) {
      window.clearInterval(timer);
      timer = null;
      document.removeEventListener('visibilitychange', handleVisibility);
    }
  };
}

export const SYNC_INTERVAL = DEFAULT_INTERVAL;
export const STATIC_SYNC_INTERVAL = 60_000;

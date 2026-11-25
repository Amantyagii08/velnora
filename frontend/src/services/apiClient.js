import axios from 'axios';

// Keep future API configuration in one place; no requests are made during initialization.
const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  headers: {
    Accept: 'application/json',
  },
});

export default apiClient;

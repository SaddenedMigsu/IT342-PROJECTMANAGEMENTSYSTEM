// src/services/axiosConfig.js

import axios from 'axios';

// Create axios instance with a base URL
const apiClient = axios.create({
  baseURL: 'http://localhost:8080', // Update this to match your backend URL
  headers: {
    'Content-Type': 'application/json'
  }
});

// Add a request interceptor
apiClient.interceptors.request.use(
  config => {
    // Get the token from localStorage
    const user = JSON.parse(localStorage.getItem('user'));
    if (user && user.token) {
      // Add token to Authorization header
      config.headers['Authorization'] = `Bearer ${user.token}`;
    }
    return config;
  },
  error => {
    return Promise.reject(error);
  }
);

// Add a response interceptor
apiClient.interceptors.response.use(
  response => {
    return response;
  },
  error => {
    // Handle 401 (Unauthorized) responses
    if (error.response && error.response.status === 401) {
      // Clear user from localStorage and redirect to login
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default apiClient;
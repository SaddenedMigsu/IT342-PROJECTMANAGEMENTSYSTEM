import axios from "axios";

// Create axios instance with base configuration
const api = axios.create({
  baseURL: "http://localhost:8080/api",
  headers: {
    "Content-Type": "application/json",
  },
});

// Add a request interceptor
api.interceptors.request.use(
  (config) => {
    // Don't add token for auth endpoints
    if (config.url.includes('/auth/')) {
      return config;
    }

    const token = localStorage.getItem("token");
    if (token) {
      // Set the Authorization header for every request if token exists
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Add a response interceptor
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    // Don't handle auth errors for auth endpoints
    if (originalRequest.url.includes('/auth/')) {
      return Promise.reject(error);
    }

    // Handle 401 Unauthorized errors
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;
      
      // Clear authentication data
      localStorage.removeItem("token");
      localStorage.removeItem("user");
      
      // Redirect to login page if not already there
      if (!window.location.pathname.includes("/login")) {
        window.location.href = "/admin/login";
      }
    }

    return Promise.reject(error);
  }
);

export default api;

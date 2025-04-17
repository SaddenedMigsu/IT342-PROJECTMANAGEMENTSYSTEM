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
    const token = localStorage.getItem("token");
    if (token) {
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
  (error) => {
    // Only handle 401/403 for non-login endpoints
    if (error.response?.status === 401 || error.response?.status === 403) {
      const isLoginEndpoint = error.config.url.includes("/users/login");
      if (!isLoginEndpoint) {
        // Clear token only for non-login endpoints
        localStorage.removeItem("token");
        localStorage.removeItem("user");
        // Redirect to login only if not already on login page
        if (!window.location.pathname.includes("/login")) {
          window.location.href = "/admin/login";
        }
      }
    }
    return Promise.reject(error);
  }
);

export default api;

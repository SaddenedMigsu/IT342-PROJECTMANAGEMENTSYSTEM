import axios from "axios";

// Update this URL to match your backend
const API_BASE_URL = "https://it342-projectmanagementsystem.onrender.com/api/auth";

// Create an axios instance with default config
const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json'
  }
});

// Add response interceptor for error handling
api.interceptors.response.use(
  (response) => response,
  (error) => {
    console.error('API Error:', {
      status: error.response?.status,
      data: error.response?.data,
      message: error.message
    });
    return Promise.reject(error);
  }
);

const authService = {
  // Login function that directly accesses the login endpoint
  login: async (identifier, password) => {
    try {
      // Clean up identifier (remove spaces but keep dashes)
      const cleanIdentifier = identifier.trim();
      
      const response = await api.post('/login', {
        identifier: cleanIdentifier,
        password: password
      });

      if (response.data && response.data.token) {
        // Store auth data
        localStorage.setItem("token", response.data.token);
        localStorage.setItem("user", JSON.stringify({
          userId: response.data.userId,
          studId: response.data.studId,
          email: response.data.email,
          role: response.data.role
        }));
        
        // Set default auth header for future requests
        api.defaults.headers.common["Authorization"] = `Bearer ${response.data.token}`;
        return response.data;
      } else {
        throw new Error("Invalid response format");
      }
    } catch (error) {
      if (error.response) {
        // The request was made and the server responded with a status code
        // that falls out of the range of 2xx
        throw new Error(error.response.data || "Authentication failed");
      } else if (error.request) {
        // The request was made but no response was received
        throw new Error("No response from server");
      } else {
        // Something happened in setting up the request that triggered an Error
        throw new Error(error.message || "Login failed");
      }
    }
  },

  // Get current user from localStorage
  getCurrentUser: () => {
    try {
      const userStr = localStorage.getItem("user");
      return userStr ? JSON.parse(userStr) : null;
    } catch (error) {
      console.error("Error getting current user:", error);
      return null;
    }
  },

  // Check if user is authenticated
  isAuthenticated: () => {
    const token = localStorage.getItem("token");
    if (token) {
      api.defaults.headers.common["Authorization"] = `Bearer ${token}`;
      return true;
    }
    return false;
  },

  // Logout function
  logout: () => {
    localStorage.removeItem("token");
    localStorage.removeItem("user");
    delete api.defaults.headers.common["Authorization"];
  },
};

export default authService;

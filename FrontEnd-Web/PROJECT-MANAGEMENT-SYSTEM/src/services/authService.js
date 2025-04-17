import axios from "axios";

// Update this URL to match your backend
const API_BASE_URL = "http://localhost:8080/api/auth";

const authService = {
  // Login function that directly accesses the login endpoint
  login: async (identifier, password) => {
    try {
      const response = await axios.post(`${API_BASE_URL}/login`, {
        identifier: identifier,
        password: password,
      });

      if (response.data) {
        // Store the token and user data
        localStorage.setItem("token", response.data.token);
        localStorage.setItem("user", JSON.stringify(response.data.user));
      }

      return response.data;
    } catch (error) {
      console.error("Login failed:", error);
      throw error;
    }
  },

  // Get current user from localStorage
  getCurrentUser: () => {
    const user = localStorage.getItem("user");
    if (user) {
      return JSON.parse(user);
    }
    return null;
  },

  // Check if user is authenticated
  isAuthenticated: () => {
    const token = localStorage.getItem("token");
    return !!token;
  },

  // Logout function
  logout: () => {
    localStorage.removeItem("token");
    localStorage.removeItem("user");
  },
};

export default authService;

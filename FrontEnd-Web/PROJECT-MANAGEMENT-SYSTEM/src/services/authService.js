import axios from 'axios';

// Update this URL to match your Spring Boot backend
const API_BASE_URL = 'http://localhost:8080/api/auth';

const authService = {
  // Login function that directly accesses the login endpoint
  login: async (identifier, password) => {
    try {
      const response = await axios.post(`${API_BASE_URL}/login`, {
        identifier: identifier,
        password: password
      });
      
      if (response.data) {
        localStorage.setItem('user', JSON.stringify(response.data));
      }
      
      return response.data;
    } catch (error) {
      console.error('Login failed:', error);
      throw error;
    }
  },

  // Get current user from localStorage
  getCurrentUser: () => {
    const user = localStorage.getItem('user');
    if (user) {
      return JSON.parse(user);
    }
    return null;
  },

  // Check if user is authenticated
  isAuthenticated: () => {
    const user = localStorage.getItem('user');
    return !!user && !!JSON.parse(user).token;
  },

  // Logout function
  logout: () => {
    localStorage.removeItem('user');
  }
};

export default authService;
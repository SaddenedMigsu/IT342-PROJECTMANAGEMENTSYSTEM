import api from "./axiosConfig";

const userService = {
  // Get all users (admin only)
  getAllUsers: async () => {
    try {
      const response = await api.get("/users/all");
      return response.data;
    } catch (error) {
      console.error("Error fetching users:", error);
      throw error;
    }
  },

  // Get current user profile
  getCurrentProfile: async () => {
    try {
      const response = await api.get("/users/profile");
      return response.data;
    } catch (error) {
      console.error("Error fetching user profile:", error);
      throw error;
    }
  },

  // Update user profile
  updateUserProfile: async (updates) => {
    try {
      const response = await api.patch("/users/profile", updates);
      return response.data;
    } catch (error) {
      console.error("Error updating user profile:", error);
      throw error;
    }
  },

  // Get total active users count
  getActiveUsersCount: async () => {
    try {
      const response = await api.get("/users/active/count");
      return response.data.totalActiveUsers;
    } catch (error) {
      console.error("Error fetching active users count:", error);
      throw error;
    }
  },

  // Delete user (admin only)
  deleteUser: async (userId) => {
    try {
      const response = await api.delete(`/users/${userId}`);
      return response.data;
    } catch (error) {
      console.error("Error deleting user:", error);
      throw error;
    }
  },
};

export default userService;
